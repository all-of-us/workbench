package org.pmiops.workbench.cdr;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private static final ImmutableList<Domain> CHILD_LOOKUP_DOMAINS =
      ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT);
  private static final String SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE =
      "select DISTINCT(question_concept_id) as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n";

  @Autowired
  public ConceptBigQueryService(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
  }

  public int getParticipantCountForConcepts(
      Domain domain, String omopTable, Set<DbConceptSetConceptId> dbConceptSetConceptIds) {
    ConceptColumns conceptColumns = cdrBigQuerySchemaConfigService.getConceptColumns(omopTable);
    Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
        dbConceptSetConceptIds.stream()
            .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
    List<Long> standardList =
        partitionSourceAndStandard.get(true).stream()
            .map(DbConceptSetConceptId::getConceptId)
            .collect(Collectors.toList());
    List<Long> sourceList =
        partitionSourceAndStandard.get(false).stream()
            .map(DbConceptSetConceptId::getConceptId)
            .collect(Collectors.toList());
    StringBuilder innerSql = new StringBuilder("select count(distinct person_id) person_count\n");
    innerSql.append("from ");
    innerSql.append(String.format("`${projectId}.${dataSetId}.%s`", omopTable));
    innerSql.append(" where ");
    ImmutableMap.Builder<String, QueryParameterValue> paramMap = ImmutableMap.builder();
    if (!standardList.isEmpty()) {
      innerSql.append(conceptColumns.getStandardConceptColumn().name);
      generateParentChildLookupSql(
          innerSql, domain, "standardConceptIds", 1, standardList, paramMap);
      if (!sourceList.isEmpty()) {
        innerSql.append(" or ");
      }
    }
    if (!sourceList.isEmpty()) {
      if (Domain.SURVEY.equals(domain)) {
        innerSql.append("observation_source_concept_id");
      } else {
        innerSql.append(conceptColumns.getSourceConceptColumn().name);
      }
      generateParentChildLookupSql(innerSql, domain, "sourceConceptIds", 0, sourceList, paramMap);
    }
    QueryJobConfiguration jobConfiguration =
        QueryJobConfiguration.newBuilder(innerSql.toString())
            .setNamedParameters(paramMap.build())
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration));
    return (int) result.iterateAll().iterator().next().get(0).getLongValue();
  }

  private void generateParentChildLookupSql(
      StringBuilder sqlBuilder,
      Domain domain,
      String conceptIdsParam,
      int standardOrSource,
      List<Long> conceptIds,
      ImmutableMap.Builder<String, QueryParameterValue> paramMap) {
    if (CHILD_LOOKUP_DOMAINS.contains(domain)) {
      sqlBuilder.append(
          " in (select concept_id\n"
              + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
              + "join (select cast(id as string) as id\n"
              + "from `${projectId}.${dataSetId}.cb_criteria`\n"
              + "where concept_id in unnest(@"
              + conceptIdsParam
              + ")\n"
              + "and domain_id = @domain\n"
              + "and is_standard = @standard) a\n"
              + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
              + "and domain_id = @domain\n"
              + "and is_standard = @standard)");
    } else if (Domain.DRUG.equals(domain)) {
      sqlBuilder.append(
          " in (select distinct ca.descendant_id\n"
              + "from `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
              + "join (select distinct c.concept_id\n"
              + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
              + "join (select cast(cr.id as string) as id\n"
              + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
              + "where domain_id = @domain\n"
              + "and concept_id in unnest(@"
              + conceptIdsParam
              + ")\n"
              + ") a\n"
              + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
              + "and domain_id = @domain\n"
              + ") b on (ca.ancestor_id = b.concept_id))");
    } else {
      sqlBuilder.append(" in unnest(@" + conceptIdsParam + ")");
    }
    paramMap.put(
        conceptIdsParam, QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class));
    if (CHILD_LOOKUP_DOMAINS.contains(domain) || Domain.DRUG.equals(domain)) {
      paramMap.put("domain", QueryParameterValue.string(domain.toString()));
      paramMap.put("standard", QueryParameterValue.int64(standardOrSource));
    }
  }

  public List<Long> getSurveyQuestionConceptIds() {
    QueryJobConfiguration qjc =
        QueryJobConfiguration.newBuilder(SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE)
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc), 360000L);
    List<Long> conceptIdList = new ArrayList<>();
    result
        .getValues()
        .forEach(
            surveyValue -> {
              conceptIdList.add(Long.parseLong(surveyValue.get(0).getValue().toString()));
            });
    return conceptIdList;
  }
}
