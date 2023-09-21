package org.pmiops.workbench.cdr;

import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.CHILD_LOOKUP_SQL;
import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.DRUG_CHILD_LOOKUP_SQL;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private static final ImmutableList<Domain> CHILD_LOOKUP_DOMAINS =
      ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT, Domain.DRUG);

  @Autowired
  public ConceptBigQueryService(BigQueryService bigQueryService) {
    this.bigQueryService = bigQueryService;
  }

  public int getParticipantCountForConcepts(
      Domain domain, Set<DbConceptSetConceptId> dbConceptSetConceptIds) {
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
    StringBuilder innerSql = new StringBuilder();
    ImmutableMap.Builder<String, QueryParameterValue> paramMap = ImmutableMap.builder();
    if (!standardList.isEmpty()) {
      innerSql.append("SELECT person_id\n");
      innerSql.append("FROM `${projectId}.${dataSetId}.cb_search_all_events`\n");
      generateParentChildLookupSql(
          innerSql, domain, "standardConceptIds", 1, standardList, paramMap);
      innerSql.append("\n");
      if (!sourceList.isEmpty()) {
        innerSql.append(" UNION ALL\n");
      }
    }
    if (!sourceList.isEmpty()) {
      innerSql.append("SELECT person_id\n");
      innerSql.append("FROM `${projectId}.${dataSetId}.cb_search_all_events`\n");
      generateParentChildLookupSql(innerSql, domain, "sourceConceptIds", 0, sourceList, paramMap);
    }
    String finalSql = "SELECT COUNT(DISTINCT person_id) person_count FROM (\n" + innerSql + ")";
    QueryJobConfiguration jobConfiguration =
        QueryJobConfiguration.newBuilder(finalSql)
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
    String standardParam = (standardOrSource == 1 ? "standard" : "source");
    sqlBuilder.append(
        String.format("WHERE is_standard = %s AND concept_id IN ", "@" + standardParam));
    paramMap.put(
        conceptIdsParam, QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class));
    paramMap.put(standardParam, QueryParameterValue.int64(standardOrSource));
    if (CHILD_LOOKUP_DOMAINS.contains(domain)) {
      sqlBuilder.append(
          String.format(
              Domain.DRUG.equals(domain) ? DRUG_CHILD_LOOKUP_SQL : CHILD_LOOKUP_SQL,
              "@" + conceptIdsParam,
              "@" + standardParam));
    } else {
      sqlBuilder.append(" unnest(@" + conceptIdsParam + ")");
    }
  }
}
