package org.pmiops.workbench.cdr.model;

import javax.persistence.*;


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
    private long countValue;
    private float prevalence;


    @Id
    @Column(name = "concept_id")
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

    @Column(name = "standard_concept")
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

    @Column(name = "concept_code")
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
    @Column(name = "concept_class_id")
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

    @Column(name = "vocabulary_id")
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

    @Column(name = "domain_id")
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


    @Column(name= "count_value")
    public long getCountValue() {
        return countValue;
    }

    public void setCountValue(long count) {
        this.countValue = count;
    }

    public Concept count(long count) {
        this.countValue = count;
        return this;
    }

    @Column(name = "prevalence")
    public float getPrevalence() {
        return prevalence;
    }

    public void setPrevalence(float prevalence) {
        this.prevalence = prevalence;
    }

    public Concept prevalence(float prevalence) {
        this.prevalence = prevalence;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Concept concept = (Concept) o;

        if (conceptId != concept.conceptId) return false;
        if (countValue != concept.countValue) return false;
        if (Float.compare(concept.prevalence, prevalence) != 0) return false;
        if (conceptName != null ? !conceptName.equals(concept.conceptName) : concept.conceptName != null) return false;
        if (standardConcept != null ? !standardConcept.equals(concept.standardConcept) : concept.standardConcept != null)
            return false;
        if (conceptCode != null ? !conceptCode.equals(concept.conceptCode) : concept.conceptCode != null) return false;
        if (conceptClassId != null ? !conceptClassId.equals(concept.conceptClassId) : concept.conceptClassId != null)
            return false;
        if (vocabularyId != null ? !vocabularyId.equals(concept.vocabularyId) : concept.vocabularyId != null)
            return false;
        return domainId != null ? domainId.equals(concept.domainId) : concept.domainId == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (conceptId ^ (conceptId >>> 32));
        result = 31 * result + (conceptName != null ? conceptName.hashCode() : 0);
        result = 31 * result + (standardConcept != null ? standardConcept.hashCode() : 0);
        result = 31 * result + (conceptCode != null ? conceptCode.hashCode() : 0);
        result = 31 * result + (conceptClassId != null ? conceptClassId.hashCode() : 0);
        result = 31 * result + (vocabularyId != null ? vocabularyId.hashCode() : 0);
        result = 31 * result + (domainId != null ? domainId.hashCode() : 0);
        result = 31 * result + (int) (countValue ^ (countValue >>> 32));
        result = 31 * result + (prevalence != +0.0f ? Float.floatToIntBits(prevalence) : 0);
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
                ", countValue=" + countValue +
                ", prevalence=" + prevalence +
                '}';
    }
}
