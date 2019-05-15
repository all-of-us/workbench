package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.MENTION;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SEARCH_GROUP;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SEARCH_GROUP_ITEM;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TEMPORAL_GROUP;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TEMPORAL_GROUP_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TIME;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TIME_VALUE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.mentionInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.temporalGroupNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.notZeroAndNotOne;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.timeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.timeValueNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.timeValueRequired;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * TemporalQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the Temporal criteria groups. The temporal group functionality description
 * is here: https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
 */
@Service
public class BaseQueryBuilder {

  private static final String UNION_TEMPLATE = "union all\n";
  private static final String SAME_ENC =
    "temp1.person_id = temp2.person_id and temp1.visit_occurrence_id = temp2.visit_occurrence_id\n";
  private static final String X_DAYS_BEFORE =
    "temp1.person_id = temp2.person_id and temp1.entry_date <= DATE_SUB(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String X_DAYS_AFTER =
    "temp1.person_id = temp2.person_id and temp1." +
      "entry_date >= DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
    "temp1.person_id = temp2.person_id and temp1.entry_date between " +
      "DATE_SUB(temp2.entry_date, INTERVAL ${timeValue} DAY) and DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String TEMPORAL_EXIST_TEMPLATE = "select temp1.person_id\n" +
      "from (${query1}) temp1\n" +
      "where exists (select 1\n" +
      "from (${query2}) temp2\n" +
      "where (${conditions}))\n";
  private static final String TEMPORAL_JOIN_TEMPLATE = "select temp1.person_id\n" +
      "from (${query1}) temp1\n" +
      "join (select person_id, visit_occurrence_id, entry_date\n" +
      "from (${query2})\n" +
      ") temp2 on (${conditions})\n";

  private static final Logger log = Logger.getLogger(BaseQueryBuilder.class.getName());
  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  public BaseQueryBuilder(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  public void buildQuery(Map<String, QueryParameterValue> params, List<String> queryParts, SearchGroup searchGroup) {
    if (searchGroup.getTemporal()) {
      String query = buildTemporalQuery(params, searchGroup);
      queryParts.add(query);
    } else {
      if (configProvider.get().cohortbuilder.enableListSearch) {
        log.log(Level.INFO, "new search here!");
      } else {
        for (SearchGroupItem includeItem : searchGroup.getItems()) {
          String query = QueryBuilderFactory
            .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
            .buildQuery(params, includeItem, searchGroup.getMention());
          queryParts.add(query);
        }
      }
    }
  }

  public String buildTemporalQuery(Map<String, QueryParameterValue> params,
                                   SearchGroup includeGroup) {
    validateSearchGroup(includeGroup);
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(includeGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      //key of zero indicates belonging to the first temporal group
      //key of one indicates belonging to the second temporal group
      boolean isFirstGroup = key == 0;
      String query = "";
      for (SearchGroupItem tempGroup : tempGroups) {
        if (configProvider.get().cohortbuilder.enableListSearch) {
          log.log(Level.INFO, "new search here!");
        } else {
          query = QueryBuilderFactory
            .getQueryBuilder(FactoryKey.getType(tempGroup.getType()))
            .buildQuery(params, tempGroup,
              isFirstGroup ? includeGroup.getMention() : TemporalMention.ANY_MENTION);
        }
        if (isFirstGroup) {
          temporalQueryParts1.add(query);
        } else {
          temporalQueryParts2.add(query);
        }
      }
    }
    String conditions = SAME_ENC;
    if (TemporalTime.WITHIN_X_DAYS_OF.equals(includeGroup.getTime())) {
      String parameterName = "p" + params.size();
      params.put(parameterName, QueryParameterValue.int64(includeGroup.getTimeValue()));
      conditions = WITHIN_X_DAYS_OF.replace("${timeValue}", "@" + parameterName);
    } else if (TemporalTime.X_DAYS_BEFORE.equals(includeGroup.getTime())) {
      String parameterName = "p" + params.size();
      params.put(parameterName, QueryParameterValue.int64(includeGroup.getTimeValue()));
      conditions = X_DAYS_BEFORE.replace("${timeValue}", "@" + parameterName);
    } else if (TemporalTime.X_DAYS_AFTER.equals(includeGroup.getTime())) {
      String parameterName = "p" + params.size();
      params.put(parameterName, QueryParameterValue.int64(includeGroup.getTimeValue()));
      conditions = X_DAYS_AFTER.replace("${timeValue}", "@" + parameterName);
    }
    return (temporalQueryParts2.size() == 1 ?
      TEMPORAL_EXIST_TEMPLATE : TEMPORAL_JOIN_TEMPLATE)
      .replace("${query1}", String.join(UNION_TEMPLATE, temporalQueryParts1))
      .replace("${query2}", String.join(UNION_TEMPLATE, temporalQueryParts2))
      .replace("${conditions}", conditions);
  }

  private ListMultimap<Integer, SearchGroupItem> getTemporalGroups(SearchGroup searchGroup) {
    ListMultimap<Integer, SearchGroupItem> itemMap = ArrayListMultimap.create();
    searchGroup.getItems()
      .forEach(item -> {
        from(temporalGroupNull()).test(item)
          .throwException(NOT_VALID_MESSAGE, SEARCH_GROUP_ITEM, TEMPORAL_GROUP, item.getTemporalGroup());
        itemMap.put(item.getTemporalGroup(), item);
      });
    from(notZeroAndNotOne()).test(itemMap).throwException(TEMPORAL_GROUP_MESSAGE);
    return itemMap;
  }

  private void validateSearchGroup(SearchGroup searchGroup) {
    from(mentionInvalid()).test(searchGroup).throwException(NOT_VALID_MESSAGE, SEARCH_GROUP, MENTION, searchGroup.getMention());
    from(timeInvalid()).test(searchGroup).throwException(NOT_VALID_MESSAGE, SEARCH_GROUP, TIME, searchGroup.getTime());
    from(timeValueNull().and(timeValueRequired())).test(searchGroup).throwException(NOT_VALID_MESSAGE, SEARCH_GROUP, TIME_VALUE, searchGroup.getTimeValue());
  }
}
