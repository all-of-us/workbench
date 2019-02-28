package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: delete when ui work is done.
 */
@Service
public class ReviewQueryBuilder {

  private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
  private static final String NAMED_LIMIT_PARAM = "limit";
  private static final String TABLE_PREFIX = QueryBuilderConstants.TABLE_PREFIX;

  private static final String VISIT_COLUMNS =
    ", visit_type as visitType\n";

  private static final String OBS_VISIT_COLUMNS =
    ", visit_id as visitId\n" +
      ", visit_concept_id as visitConceptId\n";

  private static final String STANDARD_COLUMNS =
    ", standard_name as standardName\n" +
      ", standard_code as standardCode\n" +
      ", standard_vocabulary as standardVocabulary\n";

  private static final String DRUG_COLUMNS =
    ", route as route\n" +
      ", dose as dose\n" +
      ", strength as strength\n";

  private static final String MEASUREMENT_COLUMNS =
    ", unit as unit\n" +
      ", ref_range as refRange\n";

  private static final String SOURCE_COLUMNS =
    ", source_name as sourceName\n" +
      ", source_code as sourceCode\n" +
      ", source_vocabulary as sourceVocabulary\n";

  private static final String AGE_AT_EVENT =
    ", age_at_event as ageAtEvent\n";

  private static final String MENTION_COLUMNS =
    ", num_mentions as numMentions\n" +
      ", first_mention as firstMention\n" +
      ", last_mention as lastMention\n";

  private static final String VALUE_COLUMNS =
      ", value_as_number as value\n";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id as personId\n" +
      ", data_id as dataId\n" +
      ", start_datetime as startDate\n";

  private static final String ALL_EVENTS_SQL_TEMPLATE =
    ", domain as domain\n" +
      STANDARD_COLUMNS +
      SOURCE_COLUMNS +
      AGE_AT_EVENT +
      VISIT_COLUMNS +
      DRUG_COLUMNS +
      MEASUREMENT_COLUMNS +
      MENTION_COLUMNS +
      VALUE_COLUMNS;

  private static final String CONDITION_SQL_TEMPLATE =
    STANDARD_COLUMNS +
      SOURCE_COLUMNS +
      VISIT_COLUMNS +
      AGE_AT_EVENT;

  private static final String PROCEDURE_SQL_TEMPLATE =
    CONDITION_SQL_TEMPLATE;

  private static final String DRUG_SQL_TEMPLATE =
    ", standard_name as standardName\n" +
      DRUG_COLUMNS +
      AGE_AT_EVENT +
      MENTION_COLUMNS +
      VISIT_COLUMNS;

  private static final String LAB_SQL_TEMPLATE =
    ", standard_name as standardName\n" +
      VALUE_COLUMNS +
      MEASUREMENT_COLUMNS +
      AGE_AT_EVENT +
      VISIT_COLUMNS;

  private static final String VITAL_SQL_TEMPLATE =
    LAB_SQL_TEMPLATE;

  private static final String OBSERVATION_SQL_TEMPLATE =
    STANDARD_COLUMNS +
      SOURCE_COLUMNS +
      OBS_VISIT_COLUMNS +
      AGE_AT_EVENT;

  private static final String PHYSICAL_MEASURE_SQL_TEMPLATE =
    STANDARD_COLUMNS +
      VALUE_COLUMNS +
      ", unit as unit\n" +
      AGE_AT_EVENT;

  private static final String SURVEY_SQL_TEMPLATE =
    ", survey as survey\n" +
      ", question as question\n" +
      ", answer as answer\n";

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

  private static final String CHART_DATA_TEMPLATE =
    "select distinct a.standard_name as standardName, a.standard_vocabulary as standardVocabulary, " +
      "DATE(a.start_datetime) as startDate, a.age_at_event as ageAtEvent, rnk as rank\n" +
      "from `${projectId}.${dataSetId}.%s` a\n" +
      "left join (select standard_code, RANK() OVER(ORDER BY COUNT(*) DESC) as rnk\n" +
      "from `${projectId}.${dataSetId}.%s`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "and standard_concept_id != 0 \n" +
      "group by standard_code\n" +
      "LIMIT @" + NAMED_LIMIT_PARAM + ") b on a.standard_code = b.standard_code\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "and rnk <= @" + NAMED_LIMIT_PARAM + "\n" +
      "order by rank, standardName, startDate\n";

  public QueryJobConfiguration buildQuery(Long participantId,
                                          DomainType domain,
                                          PageRequest pageRequest) {
    String tableName = TABLE_PREFIX + domain.toString().toLowerCase();
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
    String tableName = TABLE_PREFIX + domain.toString().toLowerCase();
    String finalSql = String.format(COUNT_TEMPLATE, tableName);
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  public QueryJobConfiguration buildChartDataQuery(Long participantId,
                                                   DomainType domain,
                                                   Integer limit) {
    String tableName = TABLE_PREFIX + domain.toString().toLowerCase();
    String finalSql = String.format(CHART_DATA_TEMPLATE, tableName, tableName);
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    params.put(NAMED_LIMIT_PARAM, QueryParameterValue.int64(limit));
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  private String getSqlTemplate(DomainType domainType) {
    switch (domainType) {
      case ALL_EVENTS:
        return ALL_EVENTS_SQL_TEMPLATE;
      case DRUG:
        return DRUG_SQL_TEMPLATE;
      case CONDITION:
        return CONDITION_SQL_TEMPLATE;
      case PROCEDURE:
        return PROCEDURE_SQL_TEMPLATE;
      case OBSERVATION:
        return OBSERVATION_SQL_TEMPLATE;
      case LAB:
        return LAB_SQL_TEMPLATE;
      case VITAL:
        return VITAL_SQL_TEMPLATE;
      case SURVEY:
        return SURVEY_SQL_TEMPLATE;
      default:
        return PHYSICAL_MEASURE_SQL_TEMPLATE;
    }
  }
}
