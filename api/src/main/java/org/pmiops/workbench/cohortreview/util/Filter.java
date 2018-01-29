package org.pmiops.workbench.cohortreview.util;

public class Filter {
    private String property;
    private FilterOperation operation;
    private String value;

    public Filter() {

    }

    public Filter(final String property, final FilterOperation operation, final String value) {
        super();
        this.property = property;
        this.operation = operation;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(final String property) {
        this.property = property;
    }

    public FilterOperation getOperation() {
        return operation;
    }

    public void setOperation(final FilterOperation operation) {
        this.operation = operation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
