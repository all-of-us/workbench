package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalTime;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TemporalQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the Temporal criteria groups.
 */
@Service
public class TemporalQueryBuilder {

  private static final String UNION_TEMPLATE = "union all\n";
  private static final String SAME_ENC =
    "temp1.person_id = temp2.person_id and temp1.enc_id = temp2.enc_id\n";
  private static final String X_DAYS_BEFORE =
    "temp1.person_id = temp2.person_id and temp1.entry_date < DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String X_DAYS_AFTER =
    "temp1.person_id = temp2.person_id and temp1." +
      "entry_date > DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
    "temp1.person_id = temp2.person_id and temp1.entry_date between " +
      "temp2.entry_date - ${timeValue} and temp2.entry_date + ${timeValue}\n";
  private static final String TEMPORAL_TEMPLATE =
    "select count(distinct temp1.person_id)\n" +
      "from (${query1}) temp1\n" +
      "where exists (select 1\n" +
      "from (${query2}) temp2\n" +
      "where (${conditions}))\n";

  public String buildQuery(Map<String, QueryParameterValue> params,
                           SearchGroup includeGroup) {
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(includeGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      for (SearchGroupItem tempGroup : tempGroups) {
        String query = QueryBuilderFactory
          .getQueryBuilder(FactoryKey.getType(tempGroup.getType()))
          .buildQuery(params, tempGroup, true);
        if (key == 0) {
          temporalQueryParts1.add(query);
        } else {
          temporalQueryParts2.add(query);
        }
      }
    }
    String query1 = String.join(UNION_TEMPLATE, temporalQueryParts1);
    String query2 = String.join(UNION_TEMPLATE, temporalQueryParts2);
    String conditions = SAME_ENC;
    if (includeGroup.getTime().equals(TemporalTime.DURING_SAME_ENCOUNTER_AS.toString())) {
      conditions = WITHIN_X_DAYS_OF.replace("${timeValue}", includeGroup.getTimeValue().toString());
    } else if (includeGroup.getTime().equals(TemporalTime.X_DAYS_BEFORE.toString())) {
      conditions = X_DAYS_BEFORE.replace("${timeValue}", includeGroup.getTimeValue().toString());
    } else if (includeGroup.getTime().equals(TemporalTime.X_DAYS_AFTER.toString())) {
      conditions = X_DAYS_AFTER.replace("${timeValue}", includeGroup.getTimeValue().toString());
    }
    return TEMPORAL_TEMPLATE.replace("${query1}", query1)
      .replace("${query2}", query2)
      .replace("${conditions}", conditions);
  }

  private ListMultimap<Integer, SearchGroupItem> getTemporalGroups(SearchGroup searchGroup) {
    ListMultimap<Integer, SearchGroupItem> itemMap = ArrayListMultimap.create();
    searchGroup.getItems()
      .forEach(item -> itemMap.put(item.getTemporalGroup(), item));
    return itemMap;
  }
}
