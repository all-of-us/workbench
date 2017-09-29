package org.pmiops.workbench.api.util.query;

import org.pmiops.workbench.model.SearchParameter;

import java.util.List;

public class QueryParameters {

    private String type;
    private Long parentId;
    private List<String> codes;
    private List<SearchParameter> parameters;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public QueryParameters parentId(Long parentId) {
        this.parentId = parentId;
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

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }

    public QueryParameters codes(List<String> codes) {
        this.codes = codes;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryParameters that = (QueryParameters) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (parentId != null ? !parentId.equals(that.parentId) : that.parentId != null) return false;
        if (codes != null ? !codes.equals(that.codes) : that.codes != null) return false;
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        result = 31 * result + (codes != null ? codes.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}
