package org.pmiops.workbench.cohortreview;

import static org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT;
import static org.pmiops.workbench.model.FilterColumns.ANSWER;
import static org.pmiops.workbench.model.FilterColumns.DOMAIN;
import static org.pmiops.workbench.model.FilterColumns.DOSE;
import static org.pmiops.workbench.model.FilterColumns.FIRST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.LAST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.NUM_MENTIONS;
import static org.pmiops.workbench.model.FilterColumns.QUESTION;
import static org.pmiops.workbench.model.FilterColumns.REF_RANGE;
import static org.pmiops.workbench.model.FilterColumns.ROUTE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CODE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_NAME;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_VOCABULARY;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CODE;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_NAME;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_VOCABULARY;
import static org.pmiops.workbench.model.FilterColumns.START_DATETIME;
import static org.pmiops.workbench.model.FilterColumns.STRENGTH;
import static org.pmiops.workbench.model.FilterColumns.SURVEY_NAME;
import static org.pmiops.workbench.model.FilterColumns.UNIT;
import static org.pmiops.workbench.model.FilterColumns.VALUE_AS_NUMBER;
import static org.pmiops.workbench.model.FilterColumns.VISIT_TYPE;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;
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
        START_DATETIME.toString(),
        DOMAIN.toString(),
        STANDARD_NAME.toString(),
        STANDARD_CODE.toString(),
        STANDARD_VOCABULARY.toString(),
        STANDARD_CONCEPT_ID.toString(),
        SOURCE_NAME.toString(),
        SOURCE_CODE.toString(),
        SOURCE_VOCABULARY.toString(),
        SOURCE_CONCEPT_ID.toString(),
        AGE_AT_EVENT.toString(),
        VISIT_TYPE.toString(),
        ROUTE.toString(),
        DOSE.toString(),
        STRENGTH.toString(),
        UNIT.toString(),
        REF_RANGE.toString(),
        NUM_MENTIONS.toString(),
        FIRST_MENTION.toString(),
        LAST_MENTION.toString(),
        VALUE_AS_NUMBER.toString(),
        REVIEW_TABLE,
        PART_ID
      };
  private static final Object[] SURVEY_ARGS =
      new Object[] {
        START_DATETIME.toString(),
        SURVEY_NAME.toString(),
        QUESTION.toString(),
        ANSWER.toString(),
        SURVEY_TABLE,
        PART_ID
      };

  private static final ImmutableList<FilterColumns> LONG_NUMBERS =
      ImmutableList.of(AGE_AT_EVENT, NUM_MENTIONS);
  private static final ImmutableList<FilterColumns> DOUBLE_NUMBERS =
      ImmutableList.of(VALUE_AS_NUMBER);

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
  private static final String AND = "and";
  private static final String UNNEST = "unnest";
  private static final String NEW_LINE = "\n";
  private static final String OPEN_PAREN = "(";
  private static final String CLOSE_PAREN = ")";

  public QueryJobConfiguration buildQuery(
      Long participantId, Domain domain, PageRequest pageRequest) {
    return buildQueryJobConfiguration(participantId, domain, pageRequest, false);
  }

  public QueryJobConfiguration buildCountQuery(
      Long participantId, Domain domain, PageRequest pageRequest) {
    return buildQueryJobConfiguration(participantId, domain, pageRequest, true);
  }

  public QueryJobConfiguration buildVocabularyDataQuery() {
    return QueryJobConfiguration.newBuilder(String.format(VOCAB_DATA_TEMPLATE, REVIEW_TABLE))
        .setUseLegacySql(false)
        .build();
  }

  private QueryJobConfiguration buildQueryJobConfiguration(
      Long participantId, Domain domain, PageRequest pageRequest, boolean isCountQuery) {
    String finalSql;
    Map<String, QueryParameterValue> params = new HashMap<>();
    List<Filter> filters = pageRequest.getFilters();
    params.put(PART_ID, QueryParameterValue.int64(participantId));

    switch (domain) {
      case SURVEY:
        finalSql =
            isCountQuery
                ? String.format(
                    COUNT_SURVEY_TEMPLATE + filterBy(filters, params), SURVEY_TABLE, PART_ID)
                : String.format(
                    SURVEY_SQL_TEMPLATE + filterBy(filters, params) + ORDER_BY,
                    addOrderByArgs(SURVEY_ARGS, pageRequest));
        break;
      case ALL_EVENTS:
        finalSql =
            isCountQuery
                ? String.format(COUNT_TEMPLATE + filterBy(filters, params), REVIEW_TABLE, PART_ID)
                : String.format(
                    BASE_SQL_TEMPLATE + filterBy(filters, params) + ORDER_BY,
                    addOrderByArgs(BASE_SQL_ARGS, pageRequest));
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
                ? String.format(
                    COUNT_TEMPLATE + filterBy(filters, params) + DOMAIN_SQL, REVIEW_TABLE, PART_ID)
                : String.format(
                    BASE_SQL_TEMPLATE + filterBy(filters, params) + DOMAIN_SQL + ORDER_BY,
                    addOrderByArgs(BASE_SQL_ARGS, pageRequest));
        params.put(DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
        break;
      default:
        // Not supported for other domain(s) : MEASUREMENT, DEATH, FITBIT....
        throw new BadRequestException("Not supported for domain named: " + domain);
    }
    return QueryJobConfiguration.newBuilder(finalSql)
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  private String filterBy(List<Filter> filters, Map<String, QueryParameterValue> params) {
    StringBuilder filterSql = new StringBuilder();
    for (Filter filter : filters) {
      StringJoiner stringJoiner = new StringJoiner(" ", AND + " ", "");
      switch (filter.getOperator()) {
        case EQUAL:
        case NOT_EQUAL:
        case LESS_THAN:
        case GREATER_THAN:
        case LESS_THAN_OR_EQUAL_TO:
        case GREATER_THAN_OR_EQUAL_TO:
        case LIKE:
          stringJoiner
              .add(addLowerColumnName(filter))
              .add(OperatorUtils.getSqlOperator(filter.getOperator()))
              .add(QueryParameterValues.buildParameter(params, buildQueryParameterValue(filter, 0)))
              .add(NEW_LINE);
          break;
        case IN:
          stringJoiner
              .add(addLowerColumnName(filter))
              .add(OperatorUtils.getSqlOperator(filter.getOperator()))
              .add(UNNEST)
              .add(OPEN_PAREN)
              .add(QueryParameterValues.buildParameter(params, buildQueryParameterValue(filter)))
              .add(CLOSE_PAREN)
              .add(NEW_LINE);
          break;
        case BETWEEN:
          stringJoiner
              .add(filter.getProperty().toString())
              .add(OperatorUtils.getSqlOperator(filter.getOperator()))
              .add(QueryParameterValues.buildParameter(params, buildQueryParameterValue(filter, 0)))
              .add(AND)
              .add(QueryParameterValues.buildParameter(params, buildQueryParameterValue(filter, 1)))
              .add(NEW_LINE);
          break;
        default:
          throw new BadRequestException(
              "There is no implementation for operator named: "
                  + OperatorUtils.getSqlOperator(filter.getOperator()));
      }
      filterSql.append(stringJoiner.toString());
    }
    return filterSql.toString();
  }

  private String addLowerColumnName(Filter filter) {
    if (LONG_NUMBERS.contains(filter.getProperty())
        || DOUBLE_NUMBERS.contains(filter.getProperty())) {
      return filter.getProperty().toString();
    }
    return "lower(" + filter.getProperty().toString() + ")";
  }

  private QueryParameterValue buildQueryParameterValue(Filter filter, int index) {
    if (LONG_NUMBERS.contains(filter.getProperty())) {
      return QueryParameterValue.int64(Long.valueOf(filter.getValues().get(index)));
    } else if (DOUBLE_NUMBERS.contains(filter.getProperty())) {
      return QueryParameterValue.float64(Double.valueOf(filter.getValues().get(index)));
    }
    return filter.getOperator().equals(Operator.LIKE)
        ? QueryParameterValue.string(filter.getValues().get(index).toLowerCase() + "%")
        : QueryParameterValue.string(filter.getValues().get(index).toLowerCase());
  }

  private QueryParameterValue buildQueryParameterValue(Filter filter) {
    if (LONG_NUMBERS.contains(filter.getProperty())) {
      return QueryParameterValue.array(filter.getValues().toArray(new Long[0]), Long.class);
    } else if (DOUBLE_NUMBERS.contains(filter.getProperty())) {
      return QueryParameterValue.array(filter.getValues().toArray(new Double[0]), Double.class);
    }
    return QueryParameterValue.array(
        filter.getValues().stream().map(String::toLowerCase).toArray(String[]::new), String.class);
  }

  private Object[] addOrderByArgs(Object[] args, PageRequest pageRequest) {
    List<Object> newArgs = new ArrayList<>(Arrays.asList(args));
    newArgs.add(pageRequest.getSortColumn());
    newArgs.add(pageRequest.getSortOrder().toString());
    newArgs.add(String.valueOf(pageRequest.getPageSize()));
    newArgs.add(String.valueOf(pageRequest.getPage() * pageRequest.getPageSize()));
    return newArgs.toArray(new Object[0]);
  }
}
