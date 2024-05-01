package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "cb_data_filter")
public class DbDataFilter {

  private long dataFilterId;
  private String displayName;
  private String name;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_filter_id")
  public long getDataFilterId() {
    return dataFilterId;
  }

  public void setDataFilterId(long dataFilterId) {
    this.dataFilterId = dataFilterId;
  }

  @Column(name = "display_name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbDataFilter dbDataFilter = (DbDataFilter) o;
    return Objects.equals(displayName, dbDataFilter.displayName)
        && Objects.equals(name, dbDataFilter.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataFilterId, displayName, name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static DbDataFilter.Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long dataFilterId;
    private String displayName;
    private String name;

    private Builder() {}

    public DbDataFilter.Builder addDataFilterId(long dataFilterId) {
      this.dataFilterId = dataFilterId;
      return this;
    }

    public DbDataFilter.Builder addDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public DbDataFilter.Builder addName(String name) {
      this.name = name;
      return this;
    }

    public DbDataFilter build() {
      DbDataFilter dbDataFilter = new DbDataFilter();
      dbDataFilter.setDataFilterId(this.dataFilterId);
      dbDataFilter.setDisplayName(this.displayName);
      dbDataFilter.setName(this.name);
      return dbDataFilter;
    }
  }
}
