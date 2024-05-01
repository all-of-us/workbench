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
@Table(name = "cb_criteria_attribute")
public class DbCriteriaAttribute {

  private long id;
  private long conceptId;
  private long valueAsConceptId;
  private String conceptName;
  private String type;
  private String estCount;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "concept_id")
  public long getConceptId() {
    return conceptId;
  }

  public void setConceptId(long conceptId) {
    this.conceptId = conceptId;
  }

  @Column(name = "value_as_concept_id")
  public long getValueAsConceptId() {
    return valueAsConceptId;
  }

  public void setValueAsConceptId(long valueAsConceptId) {
    this.valueAsConceptId = valueAsConceptId;
  }

  @Column(name = "concept_name")
  public String getConceptName() {
    return conceptName;
  }

  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }

  @Column(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Column(name = "est_count")
  public String getEstCount() {
    return estCount;
  }

  public void setEstCount(String estCount) {
    this.estCount = estCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbCriteriaAttribute that = (DbCriteriaAttribute) o;
    return conceptId == that.conceptId
        && valueAsConceptId == that.valueAsConceptId
        && Objects.equals(conceptName, that.conceptName)
        && Objects.equals(type, that.type)
        && Objects.equals(estCount, that.estCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, valueAsConceptId, conceptName, type, estCount);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static DbCriteriaAttribute.Builder builder() {
    return new DbCriteriaAttribute.Builder();
  }

  public static class Builder {
    private long id;
    private long conceptId;
    private long valueAsConceptId;
    private String conceptName;
    private String type;
    private String estCount;

    private Builder() {}

    public DbCriteriaAttribute.Builder addId(long id) {
      this.id = id;
      return this;
    }

    public DbCriteriaAttribute.Builder addConceptId(long conceptId) {
      this.conceptId = conceptId;
      return this;
    }

    public DbCriteriaAttribute.Builder addValueAsConceptId(long valueAsConceptId) {
      this.valueAsConceptId = valueAsConceptId;
      return this;
    }

    public DbCriteriaAttribute.Builder addConceptName(String conceptName) {
      this.conceptName = conceptName;
      return this;
    }

    public DbCriteriaAttribute.Builder addType(String type) {
      this.type = type;
      return this;
    }

    public DbCriteriaAttribute.Builder addEstCount(String estCount) {
      this.estCount = estCount;
      return this;
    }

    public DbCriteriaAttribute build() {
      DbCriteriaAttribute dbCriteriaAttribute = new DbCriteriaAttribute();
      dbCriteriaAttribute.setId(this.id);
      dbCriteriaAttribute.setConceptId(this.conceptId);
      dbCriteriaAttribute.setValueAsConceptId(this.valueAsConceptId);
      dbCriteriaAttribute.setConceptName(this.conceptName);
      dbCriteriaAttribute.setType(this.type);
      dbCriteriaAttribute.setEstCount(this.estCount);
      return dbCriteriaAttribute;
    }
  }
}
