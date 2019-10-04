package org.pmiops.workbench.cohortreview;

import static org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT;
import static org.pmiops.workbench.model.FilterColumns.ANSWER;
import static org.pmiops.workbench.model.FilterColumns.DOMAIN;
import static org.pmiops.workbench.model.FilterColumns.DOSE;
import static org.pmiops.workbench.model.FilterColumns.FIRST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.LAST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.NUM_OF_MENTIONS;
import static org.pmiops.workbench.model.FilterColumns.QUESTION;
import static org.pmiops.workbench.model.FilterColumns.REF_RANGE;
import static org.pmiops.workbench.model.FilterColumns.ROUTE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CODE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_NAME;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_VOCAB;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CODE;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_NAME;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_VOCAB;
import static org.pmiops.workbench.model.FilterColumns.START_DATE;
import static org.pmiops.workbench.model.FilterColumns.STRENGTH;
import static org.pmiops.workbench.model.FilterColumns.SURVEY_NAME;
import static org.pmiops.workbench.model.FilterColumns.UNIT;
import static org.pmiops.workbench.model.FilterColumns.VAL_AS_NUMBER;
import static org.pmiops.workbench.model.FilterColumns.VISIT_TYPE;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.DomainType;
import org.springframework.stereotype.Service;

/** TODO: delete when ui work is done. */
@Service
public class ReviewQueryBuilder {

  private static final String PART_ID = "participantId";
  private static final String DOMAIN_PARAM = "domain";
  private static final String LIMIT = "limit";
  private static final String REVIEW_TABLE = "cb_review_all_events";
  private static final String SURVEY_TABLE = "cb_review_survey";
  private static final Object[] BASE_SQL_ARGS =
      new Object[] {
        START_DATE.toString(),
        DOMAIN.toString(),
        STANDARD_NAME.toString(),
        STANDARD_CODE.toString(),
        STANDARD_VOCAB.toString(),
        STANDARD_CONCEPT_ID.toString(),
        SOURCE_NAME.toString(),
        SOURCE_CODE.toString(),
        SOURCE_VOCAB.toString(),
        SOURCE_CONCEPT_ID.toString(),
        AGE_AT_EVENT.toString(),
        VISIT_TYPE.toString(),
        ROUTE.toString(),
        DOSE.toString(),
        STRENGTH.toString(),
        UNIT.toString(),
        REF_RANGE.toString(),
        NUM_OF_MENTIONS.toString(),
        FIRST_MENTION.toString(),
        LAST_MENTION.toString(),
        VAL_AS_NUMBER.toString(),
        REVIEW_TABLE,
        PART_ID
      };
  private static final Object[] SURVEY_ARGS =
      new Object[] {
        START_DATE.toString(),
        SURVEY_NAME.toString(),
        QUESTION.toString(),
        ANSWER.toString(),
        SURVEY_TABLE,
        PART_ID
      };
  private static final Object[] CHART_DATA_ARGS =
      new Object[] {
        REVIEW_TABLE, REVIEW_TABLE, PART_ID, DOMAIN_PARAM, LIMIT, PART_ID, DOMAIN_PARAM, LIMIT
      };

  private static final String DOMAIN_SQL = "and domain = @" + DOMAIN_PARAM + "\n";
  private static final String ORDER_BY = "order by %s %s, dataId\n limit %s offset %s\n";
  private static final String BASE_SQL_TEMPLATE =
      "select person_id as personId,\n"
          + "data_id as dataId,\n"
          + "start_datetime as %s,\n"
          + "domain as %s,\n"
          + "standard_name as %s,\n"
          + "standard_code as %s,\n"
          + "standard_vocabulary as %s,\n"
          + "standard_concept_id as %s,\n"
          + "source_name as %s,\n"
          + "source_code as %s,\n"
          + "source_vocabulary as %s,\n"
          + "source_concept_id as %s,\n"
          + "age_at_event as %s,\n"
          + "visit_type as %s,\n"
          + "route as %s,\n"
          + "dose as %s,\n"
          + "strength as %s,\n"
          + "unit as %s,\n"
          + "ref_range as %s,\n"
          + "num_mentions as %s,\n"
          + "first_mention as %s,\n"
          + "last_mention as %s,\n"
          + "value_as_number as %s\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n";
  private static final String SURVEY_SQL_TEMPLATE =
      "select person_id as personId,\n"
          + "data_id as dataId,\n"
          + "start_datetime as %s,\n"
          + "survey as %s,\n"
          + "question as %s,\n"
          + "answer as %s\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n";
  private static final String COUNT_TEMPLATE =
      "select count(*) as count\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n";
  private static final String COUNT_SURVEY_TEMPLATE =
      "select count(*) as count\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n";
  private static final String CHART_DATA_TEMPLATE =
      "select distinct a.standard_name as standardName, a.standard_vocabulary as standardVocabulary, "
          + "DATE(a.start_datetime) as startDate, a.age_at_event as ageAtEvent, rnk as rank\n"
          + "from `${projectId}.${dataSetId}.%s` a\n"
          + "left join (select standard_code, RANK() OVER(ORDER BY COUNT(*) DESC) as rnk\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n"
          + "and domain = @%s\n"
          + "and standard_concept_id != 0 \n"
          + "group by standard_code\n"
          + "LIMIT @%s) b on a.standard_code = b.standard_code\n"
          + "where person_id = @%s\n"
          + "and domain = @%s\n"
          + "and rnk <= @%s\n"
          + "order by rank, standardName, startDate\n";
  private static final String VOCAB_DATA_TEMPLATE =
      "SELECT distinct 'Standard' as type, 'ALL_EVENTS' as domain, standard_vocabulary as vocabulary\n"
          + "FROM `${projectId}.${dataSetId}.%1$s`\n"
          + "UNION ALL\n"
          + "SELECT distinct 'Standard' as type, domain, standard_vocabulary as vocabulary\n"
          + "FROM `${projectId}.${dataSetId}.%1$s`\n"
          + "UNION ALL\n"
          + "SELECT distinct 'Source' as type, 'ALL_EVENTS' as domain, source_vocabulary as vocabulary\n"
          + "FROM `${projectId}.${dataSetId}.%1$s`\n"
          + "where domain in ('CONDITION', 'PROCEDURE')\n"
          + "UNION ALL\n"
          + "SELECT distinct 'Source' as type, domain, source_vocabulary as vocabulary\n"
          + "FROM `${projectId}.${dataSetId}.%1$s`\n"
          + "where domain in ('CONDITION', 'PROCEDURE')\n"
          + "order by type, domain, vocabulary";

