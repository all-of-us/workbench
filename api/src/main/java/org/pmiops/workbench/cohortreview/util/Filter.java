package org.pmiops.workbench.cohortreview.util;

public class Filter {
    private String property;
    private Operator operator;
    private String value;

    public Filter() {}

    public Filter(final String property, final Operator operator, final String value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(final String property) {
        this.property = property;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(final Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
