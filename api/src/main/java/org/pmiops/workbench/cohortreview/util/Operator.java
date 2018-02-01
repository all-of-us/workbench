package org.pmiops.workbench.cohortreview.util;

public enum Operator {

    EQUALS("=");

    private final String expression;

    private Operator(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return this.expression;
    }
}
