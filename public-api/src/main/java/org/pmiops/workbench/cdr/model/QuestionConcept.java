package org.pmiops.workbench.cdr.model;


import org.pmiops.workbench.cdr.model.AchillesResult;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "concept")
public class QuestionConcept {

    private long conceptId;
    private String conceptName;
    private String conceptCode;
    private String domainId;
    private long countValue;
    private float prevalence;
    private List<AchillesAnalysis> analyses = new ArrayList<>();

    @Id
    @Column(name = "concept_id")
    public long getConceptId() {
        return conceptId;
    }

    public void setConceptId(long conceptId) {
        this.conceptId = conceptId;
    }

    public QuestionConcept conceptId(long conceptId) {
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

    public QuestionConcept conceptName(String conceptName) {
        this.conceptName = conceptName;
        return this;
    }

    @Column(name = "concept_code")
    public String getConceptCode() {
        return conceptCode;
    }

    public void setConceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
    }

    public QuestionConcept conceptCode(String conceptCode) {
        this.conceptCode = conceptCode;
        return this;
    }

    @Column(name = "domain_id")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public QuestionConcept domainId(String domainId) {
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

    public QuestionConcept count(long count) {
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

    public QuestionConcept prevalence(float prevalence) {
        this.prevalence = prevalence;
        return this;
    }

    /*@OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name="stratum_2")
    public List<AchillesResult> getAnswers() {
        return answers;
    }
    public void setAnswers(List<AchillesResult> answers) {
        this.answers = answers;
    }
    public QuestionConcept answers(List<AchillesResult> answers) {
        this.answers = answers;
        return this;
    }*/

    @Transient
    public List<AchillesAnalysis> getAnalyses() {
        return analyses;
    }
    public void setAnalyses(List<AchillesAnalysis> analyses) {
        this.analyses = analyses;
    }
    public QuestionConcept analyses(List<AchillesAnalysis> analyses) {
        this.analyses = analyses;
        return this;
    }

    /*
    public AchillesAnalysis getAnalysisWithAnswers(long analysisId) {
        List<AchillesResult> allAnswers = this.getAnswers();
        List<AchillesResult> results = new ArrayList<>();
        AchillesAnalysis a = new AchillesAnalysis();

        for (AchillesResult r : allAnswers) {
            if (r.getAnalysisId() == analysisId) {
                results.add(r);
            }
        }
        // Pull analysis object off first result
        if (!results.isEmpty()) {
            a = results.get(0).getAnalysis();
            a.setResults(results);
            AchillesResult ar = a.getResults().get(0);
        } else {
            a.setAnalysisId(analysisId);
            a.setResults(results);
        }
        return a;
    }
    */
}
