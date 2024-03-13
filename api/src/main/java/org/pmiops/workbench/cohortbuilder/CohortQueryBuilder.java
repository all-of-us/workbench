package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

@Service
public class CohortQueryBuilder extends QueryBuilder {

  private static final String SEARCH_PERSON_TABLE = "cb_search_person";

  private static final String COUNT_SQL_TEMPLATE =
      "SELECT COUNT(*) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n"
          + "WHERE ";

  private static final String RANDOM_SQL_TEMPLATE =
      "SELECT RAND() as x, person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, sex_at_birth_concept_id, birth_datetime, deceased\n"
          + "FROM (SELECT person.person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, sex_at_birth_concept_id, birth_datetime, CASE WHEN death.person_id IS NULL THEN false ELSE true END as deceased\n"
          + "FROM `${projectId}.${dataSetId}.person` person\n"
          + "LEFT JOIN `${projectId}.${dataSetId}.death` death ON (person.person_id = death.person_id)\n"
          + "WHERE person.person_id IN (${innerSql})\n"
          + "GROUP BY person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, sex_at_birth_concept_id, birth_datetime, deceased)\n";

  private static final String RANDOM_SQL_ORDER_BY = "ORDER BY x\nLIMIT";

  private static final String OFFSET_SUFFIX = " OFFSET ";

  private static final String ID_SQL_TEMPLATE =
      "SELECT distinct person_id\n FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n WHERE\n";

  private static final String UNION_TEMPLATE = "UNION DISTINCT\n";

  private static final Logger log = Logger.getLogger(CohortQueryBuilder.class.getName());

  /** Provides counts of unique subjects defined by the provided {@link ParticipantCriteria}. */
  public QueryJobConfiguration buildParticipantCounterQuery(
      ParticipantCriteria participantCriteria) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(COUNT_SQL_TEMPLATE);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(
        participantCriteria.getCohortDefinition().getDataFilters(), queryBuilder, params);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildRandomParticipantQuery(
      ParticipantCriteria participantCriteria, long resultSize, long offset) {
    String endSql = RANDOM_SQL_ORDER_BY + " " + resultSize;
    if (offset > 0) {
      endSql += OFFSET_SUFFIX + offset;
    }
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(ID_SQL_TEMPLATE);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(
        participantCriteria.getCohortDefinition().getDataFilters(), queryBuilder, params);
    String searchPersonSql = queryBuilder.toString();

    queryBuilder = new StringBuilder(RANDOM_SQL_TEMPLATE.replace("${innerSql}", searchPersonSql));
    queryBuilder.append(endSql);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  // This method supports the Data Set Builder, for further details see:
  // https://docs.google.com/document/d/1-wzSCHDM_LSaBRARyLFbsTGcBaKi5giRs-eDmaMBr0Y/edit
  public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria) {
    return buildUnionedParticipantIdQuery(ImmutableList.of(participantCriteria));
  }

  public QueryJobConfiguration buildUnionedParticipantIdQuery(
      List<ParticipantCriteria> participantCriteriaList) {
    List<String> queries = new ArrayList<>();
    Map<String, QueryParameterValue> params = new HashMap<>();

    for (ParticipantCriteria participantCriteria : participantCriteriaList) {
      StringBuilder queryBuilder = new StringBuilder(ID_SQL_TEMPLATE);
      addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
      queries.add(queryBuilder.toString());
    }
    return QueryJobConfiguration.newBuilder(String.join(UNION_TEMPLATE, queries))
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }
}
