package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DbConceptRelationshipId implements Serializable {

  @Column(name = "concept_id_1")
  long conceptId1;

  @Column(name = "concept_id_2")
  long conceptId2;

  @Column(name = "relationship_id")
  String relationshipId;

  public DbConceptRelationshipId() {}

  public DbConceptRelationshipId(long conceptId1, long conceptId2, String relationshipId) {
    this.conceptId1 = conceptId1;
    this.conceptId2 = conceptId2;
    this.relationshipId = relationshipId;
  }

  public long getConceptId1() {
    return conceptId1;
  }

  public void setConceptId1(long conceptId1) {
    this.conceptId1 = conceptId1;
  }

  public DbConceptRelationshipId conceptId1(long conceptId1) {
    this.conceptId1 = conceptId1;
    return this;
  }

  public long getConceptId2() {
    return conceptId2;
  }

  public void setConceptId2(long conceptId2) {
    this.conceptId2 = conceptId2;
  }

  public DbConceptRelationshipId conceptId2(long conceptId2) {
    this.conceptId2 = conceptId2;
    return this;
  }

  public String getRelationshipId() {
    return relationshipId;
  }

  public void setRelationshipId(String relationshipId) {
    this.relationshipId = relationshipId;
  }

  public DbConceptRelationshipId relationshipId(String relationshipId) {
    this.relationshipId = relationshipId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbConceptRelationshipId that = (DbConceptRelationshipId) o;
    return conceptId1 == that.conceptId1
        && conceptId2 == that.conceptId2
        && relationshipId == that.relationshipId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId1, conceptId2, relationshipId);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("conceptId1", conceptId1)
        .append("conceptId2", conceptId2)
        .append("relationshipId", relationshipId)
        .toString();
  }
}
