package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String SYSTOLIC_MEAN_CONCEPT = "903118";
  private static final String DIASTOLIC_MEAN_CONCEPT = "903115";
  private static final String HYPOTENSIVE_SYSTOLIC_VALUE = "90";
  private static final String HYPOTENSIVE_DIASTOLIC_VALUE = "60";
  private static final String NORMAL_SYSTOLIC_VALUE = "120";
  private static final String NORMAL_DIASTOLIC_VALUE = "80";
  private static final String PRE_HYPERTENSIVE_SYSTOLIC_VALUEs = "121 and 139";
  private static final String PRE_HYPERTENSIVE_DIASTOLIC_VALUES = "81 and 89";
  private static final String HYPERTENSIVE_SYSTOLIC_VALUEs = "140";
  private static final String HYPERTENSIVE_DIASTOLIC_VALUES = "90";

  private static final String BP_SQL_TEMPLATE =
    "select person_id from(\n" +
      "  select person_id, measurement_date from `${projectId}.${dataSetId}.measurement`\n" +
      "  where measurement_source_concept_id = " + DIASTOLIC_MEAN_CONCEPT + "\n" +
      "  and value_as_number ${diastolicOperator} ${diastolicValue}\n" +
      "  intersect distinct\n" +
      "  select person_id, measurement_date from `${projectId}.${dataSetId}.measurement`\n" +
      "  where measurement_source_concept_id = " + SYSTOLIC_MEAN_CONCEPT + "\n" +
      "  and value_as_number ${systolicOperator} ${systolicValue})";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
//    Map<String, QueryParameterValue> params = new HashMap<>();
//    params.put("limit", QueryParameterValue.newBuilder().setValue("1").build());
    String finalSql = "";
    for (SearchParameter parameter : parameters.getParameters()) {
      if (parameter.getSubtype().equals("BP-Hypotensive")) {
        finalSql = BP_SQL_TEMPLATE.replace("${diastolicOperator}", "<=")
        .replace("${systolicOperator}", "<=")
        .replace("${diastolicValue}", HYPOTENSIVE_DIASTOLIC_VALUE)
        .replace("${systolicValue}", HYPOTENSIVE_SYSTOLIC_VALUE);
      } else if (parameter.getSubtype().equals("BP-Normal")) {
        finalSql = BP_SQL_TEMPLATE.replace("${diastolicOperator}", "<=")
          .replace("${systolicOperator}", "<=")
          .replace("${diastolicValue}", NORMAL_DIASTOLIC_VALUE)
          .replace("${systolicValue}", NORMAL_SYSTOLIC_VALUE);
      } else if (parameter.getSubtype().equals("BP-Pre-Hypertensive")) {
        finalSql = BP_SQL_TEMPLATE.replace("${diastolicOperator}", "between")
          .replace("${systolicOperator}", "between")
          .replace("${diastolicValue}", PRE_HYPERTENSIVE_DIASTOLIC_VALUES)
          .replace("${systolicValue}", PRE_HYPERTENSIVE_SYSTOLIC_VALUEs);
      } else if (parameter.getSubtype().equals("BP-Hypertensive")) {
        finalSql = BP_SQL_TEMPLATE.replace("${diastolicOperator}", "between")
          .replace("${systolicOperator}", "between")
          .replace("${diastolicValue}", PRE_HYPERTENSIVE_DIASTOLIC_VALUES)
          .replace("${systolicValue}", PRE_HYPERTENSIVE_SYSTOLIC_VALUEs);
      } else if (parameter.getSubtype().equals("BP-User-Defined")) {
//        finalSql = BP_SQL_TEMPLATE.replace("${diastolicOperator}", parameter.getAttribute().getOperator())
//          .replace("${systolicOperator}", parameter.getAttribute().getOperator())
//          .replace("${diastolicValue}", parameter.getAttribute().getOperands().get(0))
//          .replace("${systolicValue}", parameter.getAttribute().getOperands().get(1));
      }
    }
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PM;
  }
}