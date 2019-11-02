package org.pmiops.workbench.utils

import com.google.common.collect.ImmutableMap
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.utils.OperatorUtils.operatorToSqlOperator

object OperatorUtils {

    private val operatorToSqlOperator = ImmutableMap.builder<Operator, String>()
            .put(Operator.EQUAL, "=")
            .put(Operator.NOT_EQUAL, "!=")
            .put(Operator.GREATER_THAN, ">")
            .put(Operator.GREATER_THAN_OR_EQUAL_TO, ">=")
            .put(Operator.IN, "IN")
            .put(Operator.LESS_THAN, "<")
            .put(Operator.LESS_THAN_OR_EQUAL_TO, "<=")
            .put(Operator.LIKE, "LIKE")
            .put(Operator.BETWEEN, "BETWEEN")
            .build()

    fun getSqlOperator(operator: Operator): String {
        return operatorToSqlOperator[operator]
                ?: throw IllegalStateException("Invalid operator: $operator")
    }
}
