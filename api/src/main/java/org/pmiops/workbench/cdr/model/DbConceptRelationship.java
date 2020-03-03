package org.pmiops.workbench.cdr.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "concept_relationship")
public class DbConceptRelationship {

  private DbConceptRelationshipId conceptRelationshipId;

  @EmbeddedId
  @AttributeOverrides({
    @AttributeOverride(name = "conceptId1", column = @Column(name = "concept_id_1")),
    @AttributeOverride(name = "conceptId2", column = @Column(name = "concept_id_2")),
    @AttributeOverride(name = "relationshipId", column = @Column(name = "relationship_id"))
  })
  public DbConceptRelationshipId getConceptRelationshipId() {
    return conceptRelationshipId;
  }

  public void setConceptRelationshipId(DbConceptRelationshipId conceptRelationshipId) {
    this.conceptRelationshipId = conceptRelationshipId;
  }

  public DbConceptRelationship conceptRelationshipId(
      DbConceptRelationshipId conceptRelationshipId) {
    this.conceptRelationshipId = conceptRelationshipId;
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("conceptRelationshipId", conceptRelationshipId)
        .toString();
  }
}
