package org.pmiops.workbench.cohortreview.util;

public class Filter {
    private String property;
    private SearchOperation operation;
    private String value;

    public Filter() {}

    public Filter(final String property, final SearchOperation operation, final String value) {
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

    public SearchOperation getOperation() {
        return operation;
    }

    public void setOperation(final SearchOperation operation) {
        this.operation = operation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
