package org.pmiops.workbench.cohortbuilder.querybuilder;

import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is an adapter that allows for the wrapping of all parameters
 * that the {@link AbstractQueryBuilder} implementations are interested
 * in.
 */
public class QueryParameters {

    private String type;
    private List<Modifier> modifiers = new ArrayList<>();
    private List<SearchParameter> parameters = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public QueryParameters type(String type) {
        this.type = type;
        return this;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public QueryParameters modifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public List<SearchParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<SearchParameter> parameters) {
        this.parameters = parameters;
    }

    public QueryParameters parameters(List<SearchParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryParameters that = (QueryParameters) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (modifiers != null ? !modifiers.equals(that.modifiers) : that.modifiers != null) return false;
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (modifiers != null ? modifiers.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}
