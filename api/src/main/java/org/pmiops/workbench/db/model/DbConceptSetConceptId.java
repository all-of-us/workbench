package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;

@Embeddable
@Table(name = "concept_set_concept_id")
public class DbConceptSetConceptId {
  private Long conceptId;
  private Boolean standard;

  @Column(name = "concept_id")
  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  @Column(name = "is_standard")
  public Boolean getStandard() {
    return standard;
  }

  public void setStandard(Boolean standard) {
    this.standard = standard;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbConceptSetConceptId that = (DbConceptSetConceptId) o;
    return Objects.equals(conceptId, that.conceptId) && Objects.equals(standard, that.standard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, standard);
  }

  public static DbConceptSetConceptId.Builder builder() {
    return new DbConceptSetConceptId.Builder();
  }

  public static class Builder {
    private Long conceptId;
    private Boolean standard;

    private Builder() {}

    public DbConceptSetConceptId.Builder addConceptId(long conceptId) {
      this.conceptId = conceptId;
      return this;
    }

    public DbConceptSetConceptId.Builder addStandard(boolean standard) {
      this.standard = standard;
      return this;
    }

    public DbConceptSetConceptId build() {
      DbConceptSetConceptId dbConceptSetConceptId = new DbConceptSetConceptId();
      dbConceptSetConceptId.setConceptId(this.conceptId);
      dbConceptSetConceptId.setStandard(this.standard);
      return dbConceptSetConceptId;
    }
  }
}
