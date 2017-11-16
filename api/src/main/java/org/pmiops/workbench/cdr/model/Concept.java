package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "concept", catalog="cdr")
public class Concept {

    private long conceptId;
    private String conceptName;
    private String standardConcept;
    private String conceptCode;
    private String conceptClassId;
    private String vocabularyId;
    private String domainId;
    private long count;
    private long prevalence;


    @Id
    @Column(name = "conceptId")
    public long getConceptId() {
        return conceptId;
    }

    public void setConceptId(long conceptId) {
        this.conceptId = conceptId;
    }

    public Concept conceptId(long conceptId) {
        this.conceptId = conceptId;
        return this;
    }

    @Column(name = "concept_name")
    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public Concept conceptName(String conceptName) {
        this.conceptName = conceptName;
        return this;
    }

    @Column(name = "standardConcept")
    public String getStandardConcept() {
        return standardConcept;
    }

    public void setStandardConcept(String standardConcept) {
        this.standardConcept = standardConcept;
    }

    public Concept standardConcept(String standardConcept) {
        this.standardConcept = standardConcept;
        return this;
    }

    @Column(name = "conceptCode")
    public String getConceptCode() {
        return conceptCode;
    }

    public void setConceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
    }

    public Concept conceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
        return this;
    }
    @Column(name = "conceptClassId")
    public String getConceptClassId() {
        return conceptClassId;
    }

    public void setConceptClassId(String conceptClassId) {
        this.conceptClassId = conceptClassId;
    }

    public Concept conceptClassId(String conceptClassId) {
        this.conceptClassId = conceptClassId;
        return this;
    }

    @Column(name = "vocabularyId")
    public String getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(String vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

    public Concept vocabularyId(String vocabularyId) {
        this.vocabularyId = vocabularyId;
        return this;
    }

    @Column(name = "domainId")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Concept domainId(String domainId) {
        this.domainId = domainId;
        return this;
    }


    @Transient
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Concept count(long count) {
        this.count = count;
        return this;
    }

    @Transient
    public long getPrevalence() {
        return prevalence;
    }

    public void setPrevalence(long prevalence) {
        this.prevalence = prevalence;
    }

    public Concept prevalence(long prevalence) {
        this.prevalence = prevalence;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Concept concept = (Concept) o;

        if (conceptId != concept.conceptId) return false;
       // if (count != concept.count) return false;
       // if (prevalence != concept.prevalence) return false;
        if (!conceptName.equals(concept.conceptName)) return false;
        if (!standardConcept.equals(concept.standardConcept)) return false;
        if (!conceptCode.equals(concept.conceptCode)) return false;
        if (!conceptClassId.equals(concept.conceptClassId)) return false;
        if (!vocabularyId.equals(concept.vocabularyId)) return false;
        return domainId.equals(concept.domainId);
    }

    @Override
    public int hashCode() {
        int result = (int) (conceptId ^ (conceptId >>> 32));
        result = 31 * result + conceptName.hashCode();
        result = 31 * result + standardConcept.hashCode();
        result = 31 * result + conceptCode.hashCode();
        result = 31 * result + conceptClassId.hashCode();
        result = 31 * result + vocabularyId.hashCode();
        result = 31 * result + domainId.hashCode();
        result = 31 * result + (int) (count ^ (count >>> 32));
        result = 31 * result + (int) (prevalence ^ (prevalence >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Concept{" +
                "conceptId=" + conceptId +
                ", conceptName='" + conceptName + '\'' +
                ", standardConcept='" + standardConcept + '\'' +
                ", conceptCode='" + conceptCode + '\'' +
                ", conceptClassId='" + conceptClassId + '\'' +
                ", vocabularyId='" + vocabularyId + '\'' +
                ", domainId='" + domainId + '\'' +
                ", count=" + count +
                ", prevalence=" + prevalence +
                '}';
    }
}
