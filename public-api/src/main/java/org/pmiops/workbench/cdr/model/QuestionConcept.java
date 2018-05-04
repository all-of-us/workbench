package org.pmiops.workbench.cdr.model;


import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "concept")
public class QuestionConcept {

    private long conceptId;
    private String conceptName;
    private String conceptCode;
    private String domainId;
    private long countValue;
    private float prevalence;
    private List<AchillesAnalysis> analyses = new ArrayList<>();

    public static final long SURVEY_COUNT_ANALYSIS_ID = 3100;
    public static final long SURVEY_GENDER_ANALYSIS_ID = 3111;
    public static final long SURVEY_AGE_ANALYSIS_ID = 3112;

    public static Map<String, String> ageStratumNameMap  = new HashMap<String, String>();
    public static Map<String, String> genderStratumNameMap = new HashMap<String, String>();

    /* Take analysis list with results and put them on the list of questions.
     * This is used when we get a whole list of Analysis with the results for a list of questions from tha dao
     * so that the return to the ui is better organized
     */
    public static void addAnalysesToQuestions(List<QuestionConcept> questions, List<AchillesAnalysis> analyses ) {
        setAgeStratumNameMap();
        setGenderStratumNameMap();
        for (AchillesAnalysis a : analyses) {
            if (a.getResults().size() > 0) {
                AchillesResult r = a.getResults().get(0);
                long qid = Long.valueOf(r.getStratum2());

                // Put analysis on right question
                for (QuestionConcept q : questions) {
                    if (q.getConceptId() == qid) {
                        // :( Results here have all the results for every question ... Wahhhhh! :(
                        System.out.println("Adding analysis to qid " + qid + " concept id " +
                                q.getConceptId() + " results size " + a.getResults().size());
                        q.addAnalysis(a);
                        break;
                    }
                }
            }
            // Add stratum5Name to the results for the ui -- ie Male, Female , Age Decile name
            for (AchillesResult r : a.getResults()) {
                if (a.getAnalysisId() == SURVEY_AGE_ANALYSIS_ID) {
                    r.setStratum5Name(ageStratumNameMap.get(r.getStratum5()));
                }
               if (a.getAnalysisId() == SURVEY_GENDER_ANALYSIS_ID) {
                    r.setStratum5Name(genderStratumNameMap.get(r.getStratum5()));
                }
            }
        }
    }

    /* Todo Find right place for these static things to be generated from db if possible and live */
    public static void setAgeStratumNameMap() {
        ageStratumNameMap.put("1", "0-18 yrs old");
        ageStratumNameMap.put("2", "18-29 yrs old");
        ageStratumNameMap.put("3", "30-39 yrs old");
        ageStratumNameMap.put("4", "40-49 yrs old");
        ageStratumNameMap.put("5", "50-59 yrs old");
        ageStratumNameMap.put("6", "60-69 yrs old");
        ageStratumNameMap.put("7", "70-79 yrs old");
        ageStratumNameMap.put("8", "80-89 yrs old");
        ageStratumNameMap.put("9", "90-99 yrs old");
        ageStratumNameMap.put("10", "100-109 yrs old");
        ageStratumNameMap.put("11", "110-119 yrs old");
        ageStratumNameMap.put("12", "120-129 yrs old");
        ageStratumNameMap.put("13", "130-139 yrs old");
    }
    public static void setGenderStratumNameMap() {
        /* This is to slow to use the db */
        genderStratumNameMap.put("8507", "Male");
        genderStratumNameMap.put("8532", "Female");
        genderStratumNameMap.put("8521", "Other");
        genderStratumNameMap.put("8551", "Unknown");
        genderStratumNameMap.put("8570", "Ambiguous");
    }

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

    public void addAnalysis(AchillesAnalysis analysis) {
        this.analyses.add(analysis);
    }


}
