package org.pmiops.workbench.cdr.model;


import javax.persistence.*;
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
    private AchillesAnalysis countAnalysis;
    private AchillesAnalysis genderAnalysis;
    private AchillesAnalysis ageAnalysis;
    private AchillesAnalysis genderIdentityAnalysis;

    public static final long SURVEY_COUNT_ANALYSIS_ID = 3110;
    public static final long SURVEY_GENDER_ANALYSIS_ID = 3111;
    public static final long SURVEY_AGE_ANALYSIS_ID = 3112;
    public static final long SURVEY_GENDER_IDENTITY_ANALYSIS_ID = 3113;

    public static Map<String, String> ageStratumNameMap  = new HashMap<String, String>();
    public static Map<String, String> genderStratumNameMap = new HashMap<String, String>();
    public static Map<String, String> genderIdentityStratumNameMap = new HashMap<>();
    public static Map<String, String> raceStratumNameMap = new HashMap<String, String>();
    public static Map<String, String> ethnicityStratumNameMap = new HashMap<String, String>();

    /* Todo Find right place for these static things to be generated from db if possible and live */
    public static void setAgeStratumNameMap() {
        ageStratumNameMap.put("1", "0-18");
        ageStratumNameMap.put("2", "18-29");
        ageStratumNameMap.put("3", "30-39");
        ageStratumNameMap.put("4", "40-49");
        ageStratumNameMap.put("5", "50-59");
        ageStratumNameMap.put("6", "60-69");
        ageStratumNameMap.put("7", "70-79");
        ageStratumNameMap.put("8", "80-89");
    }

    public static void setGenderStratumNameMap() {
        /* This is to slow to use the db */
        genderStratumNameMap.put("8507", "Male");
        genderStratumNameMap.put("8532", "Female");
        genderStratumNameMap.put("8521", "Other");
        genderStratumNameMap.put("8551", "Unknown");
        genderStratumNameMap.put("8570", "Ambiguous");
        genderStratumNameMap.put("1585849", "None of these describe me");
        genderStratumNameMap.put("1585848", "Intersex");
    }

    public static void setGenderIdentityStratumNameMap() {
        genderIdentityStratumNameMap.put("1585840", "Woman");
        genderIdentityStratumNameMap.put("903070", "Other");
        genderIdentityStratumNameMap.put("903096", "Skip");
        genderIdentityStratumNameMap.put("903079", "Prefer Not To Answer");
        genderIdentityStratumNameMap.put("1585841", "Non-binary");
        genderIdentityStratumNameMap.put("1585839", "Man");
        genderIdentityStratumNameMap.put("1585842", "Transgender");
        genderIdentityStratumNameMap.put("1585843", "None of these describe me");
    }

    public static void setRaceStratumNameMap(){
        raceStratumNameMap.put("8515", "Asian");
        raceStratumNameMap.put("8516", "Black or African American");
        raceStratumNameMap.put("8522", "Other Race");
        raceStratumNameMap.put("8527", "White");
        raceStratumNameMap.put("8552", "Unknown");
        raceStratumNameMap.put("8557", "Native Hawaiian or Other Pacific Islander");
        raceStratumNameMap.put("8657", "American Indian or Alaska Native");
        raceStratumNameMap.put("9178", "Non-white");
        raceStratumNameMap.put("38003572", "American Indian");
        raceStratumNameMap.put("38003573", "Alaska Native");
        raceStratumNameMap.put("38003574", "Asian Indian");
        raceStratumNameMap.put("38003575", "Bangladeshi");
        raceStratumNameMap.put("38003576", "Bhutanese");
        raceStratumNameMap.put("38003577", "Burmese");
        raceStratumNameMap.put("38003578", "Cambodian");
        raceStratumNameMap.put("38003579", "Chinese");
        raceStratumNameMap.put("38003580", "Taiwanese");
        raceStratumNameMap.put("38003581", "Filipino");
        raceStratumNameMap.put("38003582", "Hmong");
        raceStratumNameMap.put("38003583", "Indonesian");
        raceStratumNameMap.put("38003584", "Japanese");
        raceStratumNameMap.put("38003585", "Korean");
        raceStratumNameMap.put("38003586", "Laotian");
        raceStratumNameMap.put("38003587", "Malaysian");
        raceStratumNameMap.put("38003588", "Okinawan");
        raceStratumNameMap.put("38003589", "Pakistani");
        raceStratumNameMap.put("38003590", "Sri Lankan");
        raceStratumNameMap.put("38003591", "Thai");
        raceStratumNameMap.put("38003592", "Vietnamese");
        raceStratumNameMap.put("38003593", "Iwo Jiman");
        raceStratumNameMap.put("38003594", "Maldivian");
        raceStratumNameMap.put("38003595", "Nepalese");
        raceStratumNameMap.put("38003596", "Singaporean");
        raceStratumNameMap.put("38003597", "Madagascar");
        raceStratumNameMap.put("38003598", "Black");
        raceStratumNameMap.put("38003599", "African American");
        raceStratumNameMap.put("38003600", "African");
        raceStratumNameMap.put("38003601", "Bahamian");
        raceStratumNameMap.put("38003602", "Barbadian");
        raceStratumNameMap.put("38003603", "Dominican");
        raceStratumNameMap.put("38003604", "Dominica Islander");
        raceStratumNameMap.put("38003605", "Haitian");
        raceStratumNameMap.put("38003606", "Jamaican");
        raceStratumNameMap.put("38003607", "Tobagoan");
        raceStratumNameMap.put("38003608", "Trinidadian");
        raceStratumNameMap.put("38003609", "West Indian");
        raceStratumNameMap.put("38003610", "Polynesian");
        raceStratumNameMap.put("38003611", "Micronesian");
        raceStratumNameMap.put("38003612", "Melanesian");
        raceStratumNameMap.put("38003613", "Other Pacific Islander");
        raceStratumNameMap.put("38003614", "European");
        raceStratumNameMap.put("38003615", "Middle Eastern or North African");
        raceStratumNameMap.put("38003616", "Arab");
    }

    public static void setEthnicityStratumNameMap(){

        ethnicityStratumNameMap.put("38003564","Not Hispanic or Latino");
        ethnicityStratumNameMap.put("38003563", "Hispanic or Latino");

    }

    static {
        setAgeStratumNameMap();
        setGenderStratumNameMap();
        setGenderIdentityStratumNameMap();
        setRaceStratumNameMap();
        setEthnicityStratumNameMap();
    }

    /* Take analysis list with results and put them on the list of questions.
     * This is used when we get a whole list of Analysis with the results for a list of questions from tha dao
     * so that the return to the ui is a nice question object with analyses and results
     * Questions updated by reference
     */
    public static void mapAnalysesToQuestions(List<QuestionConcept> questions, List<AchillesAnalysis> analyses ) {
        Map<Long, QuestionConcept> questionMap = new HashMap<Long, QuestionConcept>();
        for (QuestionConcept q : questions) {
            questionMap.put(q.getConceptId(), q);
        }

        for (AchillesAnalysis analysis : analyses) {
            // Add stratum5Name to the results for the ui -- ie Male, Female , Age Decile name
            for (AchillesResult r : analysis.getResults()) {
                // Add analysis to question if need to
                if(r.getStratum4().contains("PMI")) {
                    r.setStratum4(r.getStratum4().replace("PMI",""));
                }
                Long qid = Long.valueOf(r.getStratum2());
                QuestionConcept q = questionMap.get(qid);

                if ( q.getAnalysis(analysis.getAnalysisId())  == null) {
                    q.setAnalysis(new AchillesAnalysis(analysis));
                }
                AchillesAnalysis questionAnalysis = q.getAnalysis(analysis.getAnalysisId());
                questionAnalysis.addResult(r);
                String rStratum5Name = r.getAnalysisStratumName();
                if (rStratum5Name == null || rStratum5Name.equals("")) {
                    if (analysis.getAnalysisId() == SURVEY_AGE_ANALYSIS_ID) {
                        r.setAnalysisStratumName(ageStratumNameMap.get(r.getStratum5()));
                    }
                    if (analysis.getAnalysisId() == SURVEY_GENDER_ANALYSIS_ID) {
                        r.setAnalysisStratumName(genderStratumNameMap.get(r.getStratum5()));
                    }
                    if (analysis.getAnalysisId() == SURVEY_GENDER_IDENTITY_ANALYSIS_ID) {
                        r.setAnalysisStratumName(genderIdentityStratumNameMap.get(r.getStratum5()));
                    }
                }
            }
        }
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
    public AchillesAnalysis getCountAnalysis() {
        return countAnalysis;
    }
    public void setCountAnalysis(AchillesAnalysis analysis) {
        this.countAnalysis = analysis;
    }
    public QuestionConcept countAnalysis(AchillesAnalysis analysis){
        this.countAnalysis = analysis;
        return this;
    }

    @Transient
    public AchillesAnalysis getGenderAnalysis() {
        return this.genderAnalysis;
    }
    public void setGenderAnalysis(AchillesAnalysis analysis) {
        this.genderAnalysis = analysis;
    }
    public QuestionConcept genderAnalysis(AchillesAnalysis analysis){
        this.genderAnalysis = analysis;
        return this;
    }

    @Transient
    public AchillesAnalysis getAgeAnalysis() {
        return this.ageAnalysis;
    }
    public void setAgeAnalysis(AchillesAnalysis analysis) {
        this.ageAnalysis = analysis;
    }
    public QuestionConcept ageAnalysis(AchillesAnalysis analysis){
        this.ageAnalysis = analysis;
        return this;
    }

    @Transient
    public AchillesAnalysis getGenderIdentityAnalysis() { return this.genderIdentityAnalysis; }
    public void setGenderIdentityAnalysis(AchillesAnalysis analysis) { this.genderIdentityAnalysis = analysis; }
    public QuestionConcept genderIdentityAnalysis(AchillesAnalysis analysis) {
        this.genderIdentityAnalysis = analysis;
        return this;
    }

    public void setAnalysis(AchillesAnalysis analysis) {
        if (analysis.getAnalysisId() == SURVEY_COUNT_ANALYSIS_ID) {
            this.countAnalysis = analysis;
        }
        else if (analysis.getAnalysisId() == SURVEY_GENDER_ANALYSIS_ID) {
            this.genderAnalysis = analysis;
        }
        else if (analysis.getAnalysisId() == SURVEY_AGE_ANALYSIS_ID) {
            this.ageAnalysis = analysis;
        }
        else if(analysis.getAnalysisId() == SURVEY_GENDER_IDENTITY_ANALYSIS_ID) {
            this.genderIdentityAnalysis = analysis;
        }
    }

    public AchillesAnalysis getAnalysis(Long analysisId) {
        if (analysisId == SURVEY_COUNT_ANALYSIS_ID) {
            return this.countAnalysis;
        }
        else if (analysisId == SURVEY_GENDER_ANALYSIS_ID) {
            return this.genderAnalysis;
        }
        else if (analysisId == SURVEY_AGE_ANALYSIS_ID) {
            return this.ageAnalysis;
        }
        else if (analysisId == SURVEY_GENDER_IDENTITY_ANALYSIS_ID) {
            return this.genderIdentityAnalysis;
        }
        return null;
    }

}