  public QueryJobConfiguration buildQuery(
      Long participantId, DomainType domain, PageRequest pageRequest) {
    return buildQueryJobConfiguration(participantId, domain, pageRequest, false);
  }

  public QueryJobConfiguration buildCountQuery(
      Long participantId, DomainType domain, PageRequest pageRequest) {
    return buildQueryJobConfiguration(participantId, domain, pageRequest, true);
  }

  public QueryJobConfiguration buildChartDataQuery(
      Long participantId, DomainType domain, Integer limit) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(PART_ID, QueryParameterValue.int64(participantId));
    params.put(DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
    params.put(LIMIT, QueryParameterValue.int64(limit));
    return QueryJobConfiguration.newBuilder(String.format(CHART_DATA_TEMPLATE, CHART_DATA_ARGS))
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildVocabularyDataQuery() {
    return QueryJobConfiguration.newBuilder(String.format(VOCAB_DATA_TEMPLATE, REVIEW_TABLE))
        .setUseLegacySql(false)
        .build();
  }

  private QueryJobConfiguration buildQueryJobConfiguration(
      Long participantId, DomainType domain, PageRequest pageRequest, boolean isCountQuery) {
    String finalSql;
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(PART_ID, QueryParameterValue.int64(participantId));

    switch (domain) {
      case SURVEY:
        finalSql =
            isCountQuery
                ? String.format(COUNT_SURVEY_TEMPLATE, SURVEY_TABLE, PART_ID)
                : String.format(
                    SURVEY_SQL_TEMPLATE + ORDER_BY, addOrderByArgs(SURVEY_ARGS, pageRequest));
        break;
      case ALL_EVENTS:
        finalSql =
            isCountQuery
                ? String.format(COUNT_TEMPLATE, REVIEW_TABLE, PART_ID)
                : String.format(
                    BASE_SQL_TEMPLATE + ORDER_BY, addOrderByArgs(BASE_SQL_ARGS, pageRequest));
        break;
      case CONDITION:
      case DEVICE:
      case DRUG:
      case LAB:
      case OBSERVATION:
      case PHYSICAL_MEASUREMENT:
      case PROCEDURE:
      case VISIT:
      case VITAL:
        finalSql =
            isCountQuery
                ? String.format(COUNT_TEMPLATE + DOMAIN_SQL, REVIEW_TABLE, PART_ID)
                : String.format(
                    BASE_SQL_TEMPLATE + DOMAIN_SQL + ORDER_BY,
                    addOrderByArgs(BASE_SQL_ARGS, pageRequest));
        params.put(DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
        break;
      default:
        throw new BadRequestException("There is no domain named: " + domain.toString());
    }
    return QueryJobConfiguration.newBuilder(finalSql)
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  private Object[] addOrderByArgs(Object[] args, PageRequest pageRequest) {
    List<Object> newArgs = new ArrayList<>(Arrays.asList(args));
    newArgs.add(pageRequest.getSortColumn());
    newArgs.add(pageRequest.getSortOrder().toString());
    newArgs.add(String.valueOf(pageRequest.getPageSize()));
    newArgs.add(String.valueOf(pageRequest.getPage() * pageRequest.getPageSize()));
    return newArgs.toArray(new Object[newArgs.size()]);
  }
}
