package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewTabQueryBuilder {

  private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
  private static final String TABLE_PREFIX = "p_";
  private static final String MASTER_TABLE = "p_all_events";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id as personId,\n" +
      "data_id as dataId,\n" +
      "start_datetime as startDate,\n";

  private static final String ALL_EVENTS_SQL_TEMPLATE =
    "domain as domain,\n" +
      "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "source_name as sourceName,\n" +
      "source_code as sourceCode,\n" +
      "source_vocabulary as sourceVocabulary,\n" +
      "age_at_event as ageAtEvent,\n" +
      "visit_type as visitType,\n" +
      "source_value as sourceValue,\n" +
      "num_mentions as numMentions,\n" +
      "first_mention as firstMention,\n" +
      "last_mention as lastMention\n";

  private static final String CONDITION_SQL_TEMPLATE =
    "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "source_name as sourceName,\n" +
      "source_code as sourceCode,\n" +
      "source_vocabulary as sourceVocabulary,\n" +
      "visit_id as visitId,\n" +
      "visit_concept_id as visitConceptId,\n" +
      "age_at_event as ageAtEvent,\n" +
      "num_mentions as numMentions,\n" +
      "first_mention as firstMention,\n" +
      "last_mention as lastMention\n";

  private static final String PROCEDURE_SQL_TEMPLATE =
    CONDITION_SQL_TEMPLATE;

  private static final String DRUG_SQL_TEMPLATE =
    CONDITION_SQL_TEMPLATE + ", quantity as quantity,\n" +
      "refills as refills,\n" +
      "strength as strength,\n" +
      "route as route\n";

  private static final String MEASUREMENT_SQL_TEMPLATE =
    "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "source_name as sourceName,\n" +
      "source_code as sourceCode,\n" +
      "source_vocabulary as sourceVocabulary,\n" +
      "visit_id as visitId,\n" +
      "visit_concept_id as visitConceptId,\n" +
      "age_at_event as ageAtEvent,\n" +
      "value_concept as valueConcept,\n" +
      "value_as_number as valueAsNumber,\n" +
      "value_source_value as valueSourceValue,\n" +
      "units as units,\n" +
      "ref_range as refRange\n";

  private static final String OBSERVATION_SQL_TEMPLATE =
    "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "source_name as sourceName,\n" +
      "source_code as sourceCode,\n" +
      "source_vocabulary as sourceVocabulary,\n" +
      "visit_id as visitId,\n" +
      "visit_concept_id as visitConceptId,\n" +
      "age_at_event as ageAtEvent\n";

  private static final String PHYSICAL_MEASURE_SQL_TEMPLATE =
    "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "age_at_event as ageAtEvent,\n" +
      "value_concept as valueConcept,\n" +
      "value_as_number as valueAsNumber,\n" +
      "value_source_value as valueSourceValue,\n" +
      "units as units\n";

  private static final String FROM =
    "from `${projectId}.${dataSetId}.%s`\n";

  private static final String WHERE_TEMPLATE =
    "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "order by %s %s, data_id\n" +
      "limit %d offset %d\n";

  private static final String COUNT_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.%s`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  public QueryJobConfiguration buildQuery(Long participantId,
                                          DomainType domain,
                                          PageRequest pageRequest) {
    String tableName = DomainType.MASTER.equals(domain)
      ? MASTER_TABLE : TABLE_PREFIX + domain.toString().toLowerCase();
    String finalSql = String.format(BASE_SQL_TEMPLATE + getSqlTemplate(domain) + FROM + WHERE_TEMPLATE,
      tableName,
      pageRequest.getSortColumn(),
      pageRequest.getSortOrder().toString(),
      pageRequest.getPageSize(),
      pageRequest.getPage() * pageRequest.getPageSize());
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  public QueryJobConfiguration buildCountQuery(Long participantId,
                                               DomainType domain) {
    String tableName = DomainType.MASTER.equals(domain)
      ? MASTER_TABLE : TABLE_PREFIX + domain.toString().toLowerCase();
    String finalSql = String.format(COUNT_TEMPLATE, tableName);
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  public QueryJobConfiguration buildDetailsQuery(Long dataId, DomainType domain) {
    return QueryJobConfiguration
      .newBuilder("select * from `${projectId}.${dataSetId}.p_all_events` where person_id = -999")
      .setUseLegacySql(false)
      .build();
  }

  private String getSqlTemplate(DomainType domainType) {
    switch (domainType) {
      case MASTER:
        return ALL_EVENTS_SQL_TEMPLATE;
      case DRUG:
        return DRUG_SQL_TEMPLATE;
      case CONDITION:
        return CONDITION_SQL_TEMPLATE;
      case PROCEDURE:
        return PROCEDURE_SQL_TEMPLATE;
      case OBSERVATION:
        return OBSERVATION_SQL_TEMPLATE;
      case MEASUREMENT:
        return MEASUREMENT_SQL_TEMPLATE;
      default:
        return PHYSICAL_MEASURE_SQL_TEMPLATE;
    }
  }
}
