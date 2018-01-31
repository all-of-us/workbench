package org.pmiops.workbench.cohortreview.util;

public enum SearchOperation {

    EQUALS("=");

    private final String name;

    private SearchOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
