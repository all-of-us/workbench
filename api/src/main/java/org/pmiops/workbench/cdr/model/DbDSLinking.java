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
@Table(name = "ds_linking")
public class DbDSLinking {
  private long id;
  private String denormalizedName;
  private String omopSql;
  private String joinValue;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "denormalized_name")
  public String getDenormalizedName() {
    return denormalizedName;
  }

  public void setDenormalizedName(String denormalizedName) {
    this.denormalizedName = denormalizedName;
  }

  @Column(name = "omop_sql")
  public String getOmopSql() {
    return omopSql;
  }

  public void setOmopSql(String omopSql) {
    this.omopSql = omopSql;
  }

  @Column(name = "join_value")
  public String getJoinValue() {
    return joinValue;
  }

  public void setJoinValue(String joinValue) {
    this.joinValue = joinValue;
  }

  @Column(name = "domain")
  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  private String domain;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbDSLinking that = (DbDSLinking) o;
    return Objects.equals(denormalizedName, that.denormalizedName)
        && Objects.equals(omopSql, that.omopSql)
        && Objects.equals(joinValue, that.joinValue)
        && Objects.equals(domain, that.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(denormalizedName, omopSql, joinValue, domain);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static DbDSLinking.Builder builder() {
    return new DbDSLinking.Builder();
  }

  public static class Builder {
    private long id;
    private String denormalizedName;
    private String omopSql;
    private String joinValue;
    private String domain;

    private Builder() {}

    public DbDSLinking.Builder addId(long id) {
      this.id = id;
      return this;
    }

    public DbDSLinking.Builder addDenormalizedName(String denormalizedName) {
      this.denormalizedName = denormalizedName;
      return this;
    }

    public DbDSLinking.Builder addOmopSql(String omopSql) {
      this.omopSql = omopSql;
      return this;
    }

    public DbDSLinking.Builder addJoinValue(String joinValue) {
      this.joinValue = joinValue;
      return this;
    }

    public DbDSLinking.Builder addDomain(String domain) {
      this.domain = domain;
      return this;
    }

    public DbDSLinking build() {
      DbDSLinking dbDSLinking = new DbDSLinking();
      dbDSLinking.setId(this.id);
      dbDSLinking.setDenormalizedName(this.denormalizedName);
      dbDSLinking.setOmopSql(this.omopSql);
      dbDSLinking.setJoinValue(this.joinValue);
      dbDSLinking.setDomain(this.domain);
      return dbDSLinking;
    }
  }
}
