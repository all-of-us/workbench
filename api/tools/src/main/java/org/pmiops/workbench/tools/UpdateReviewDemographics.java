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
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Backfill script to adjust cohort reviews that don't include sex at birth. */
@Configuration
public class UpdateReviewDemographics {

  private static final Logger logger = Logger.getLogger(UpdateReviewDemographics.class.getName());

  private BigQuery bigQueryService;
  private String projectId;
  private String datasetId;
  private static final String PARAM_NAME = "p0";
  private static final String SELECT_DISTINCT_SEX_AT_BIRTH =
      "SELECT DISTINCT sex_at_birth_concept_id\n FROM `${projectId}.${datasetId}.person`";
  private static final String SELECT_PERSON_IDS =
      "SELECT person_id\n"
          + "FROM `${projectId}.${datasetId}.person`\n"
          + "WHERE sex_at_birth_concept_id = @"
          + PARAM_NAME;

  @Bean
  public CommandLineRunner run(
      CdrVersionDao cdrVersionDao, ParticipantCohortStatusDao participantCohortStatusDao) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 args (dry_run). Got " + Arrays.asList(args));
      }
      boolean dryRun = Boolean.parseBoolean(args[0]);

      for (DbCdrVersion cdrVersion : cdrVersionDao.findAll()) {
        if (cdrVersion.getIsDefault()) {
          projectId = cdrVersion.getBigqueryProject();
          datasetId = cdrVersion.getBigqueryDataset();
        }
      }
      Optional.ofNullable(projectId)
          .orElseThrow(() -> new IllegalArgumentException("No Default CDR version exists."));
      bigQueryService = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();

      final List<Long> sexAtBirthConceptIds =
          StreamSupport.stream(
                  executeQuery(SELECT_DISTINCT_SEX_AT_BIRTH, null).getValues().spliterator(), false)
              .map(fieldValues -> fieldValues.get("sex_at_birth_concept_id").getLongValue())
              .collect(Collectors.toList());

      for (Long sexAtBirthConceptId : sexAtBirthConceptIds) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(PARAM_NAME, QueryParameterValue.int64(sexAtBirthConceptId));

        final List<Long> personIds =
            StreamSupport.stream(
                    executeQuery(SELECT_PERSON_IDS, params).getValues().spliterator(), false)
                .map(fieldValues -> fieldValues.get("person_id").getLongValue())
                .collect(Collectors.toList());

        logger.info(String.format("Processing sex at birth concept id: %d", sexAtBirthConceptId));
        participantCohortStatusDao.bulkUpdateSexAtBirthByParticipantId(
            sexAtBirthConceptId, personIds);
      }
    };
  }

  private TableResult executeQuery(String query, Map<String, QueryParameterValue> params)
      throws InterruptedException {
    return bigQueryService
        .create(
            JobInfo.of(
                QueryJobConfiguration.newBuilder(
                        query.replace("${projectId}", projectId).replace("${datasetId}", datasetId))
                    .setUseLegacySql(false)
                    .setNamedParameters(params)
                    .build()))
        .getQueryResults(BigQuery.QueryResultsOption.maxWaitTime(300000L));
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(UpdateReviewDemographics.class, args);
  }
}
