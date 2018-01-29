package org.pmiops.workbench.cohortreview.util;

public enum FilterOperation {

    EQUALS("=");

    private final String name;

    private FilterOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
