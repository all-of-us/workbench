package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
// NOTE: This class and ConceptSynonymDao exist only to make CriteriaDao work in tests;
// if we stop using concept_synonym there at some point we can get rid of them.
@Table(name = "concept_synonym")
public class DbConceptSynonym {

  private long id;
  private long conceptId;
  private DbConcept concept;
  private String conceptSynonymName;

  @Id
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DbConceptSynonym id(Long val) {
    this.id = val;
    return this;
  }

  @Column(name = "concept_id")
  public long getConceptId() {
    return conceptId;
  }

  public void setConceptId(long conceptId) {
    this.conceptId = conceptId;
  }

  public DbConceptSynonym conceptId(long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concept_id", insertable = false, updatable = false)
  public DbConcept getConcept() {
    return concept;
  }

  public void setConcept(DbConcept concept) {
    this.concept = concept;
  }

  public DbConceptSynonym conceptSynonym(DbConcept concept) {
    this.concept = concept;
    return this;
  }

  @Column(name = "concept_synonym_name")
  public String getConceptSynonymName() {
    return conceptSynonymName;
  }

  public void setConceptSynonymName(String conceptSynonymName) {
    this.conceptSynonymName = conceptSynonymName;
  }

  public DbConceptSynonym conceptSynonymName(String conceptSynonymName) {
    this.conceptSynonymName = conceptSynonymName;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbConceptSynonym conceptSynonym = (DbConceptSynonym) o;
    return conceptId == conceptSynonym.conceptId
        && Objects.equals(conceptSynonymName, conceptSynonym.conceptSynonymName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, concept, conceptSynonymName);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
