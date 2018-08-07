package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "concept_synonym")
public class ConceptSynonym {

    private long id;
    private long conceptId;
    private Concept concept;
    private String conceptSynonymName;
    private long languageConceptId;

    @Id
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public ConceptSynonym id(Long val) {
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

    public ConceptSynonym conceptId(long conceptId) {
        this.conceptId = conceptId;
        return this;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="concept_id", insertable=false, updatable=false)
    public Concept getConcept() {
        return concept;
    }
    public void setConcept(Concept concept) {
        this.concept = concept;
    }
    public ConceptSynonym conceptSynonym(Concept concept) {
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

    public ConceptSynonym conceptSynonymName(String conceptSynonymName) {
        this.conceptSynonymName = conceptSynonymName;
        return this;
    }

    @Column(name = "language_concept_id")
    public long getLanguageConceptId() {
        return languageConceptId;
    }

    public void setLanguageConceptId(long languageConceptId) {
        this.languageConceptId = languageConceptId;
    }

    public ConceptSynonym languageConceptId(long languageConceptId) {
        this.languageConceptId = languageConceptId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptSynonym conceptSynonym = (ConceptSynonym) o;
        return conceptId == conceptSynonym.conceptId &&
                Objects.equals(conceptSynonymName, conceptSynonym.conceptSynonymName) &&
                Objects.equals(languageConceptId, conceptSynonym.languageConceptId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conceptId, concept, conceptSynonymName, languageConceptId);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);

    }
}
