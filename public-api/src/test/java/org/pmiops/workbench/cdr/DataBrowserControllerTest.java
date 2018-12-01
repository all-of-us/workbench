package org.pmiops.workbench.publicapi;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDistDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptRelationshipDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.Analysis;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptAnalysis;
import org.pmiops.workbench.model.ConceptAnalysisListResponse;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.DomainInfosAndSurveyModulesResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.SurveyModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DataBrowserControllerTest {

    private static final Concept CLIENT_CONCEPT_1 = new Concept()
            .conceptId(123L)
            .conceptName("a concept")
            .standardConcept("S")
            .conceptCode("001")
            .conceptClassId("classId")
            .vocabularyId("V1")
            .domainId("Condition")
            .countValue(123L)
            .sourceCountValue(20L)
            .prevalence(0.2F)
            .conceptSynonyms(new ArrayList<String>());

    private static final Concept CLIENT_CONCEPT_2 = new Concept()
            .conceptId(456L)
            .conceptName("b concept")
            .conceptCode("002")
            .conceptClassId("classId2")
            .vocabularyId("V2")
            .domainId("Measurement")
            .countValue(456L)
            .sourceCountValue(25L)
            .prevalence(0.3F)
            .conceptSynonyms(new ArrayList<String>());

    private static final Concept CLIENT_CONCEPT_3 = new Concept()
            .conceptId(789L)
            .conceptName("multi word concept")
            .standardConcept("")
            .conceptCode("003")
            .conceptClassId("classId3")
            .vocabularyId("V3")
            .domainId("Condition")
            .countValue(789L)
            .sourceCountValue(0L)
            .prevalence(0.4F)
            .conceptSynonyms(new ArrayList<String>());

    private static final Concept CLIENT_CONCEPT_4 = new Concept()
            .conceptId(1234L)
            .conceptName("sample test con to test the multi word search")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId4")
            .vocabularyId("V4")
            .domainId("Observation")
            .countValue(1250L)
            .sourceCountValue(99L)
            .prevalence(0.5F)
            .conceptSynonyms(new ArrayList<String>());

    private static final Concept CLIENT_CONCEPT_5 = new Concept()
            .conceptId(7890L)
            .conceptName("conceptD test concept")
            .standardConcept("S")
            .conceptCode("005")
            .conceptClassId("classId5")
            .vocabularyId("V5")
            .domainId("Condition")
            .countValue(7890L)
            .sourceCountValue(78L)
            .prevalence(0.9F)
            .conceptSynonyms(new ArrayList<String>());

    private static final Concept CLIENT_CONCEPT_6 = new Concept()
            .conceptId(7891L)
            .conceptName("conceptD test concept 2")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId6")
            .vocabularyId("V6")
            .domainId("Condition")
            .countValue(0L)
            .sourceCountValue(20L)
            .prevalence(0.1F)
            .conceptSynonyms(ImmutableList.of("cstest 1", "cstest 2", "cstest 3"));

  private static final Concept CLIENT_CONCEPT_7 = new Concept()
            .conceptId(7892L)
            .conceptName("conceptD test concept 3")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId7")
            .vocabularyId("V7")
            .domainId("Condition")
            .countValue(0L)
            .sourceCountValue(0L)
            .prevalence(0.0F)
            .conceptSynonyms(ImmutableList.of("cstest 1", "cstest 2", "cstest 3"));

    private static final DomainInfo CLIENT_DOMAIN_1 = new DomainInfo()
            .domain(Domain.CONDITION)
            .name("Diagnoses")
            .description("Condition Domain")
            .allConceptCount(123L)
            .standardConceptCount(456L)
            .participantCount(789L);

    private static final DomainInfo CLIENT_DOMAIN_2 = new DomainInfo()
            .domain(Domain.DRUG)
            .name("Medications")
            .description("Drug Domain")
            .allConceptCount(1L)
            .standardConceptCount(2L)
            .participantCount(3L);

    private static final SurveyModule CLIENT_SURVEY_MODULE_1 = new SurveyModule()
            .name("Lifestyle")
            .description("The Lifestyle module provides information on smoking, alcohol and recreational drug use")
            .conceptId(1585855L)
            .questionCount(568120L)
            .participantCount(4L);

    private static final SurveyModule CLIENT_SURVEY_MODULE_2 = new SurveyModule()
            .name("The Basics")
            .description("The Basics module provides demographics and economic information for participants")
            .conceptId(1586134L)
            .questionCount(567437L)
            .participantCount(5L);

    private static final AchillesAnalysis CLIENT_ANALYSIS_1 = new AchillesAnalysis()
            .analysisId(1900L)
            .analysisName("Measurement Response Gender Distribution")
            .stratum1Name("Measurement Concept Id")
            .stratum2Name("Gender Concept Id")
            .stratum4Name("value")
            .stratum5Name("unit source value")
            .chartType("column")
            .dataType("counts");


    private static final AchillesAnalysis CLIENT_ANALYSIS_2 = new AchillesAnalysis()
            .analysisId(1901L)
            .analysisName("Measurement Response Age Distribution")
            .stratum1Name("Measurement Concept Id")
            .stratum2Name("Age Decile")
            .stratum4Name("value")
            .stratum5Name("unit source value")
            .chartType("column")
            .dataType("counts");

    private static final AchillesAnalysis CLIENT_ANALYSIS_3 = new AchillesAnalysis()
            .analysisId(1911L)
            .analysisName("Measurement Response Male Distribution")
            .stratum1Name("Measurement Concept Id")
            .stratum2Name("Gender Concept Id")
            .stratum4Name("value")
            .stratum5Name("unit source value")
            .chartType("column")
            .dataType("counts");


    private static final AchillesAnalysis CLIENT_ANALYSIS_4 = new AchillesAnalysis()
            .analysisId(1912L)
            .analysisName("Measurement Response Female Distribution")
            .stratum1Name("Measurement Concept Id")
            .stratum2Name("Gender Concept Id")
            .stratum4Name("value")
            .stratum5Name("unit source value")
            .chartType("column")
            .dataType("counts");

    private static final AchillesAnalysis CLIENT_ANALYSIS_5 = new AchillesAnalysis()
            .analysisId(3101L)
            .analysisName("Gender Distribution")
            .stratum1Name("Concept Id")
            .stratum2Name("Gender Concept Id")
            .stratum3Name("DomainId")
            .chartType("pie")
            .dataType("counts");
    private static final AchillesAnalysis CLIENT_ANALYSIS_6 = new AchillesAnalysis()
            .analysisId(3102L)
            .analysisName("Age Distribution")
            .stratum1Name("Concept Id")
            .stratum2Name("Age decile")
            .stratum3Name("DomainId")
            .chartType("column")
            .dataType("counts");

    private static final AchillesAnalysis ACHILLES_ANALYSIS_1 = makeAchillesAnalysis(CLIENT_ANALYSIS_1);
    private static final AchillesAnalysis ACHILLES_ANALYSIS_2 = makeAchillesAnalysis(CLIENT_ANALYSIS_2);
    private static final AchillesAnalysis ACHILLES_ANALYSIS_3 = makeAchillesAnalysis(CLIENT_ANALYSIS_3);
    private static final AchillesAnalysis ACHILLES_ANALYSIS_4 = makeAchillesAnalysis(CLIENT_ANALYSIS_4);
    private static final AchillesAnalysis ACHILLES_ANALYSIS_5 = makeAchillesAnalysis(CLIENT_ANALYSIS_5);
    private static final AchillesAnalysis ACHILLES_ANALYSIS_6 = makeAchillesAnalysis(CLIENT_ANALYSIS_6);

    private static final AchillesResult CLIENT_RESULT_1 = new AchillesResult()
            .id(1L)
            .analysisId(1900L)
            .stratum1("137989")
            .stratum2("8532")
            .stratum4("Abnormal results of cardiovascular function studies")
            .countValue(12L)
            .sourceCountValue(34L);


    private static final AchillesResult CLIENT_RESULT_2 = new AchillesResult()
            .id(2L)
            .analysisId(1900L)
            .stratum1("137989")
            .stratum2("8507")
            .stratum4("Abnormal results of cardiovascular function studies")
            .countValue(22L)
            .sourceCountValue(34L);


    private static final AchillesResult CLIENT_RESULT_3 = new AchillesResult()
            .id(3L)
            .analysisId(1901L)
            .stratum1("137989")
            .stratum2("2")
            .stratum4("Abnormal results of cardiovascular function studies")
            .countValue(2L)
            .sourceCountValue(34L);


    private static final AchillesResult CLIENT_RESULT_4 = new AchillesResult()
            .id(4L)
            .analysisId(1901L)
            .stratum1("137989")
            .stratum2("4")
            .stratum4("Abnormal results of cardiovascular function studies")
            .countValue(2L)
            .sourceCountValue(34L);


    private static final AchillesResult CLIENT_RESULT_5 = new AchillesResult()
            .id(5L)
            .analysisId(1901L)
            .stratum1("137989")
            .stratum2("3")
            .stratum4("Abnormal results of cardiovascular function studies")
            .countValue(1L)
            .sourceCountValue(34L);


    private static final AchillesResult CLIENT_RESULT_6 = new AchillesResult()
            .id(6L)
            .analysisId(3101L)
            .stratum1("1586134")
            .stratum2("8507")
            .stratum3("Survey")
            .countValue(251780L)
            .sourceCountValue(251780L);

    private static final AchillesResult CLIENT_RESULT_7 = new AchillesResult()
            .id(7L)
            .analysisId(3101L)
            .stratum1("1586134")
            .stratum2("8532")
            .stratum3("Survey")
            .countValue(316080L)
            .sourceCountValue(316080L);

    private static final AchillesResult CLIENT_RESULT_8 = new AchillesResult()
            .id(8L)
            .analysisId(3102L)
            .stratum1("1586134")
            .stratum2("2")
            .stratum3("Survey")
            .countValue(93020L)
            .sourceCountValue(93020L);

    private static final AchillesResult CLIENT_RESULT_9 = new AchillesResult()
            .id(9L)
            .analysisId(3102L)
            .stratum1("1586134")
            .stratum2("3")
            .stratum3("Survey")
            .countValue(93480L)
            .sourceCountValue(93480L);


    private static final AchillesResult ACHILLES_RESULT_1 = makeAchillesResult(CLIENT_RESULT_1);
    private static final AchillesResult ACHILLES_RESULT_2 = makeAchillesResult(CLIENT_RESULT_2);
    private static final AchillesResult ACHILLES_RESULT_3 = makeAchillesResult(CLIENT_RESULT_3);
    private static final AchillesResult ACHILLES_RESULT_4 = makeAchillesResult(CLIENT_RESULT_4);
    private static final AchillesResult ACHILLES_RESULT_5 = makeAchillesResult(CLIENT_RESULT_5);
    private static final AchillesResult ACHILLES_RESULT_6 = makeAchillesResult(CLIENT_RESULT_6);
    private static final AchillesResult ACHILLES_RESULT_7 = makeAchillesResult(CLIENT_RESULT_7);
    private static final AchillesResult ACHILLES_RESULT_8 = makeAchillesResult(CLIENT_RESULT_8);
    private static final AchillesResult ACHILLES_RESULT_9 = makeAchillesResult(CLIENT_RESULT_9);

    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
            makeConcept(CLIENT_CONCEPT_1);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
            makeConcept(CLIENT_CONCEPT_2);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_3 =
            makeConcept(CLIENT_CONCEPT_3);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_4 =
            makeConcept(CLIENT_CONCEPT_4);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_5 =
            makeConcept(CLIENT_CONCEPT_5);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_6 =
            makeConcept(CLIENT_CONCEPT_6);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_7 =
            makeConcept(CLIENT_CONCEPT_7);

    private static final org.pmiops.workbench.cdr.model.DomainInfo DOMAIN_1 =
            makeDomain(CLIENT_DOMAIN_1, 1L);
    private static final org.pmiops.workbench.cdr.model.DomainInfo DOMAIN_2 =
            makeDomain(CLIENT_DOMAIN_2, 2L);
    private static final org.pmiops.workbench.cdr.model.SurveyModule SURVEY_MODULE_1 =
            makeSurveyModule(CLIENT_SURVEY_MODULE_1);
    private static final org.pmiops.workbench.cdr.model.SurveyModule SURVEY_MODULE_2 =
            makeSurveyModule(CLIENT_SURVEY_MODULE_2);

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private CdrVersionDao cdrVersionDao;
    @Autowired
    ConceptRelationshipDao conceptRelationshipDao;
    @Autowired
    private DomainInfoDao domainInfoDao;
    @Autowired
    private SurveyModuleDao surveyModuleDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private AchillesResultDistDao achillesResultDistDao;
    @PersistenceContext
    private EntityManager entityManager;


    private CdrVersion cdrVersion;
    private DataBrowserController dataBrowserController;

    @Before
    public void setUp() {
        saveData();
        ConceptService conceptService = new ConceptService(entityManager, conceptDao);
        dataBrowserController = new DataBrowserController(conceptService, conceptDao, domainInfoDao,
            surveyModuleDao, achillesResultDao, achillesAnalysisDao, achillesResultDistDao, entityManager,
            () -> cdrVersion);
    }


    @Test
    public void testGetSourceConcepts() throws Exception {
        ResponseEntity<ConceptListResponse> response = dataBrowserController.getSourceConcepts(7890L, 15);
        assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_4, CLIENT_CONCEPT_2);
    }


    @Test
    public void testGetDomainTotals() throws Exception {
        ResponseEntity<DomainInfosAndSurveyModulesResponse> response = dataBrowserController.getDomainTotals();
        assertThat(response.getBody().getDomainInfos()).containsExactly(CLIENT_DOMAIN_1, CLIENT_DOMAIN_2);
        assertThat(response.getBody().getSurveyModules()).containsExactly(CLIENT_SURVEY_MODULE_1, CLIENT_SURVEY_MODULE_2);
    }

    @Test
    public void testAllConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION)
                .minCount(0));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_5,
          CLIENT_CONCEPT_6, CLIENT_CONCEPT_7);
    }

    @Test
    public void testConceptSearchMaxResults() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION)
                .maxResults(1)
                .minCount(0));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_5);
    }

    @Test
    public void testCountConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION));
        assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_5, CLIENT_CONCEPT_6);
    }

    @Test
    public void testConceptSearchStandardConcept() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("002")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        assertThat(response.getBody().getItems()).isEmpty();
    }

    @Test
    public void testConceptSynonymSearch() throws Exception{
      ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
          .domain(Domain.CONDITION).query("cstest").standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
      // CLIENT_CONCEPT_7 excluded because it has a zero count
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_6);
    }

    @Test
    public void testConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
        assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4,
            CLIENT_CONCEPT_5, CLIENT_CONCEPT_6);
    }

    @Test
    public void testNonStandardEmptyQuerySearch() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .query("")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_2, CLIENT_CONCEPT_3);
    }

    @Test
    public void testConceptSearchNonStandardConcepts() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("7891")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
      assertThat(response.getBody().getItems()).isEmpty();
    }


    @Test
    public void testConceptSearchEmptyCount() throws Exception{
        // We can't test limiting to count > 0 with a concept name search because the match function does not work in hibernate. So we make several concepts with same concept code and one with count 0. The limit > 0 works the same weather it is code or name match.
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_4, CLIENT_CONCEPT_6);
    }

    @Test
    public void testConceptIdSearch() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("456")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_2);
    }

    @Test
    public void testConceptSearchDomainFilter() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004").domain(Domain.CONDITION));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_6);
    }


    @Test
    public void testConceptCodeMatch() throws Exception {
      ResponseEntity<ConceptListResponse> response = dataBrowserController
          .searchConcepts(new SearchConceptsRequest().query("002")
              .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
      assertThat(response.getBody().getItems()).containsExactly(CLIENT_CONCEPT_2);
    }

    @Test
    public void testGetMeasurementAnalysisNoMatch() throws Exception{
        ArrayList<String> queryConceptIds = new ArrayList<String>();
        queryConceptIds.add("137990");
        ResponseEntity<ConceptAnalysisListResponse> response = dataBrowserController.getConceptAnalysisResults(queryConceptIds);
        List<ConceptAnalysis> conceptAnalysisList = response.getBody().getItems();
        assertThat(conceptAnalysisList.get(0).getAgeAnalysis()).isEqualTo(null);
    }

    @Test
    public void testGetSurveyDemographicAnalysesMatch() throws Exception{
        List<String> conceptsIds = new ArrayList<>();
        conceptsIds.add("1586134");
        ResponseEntity<ConceptAnalysisListResponse> response = dataBrowserController.getConceptAnalysisResults(conceptsIds);
        List<ConceptAnalysis> conceptAnalysis = response.getBody().getItems();
        Analysis ageAnalysis = conceptAnalysis.get(0).getAgeAnalysis();
        Analysis genderAnalysis = conceptAnalysis.get(0).getGenderAnalysis();
        assertThat(ageAnalysis).isNotEqualTo(null);
        assertThat(genderAnalysis).isNotEqualTo(null);
    }

    @Test
    public void testGetSurveyDemographicAnalysesMultipleMatch() throws Exception{
        List<String> conceptsIds = new ArrayList<>();
        conceptsIds.add("1586134");
        conceptsIds.add("1585855");
        ResponseEntity<ConceptAnalysisListResponse> response = dataBrowserController.getConceptAnalysisResults(conceptsIds);
        List<ConceptAnalysis> conceptAnalysis = response.getBody().getItems();
        assertThat(conceptAnalysis.get(0).getGenderAnalysis().getResults().size()).isEqualTo(2);
        assertThat(conceptAnalysis.get(1).getGenderAnalysis()).isEqualTo(null);
    }

    @Test
    public void testGetSurveyDemographicAnalysesNoMatch() throws Exception{
        List<String> conceptsIds = new ArrayList<>();
        conceptsIds.add("1585855");
        ResponseEntity<ConceptAnalysisListResponse> response = dataBrowserController.getConceptAnalysisResults(conceptsIds);
        List<ConceptAnalysis> conceptAnalysisList = response.getBody().getItems();
        assertThat(conceptAnalysisList.get(0).getAgeAnalysis()).isEqualTo(null);
    }



  static org.pmiops.workbench.cdr.model.Concept makeConcept(Concept concept) {
    org.pmiops.workbench.cdr.model.Concept result = new org.pmiops.workbench.cdr.model.Concept();
    result.setConceptId(concept.getConceptId());
    result.setConceptName(concept.getConceptName());
    result.setStandardConcept(concept.getStandardConcept());
    result.setConceptCode(concept.getConceptCode());
    result.setConceptClassId(concept.getConceptClassId());
    result.setVocabularyId(concept.getVocabularyId());
    result.setDomainId(concept.getDomainId());
    result.setCountValue(concept.getCountValue());
    result.setSourceCountValue(concept.getSourceCountValue());
    result.setPrevalence(concept.getPrevalence());
    result.setSynonymsStr(
        String.valueOf(concept.getConceptId()) + '|' +
            Joiner.on("|").join(concept.getConceptSynonyms()));
    return result;
  }

    private ConceptRelationship makeConceptRelationship(long conceptId1, long conceptId2, String relationshipId) {
        ConceptRelationshipId key = new ConceptRelationshipId();
        key.setConceptId1(conceptId1);
        key.setConceptId2(conceptId2);
        key.setRelationshipId(relationshipId);

        ConceptRelationship result = new ConceptRelationship();
        result.setConceptRelationshipId(key);
        return result;
    }

    private static org.pmiops.workbench.cdr.model.DomainInfo makeDomain(DomainInfo domain, long conceptId) {
        return new org.pmiops.workbench.cdr.model.DomainInfo()
            .domainEnum(domain.getDomain())
            .domainId(CommonStorageEnums.domainToDomainId(domain.getDomain()))
            .name(domain.getName())
            .description(domain.getDescription())
            .conceptId(conceptId)
            .allConceptCount(domain.getAllConceptCount())
            .standardConceptCount(domain.getStandardConceptCount())
            .participantCount(domain.getParticipantCount());
    }

    private static org.pmiops.workbench.cdr.model.SurveyModule makeSurveyModule(SurveyModule surveyModule) {
        return new org.pmiops.workbench.cdr.model.SurveyModule()
            .conceptId(surveyModule.getConceptId())
            .name(surveyModule.getName())
            .description(surveyModule.getDescription())
            .questionCount(surveyModule.getQuestionCount())
            .participantCount(surveyModule.getParticipantCount());
    }

    private static AchillesAnalysis makeAchillesAnalysis(AchillesAnalysis achillesAnalysis){
        AchillesAnalysis aa = new AchillesAnalysis();
        aa.setAnalysisId(achillesAnalysis.getAnalysisId());
        aa.setAnalysisName(achillesAnalysis.getAnalysisName());
        aa.setStratum1Name(achillesAnalysis.getStratum1Name());
        aa.setStratum2Name(achillesAnalysis.getStratum2Name());
        aa.setStratum4Name(achillesAnalysis.getStratum4Name());
        aa.setStratum5Name(achillesAnalysis.getStratum5Name());
        aa.setChartType(achillesAnalysis.getChartType());
        aa.setDataType(achillesAnalysis.getDataType());
        return aa;
    }

    private static AchillesResult makeAchillesResult(AchillesResult achillesResult){
        AchillesResult ar = new AchillesResult();
        ar.setId(achillesResult.getId());
        ar.setAnalysisId(achillesResult.getAnalysisId());
        ar.setStratum1(achillesResult.getStratum1());
        ar.setStratum2(achillesResult.getStratum2());
        ar.setStratum4(achillesResult.getStratum3());
        ar.setStratum5(achillesResult.getStratum5());
        ar.setCountValue(achillesResult.getCountValue());
        ar.setSourceCountValue(achillesResult.getSourceCountValue());
        return ar;
    }

    private void saveData() {
        makeCdrVersion(1L, "Test Registered CDR",
            123L, DataAccessLevel.REGISTERED);
        conceptDao.save(CONCEPT_1);
        conceptDao.save(CONCEPT_2);
        conceptDao.save(CONCEPT_3);
        conceptDao.save(CONCEPT_4);
        conceptDao.save(CONCEPT_5);
        conceptDao.save(CONCEPT_6);
        conceptDao.save(CONCEPT_7);

        conceptRelationshipDao.save(makeConceptRelationship(1234L, 7890L, "maps to"));
        conceptRelationshipDao.save(makeConceptRelationship(456L, 7890L, "maps to"));

        domainInfoDao.save(DOMAIN_1);
        domainInfoDao.save(DOMAIN_2);
        surveyModuleDao.save(SURVEY_MODULE_1);
        surveyModuleDao.save(SURVEY_MODULE_2);

        achillesAnalysisDao.save(ACHILLES_ANALYSIS_1);
        achillesAnalysisDao.save(ACHILLES_ANALYSIS_2);
        achillesAnalysisDao.save(ACHILLES_ANALYSIS_3);
        achillesAnalysisDao.save(ACHILLES_ANALYSIS_4);
        achillesAnalysisDao.save(ACHILLES_ANALYSIS_5);
        achillesAnalysisDao.save(ACHILLES_ANALYSIS_6);

        achillesResultDao.save(ACHILLES_RESULT_1);
        achillesResultDao.save(ACHILLES_RESULT_2);
        achillesResultDao.save(ACHILLES_RESULT_3);
        achillesResultDao.save(ACHILLES_RESULT_4);
        achillesResultDao.save(ACHILLES_RESULT_5);
        achillesResultDao.save(ACHILLES_RESULT_6);
        achillesResultDao.save(ACHILLES_RESULT_7);
        achillesResultDao.save(ACHILLES_RESULT_8);
        achillesResultDao.save(ACHILLES_RESULT_9);
    }

    private CdrVersion makeCdrVersion(long cdrVersionId, String name, long creationTime,
        DataAccessLevel dataAccessLevel) {
        cdrVersion = new CdrVersion();
        cdrVersion.setBigqueryDataset("a");
        cdrVersion.setBigqueryProject("b");
        cdrVersion.setCdrDbName("c");
        cdrVersion.setCdrVersionId(cdrVersionId);
        cdrVersion.setCreationTime(new Timestamp(creationTime));
        cdrVersion.setDataAccessLevelEnum(dataAccessLevel);
        cdrVersion.setName(name);
        cdrVersion.setNumParticipants(123);
        cdrVersion.setPublicDbName("p");
        cdrVersion.setReleaseNumber((short) 1);
        cdrVersionDao.save(cdrVersion);
        return cdrVersion;
    }

}
