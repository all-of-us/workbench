package org.pmiops.workbench.tools;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Backfill script to update cohort reviews with the correct sex at birth concept ids. This tool
 * aligns sex at birth data from cdr versions with the proper reviews cdr version.
 */
@Configuration
@ComponentScan(basePackages = "org.pmiops.workbench")
public class UpdateReviewDemographics {

  private static final Logger logger = Logger.getLogger(UpdateReviewDemographics.class.getName());

  private static final String SEX_AT_BIRTH_CONCEPT_ID_PARAM = "sexAtBirthConceptId";
  private static final String PERSON_ID_PARAM = "personId";
  private static final String SEX_AT_BIRTH_CONCEPT_ID = "sex_at_birth_concept_id";
  private static final String SELECT_DISTINCT_SEX_AT_BIRTH =
      "SELECT DISTINCT "
          + SEX_AT_BIRTH_CONCEPT_ID
          + "\n "
          + "FROM `${projectId}.${datasetId}.person`\n"
          + "WHERE "
          + SEX_AT_BIRTH_CONCEPT_ID
          + " != 0";
  private static final String SELECT_PERSON_IDS =
      "SELECT person_id\n"
          + "FROM `${projectId}.${datasetId}.person`\n"
          + "WHERE "
          + SEX_AT_BIRTH_CONCEPT_ID
          + " = @"
          + SEX_AT_BIRTH_CONCEPT_ID_PARAM
          + "\nAND person_id in unnest(@"
          + PERSON_ID_PARAM
          + ")";

  @Bean
  public CommandLineRunner run(
      CdrVersionDao cdrVersionDao,
      ParticipantCohortStatusDao participantCohortStatusDao,
      CohortReviewDao cohortReviewDao) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 args (dry_run). Got " + Arrays.asList(args));
      }
      boolean dryRun = Boolean.parseBoolean(args[0]);

      for (DbCdrVersion dbCdrVersion : cdrVersionDao.findAll()) {
        String projectId = dbCdrVersion.getBigqueryProject();
        String datasetId = dbCdrVersion.getBigqueryDataset();

        if (StringUtils.isNotEmpty(projectId) && StringUtils.isNotEmpty(datasetId)) {
          BigQuery bigQueryService = initBigQueryService(dbCdrVersion.getBigqueryProject());

          // Get all the sex at birth concept ids per cdr version
          List<Long> sexAtBirthConceptIds =
              StreamSupport.stream(
                      executeQuery(
                              bigQueryService, dbCdrVersion, SELECT_DISTINCT_SEX_AT_BIRTH, null)
                          .getValues()
                          .spliterator(),
                      false)
                  .map(fieldValues -> fieldValues.get(SEX_AT_BIRTH_CONCEPT_ID).getLongValue())
                  .collect(Collectors.toList());

          // Iterate each review per cdr version
          for (DbCohortReview dbCohortReview :
              cohortReviewDao.findAllByCdrVersionId(dbCdrVersion.getCdrVersionId())) {

            // Get all the person ids from review that need updating
            List<Long> personIds =
                participantCohortStatusDao
                    .findByParticipantKey_CohortReviewId(dbCohortReview.getCohortReviewId())
                    .stream()
                    .map(p -> p.getParticipantKey().getParticipantId())
                    .collect(Collectors.toList());

            for (Long sexAtBirthConceptId : sexAtBirthConceptIds) {
              // All person ids to update from BQ per sex at birth concept id
              final List<Long> personIdsToUpdate =
                  StreamSupport.stream(
                          executeQuery(
                                  bigQueryService,
                                  dbCdrVersion,
                                  SELECT_PERSON_IDS,
                                  createParameterMap(personIds, sexAtBirthConceptId))
                              .getValues()
                              .spliterator(),
                          false)
                      .map(fieldValues -> fieldValues.get("person_id").getLongValue())
                      .collect(Collectors.toList());

              if (!personIdsToUpdate.isEmpty()) {
                // Bulk update the participants sex at birth per person_id and review_id
                int personCount =
                    dryRun
                        ? personIdsToUpdate.size()
                        : participantCohortStatusDao
                            .bulkUpdateSexAtBirthByParticipantAndCohortReviewId(
                                sexAtBirthConceptId,
                                personIdsToUpdate,
                                dbCohortReview.getCohortReviewId());
                logger.info(
                    String.format(
                        "%d participant(s) %s with sex at birth concept id %d - review id %d - "
                            + "CDR v: %d name: %s project: %s dataset: %s r#: %d",
                        personCount,
                        dryRun ? "pending update" : "updated",
                        sexAtBirthConceptId,
                        dbCohortReview.getCohortReviewId(),
                        dbCdrVersion.getCdrVersionId(),
                        dbCdrVersion.getName(),
                        dbCdrVersion.getBigqueryProject(),
                        dbCdrVersion.getBigqueryDataset(),
                        dbCdrVersion.getReleaseNumber()));
              }
            }
          }
        }
      }
    };
  }

  @NotNull
  private Map<String, QueryParameterValue> createParameterMap(
      List<Long> personIds, Long sexAtBirthConceptId) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(SEX_AT_BIRTH_CONCEPT_ID_PARAM, QueryParameterValue.int64(sexAtBirthConceptId));
    params.put(
        PERSON_ID_PARAM, QueryParameterValue.array(personIds.toArray(new Long[0]), Long.class));
    return params;
  }

  private BigQuery initBigQueryService(String projectId) {
    return BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();
  }

  private TableResult executeQuery(
      BigQuery bigQueryService,
      DbCdrVersion cdrVersion,
      String query,
      Map<String, QueryParameterValue> params)
      throws InterruptedException {
    return bigQueryService
        .create(
            JobInfo.of(
                QueryJobConfiguration.newBuilder(
                        query
                            .replace("${projectId}", cdrVersion.getBigqueryProject())
                            .replace("${datasetId}", cdrVersion.getBigqueryDataset()))
                    .setUseLegacySql(false)
                    .setNamedParameters(params)
                    .build()))
        .getQueryResults(BigQuery.QueryResultsOption.maxWaitTime(300000L));
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(UpdateReviewDemographics.class, args);
  }
}
