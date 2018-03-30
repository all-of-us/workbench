package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.model.Operator;

public class OperatorUtils {

  private static final ImmutableMap<Operator, String> operatorToSqlOperator =
      ImmutableMap.<Operator, String>builder()
          .put(Operator.EQUAL, "=")
          .put(Operator.GREATER_THAN, ">")
          .put(Operator.GREATER_THAN_OR_EQUAL_TO, ">=")
          .put(Operator.IN, "IN")
          .put(Operator.LESS_THAN, "<")
          .put(Operator.LESS_THAN_OR_EQUAL_TO, "<=")
          .put(Operator.LIKE, "LIKE")
          .build();

  public static String getSqlOperator(Operator operator) {
    String sqlOperator = operatorToSqlOperator.get(operator);
    if (sqlOperator == null) {
      throw new IllegalStateException("Invalid operator: " + operator);
    }
    return sqlOperator;
  }

}
