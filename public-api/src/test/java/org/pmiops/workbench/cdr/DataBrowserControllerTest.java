package org.pmiops.workbench.publicapi;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.pmiops.workbench.cdr.dao.ConceptSynonymDao;
import org.pmiops.workbench.cdr.dao.DbDomainDao;
import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.model.Analysis;
import org.pmiops.workbench.model.ConceptAnalysis;
import org.pmiops.workbench.model.ConceptAnalysisListResponse;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DbDomainListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
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

    private static final Function<org.pmiops.workbench.model.Concept, Concept>
            TO_CLIENT_CONCEPT =
            new Function<org.pmiops.workbench.model.Concept, Concept>() {
                @Override
                public Concept apply(org.pmiops.workbench.model.Concept concept) {
                    return new Concept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .standardConcept(concept.getStandardConcept())
                            .conceptCode(concept.getConceptCode())
                            .conceptClassId(concept.getConceptClassId())
                            .vocabularyId(concept.getVocabularyId())
                            .domainId(concept.getDomainId())
                            .count(concept.getCountValue())
                            .sourceCountValue(concept.getSourceCountValue())
                            .prevalence(concept.getPrevalence())
                            .synonyms(new ArrayList<>());
                }
            };

    private static final Function<org.pmiops.workbench.model.DbDomain, DbDomain>
            TO_CLIENT_DBDOMAIN =
            new Function<org.pmiops.workbench.model.DbDomain, DbDomain>() {
                @Override
                public DbDomain apply(org.pmiops.workbench.model.DbDomain dbDomain) {
                    return new DbDomain()
                            .domainId(dbDomain.getDomainId())
                            .domainDisplay(dbDomain.getDomainDisplay())
                            .domainDesc(dbDomain.getDomainDesc())
                            .dbType(dbDomain.getDbType())
                            .domainRoute(dbDomain.getDomainRoute())
                            .conceptId(dbDomain.getConceptId())
                            .standardConceptCount(dbDomain.getCountValue())
                            .participantCount(dbDomain.getParticipantCount());
                }
            };

    private static final Concept CLIENT_CONCEPT_1 = new Concept()
            .conceptId(123L)
            .conceptName("a concept")
            .standardConcept("S")
            .conceptCode("001")
            .conceptClassId("classId")
            .vocabularyId("V1")
            .domainId("Condition")
            .count(123L)
            .sourceCountValue(20L)
            .prevalence(0.2F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_2 = new Concept()
            .conceptId(456L)
            .conceptName("b concept")
            .conceptCode("002")
            .conceptClassId("classId2")
            .vocabularyId("V2")
            .domainId("Measurement")
            .count(456L)
            .sourceCountValue(25L)
            .prevalence(0.3F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_3 = new Concept()
            .conceptId(789L)
            .conceptName("multi word concept")
            .standardConcept("")
            .conceptCode("003")
            .conceptClassId("classId3")
            .vocabularyId("V3")
            .domainId("Condition")
            .count(789L)
            .sourceCountValue(0L)
            .prevalence(0.4F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_4 = new Concept()
            .conceptId(1234L)
            .conceptName("sample test con to test the multi word search")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId4")
            .vocabularyId("V4")
            .domainId("Observation")
            .count(1250L)
            .sourceCountValue(99L)
            .prevalence(0.5F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_5 = new Concept()
            .conceptId(7890L)
            .conceptName("conceptD test concept")
            .standardConcept("S")
            .conceptCode("005")
            .conceptClassId("classId5")
            .vocabularyId("V5")
            .domainId("Condition")
            .count(7890L)
            .sourceCountValue(78L)
            .prevalence(0.9F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_6 = new Concept()
            .conceptId(7891L)
            .conceptName("conceptD test concept 2")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId6")
            .vocabularyId("V6")
            .domainId("Condition")
            .count(0L)
            .sourceCountValue(20L)
            .prevalence(0.1F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final Concept CLIENT_CONCEPT_7 = new Concept()
            .conceptId(7892L)
            .conceptName("conceptD test concept 3")
            .standardConcept("S")
            .conceptCode("004")
            .conceptClassId("classId7")
            .vocabularyId("V7")
            .domainId("Condition")
            .count(0L)
            .sourceCountValue(0L)
            .prevalence(0.0F)
            .synonyms(new ArrayList<ConceptSynonym>());

    private static final ConceptSynonym CLIENT_CONCEPT_SYNONYM_1 = new ConceptSynonym()
            .conceptId(7892L)
            .conceptSynonymName("cstest 1")
            .languageConceptId(0L);

    private static final ConceptSynonym CLIENT_CONCEPT_SYNONYM_2 = new ConceptSynonym()
            .conceptId(7892L)
            .conceptSynonymName("cstest 2")
            .languageConceptId(0L);

    private static final ConceptSynonym CLIENT_CONCEPT_SYNONYM_3 = new ConceptSynonym()
            .conceptId(7892L)
            .conceptSynonymName("cstest 3")
            .languageConceptId(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_1 = new DbDomain()
            .domainId("Condition")
            .domainDisplay("Diagnoses")
            .domainDesc("Condition Domain")
            .dbType("domain_filter")
            .domainRoute("condition")
            .conceptId(19L)
            .standardConceptCount(0L)
            .participantCount(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_2 = new DbDomain()
            .domainId("Drug")
            .domainDisplay("Medications")
            .domainDesc("Drug Domain")
            .dbType("domain_filter")
            .domainRoute("drug")
            .conceptId(13L)
            .standardConceptCount(0L)
            .participantCount(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_3 = new DbDomain()
            .domainId("Lifestyle")
            .domainDisplay("Lifestyle")
            .domainDesc("he Lifestyle module provides information on smoking, alcohol and recreational drug use")
            .dbType("survey")
            .domainRoute("ppi")
            .conceptId(1585855L)
            .standardConceptCount(568120L)
            .participantCount(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_4 = new DbDomain()
            .domainId("TheBasics")
            .domainDisplay("The Basics")
            .domainDesc("The Basics module provides demographics and economic information for participants")
            .dbType("survey")
            .domainRoute("ppi")
            .conceptId(1586134L)
            .standardConceptCount(567437L)
            .participantCount(0L);

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

    private static final Concept CONCEPT_1 =
            makeConcept(CLIENT_CONCEPT_1);
    private static final Concept CONCEPT_2 =
            makeConcept(CLIENT_CONCEPT_2);
    private static final Concept CONCEPT_3 =
            makeConcept(CLIENT_CONCEPT_3);
    private static final Concept CONCEPT_4 =
            makeConcept(CLIENT_CONCEPT_4);
    private static final Concept CONCEPT_5 =
            makeConcept(CLIENT_CONCEPT_5);
    private static final Concept CONCEPT_6 =
            makeConcept(CLIENT_CONCEPT_6);
    private static final Concept CONCEPT_7 =
            makeConcept(CLIENT_CONCEPT_7);

    private static final ConceptSynonym CONCEPT_SYNONYM_1 = makeConceptSynonym(CLIENT_CONCEPT_SYNONYM_1);
    private static final ConceptSynonym CONCEPT_SYNONYM_2 = makeConceptSynonym(CLIENT_CONCEPT_SYNONYM_2);
    private static final ConceptSynonym CONCEPT_SYNONYM_3 = makeConceptSynonym(CLIENT_CONCEPT_SYNONYM_3);

    private static final DbDomain DBDOMAIN_1 =
            makeDbDomain(CLIENT_DB_DOMAIN_1);
    private static final DbDomain DBDOMAIN_2 =
            makeDbDomain(CLIENT_DB_DOMAIN_2);
    private static final DbDomain DBDOMAIN_3 =
            makeDbDomain(CLIENT_DB_DOMAIN_3);
    private static final DbDomain DBDOMAIN_4 =
            makeDbDomain(CLIENT_DB_DOMAIN_4);

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private CdrVersionDao cdrVersionDao;
    @Autowired
    ConceptRelationshipDao conceptRelationshipDao;
    @Autowired
    private DbDomainDao dbDomainDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private ConceptSynonymDao conceptSynonymDao;
    @Autowired
    private AchillesResultDistDao achillesResultDistDao;
    @PersistenceContext
    private EntityManager entityManager;


    private CdrVersion cdrVersion;
    private DataBrowserController dataBrowserController;

    @Before
    public void setUp() {
        saveData();
        ConceptService conceptService = new ConceptService(entityManager, conceptSynonymDao);
        dataBrowserController = new DataBrowserController(conceptService, conceptDao, dbDomainDao,
            achillesResultDao, achillesAnalysisDao, achillesResultDistDao, entityManager,
            () -> cdrVersion);
    }


    @Test
    public void testGetParentConcepts() throws Exception {
        ResponseEntity<ConceptListResponse> response = dataBrowserController.getParentConcepts(1234L);
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_5)
        ;
    }


    @Test
    public void testGetSourceConcepts() throws Exception {
        ResponseEntity<ConceptListResponse> response = dataBrowserController.getSourceConcepts(7890L, 15);
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_4, CONCEPT_2);
    }


    @Test
    public void testGetDomainFilters() throws Exception {
        ResponseEntity<DbDomainListResponse> response = dataBrowserController.getDomainFilters();
        List<DbDomain> domains = response.getBody().getItems().stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList());
        assertThat(domains)
                .containsExactly(DBDOMAIN_1, DBDOMAIN_2);
    }

    @Test
    public void testGetSurveyList() throws Exception{
        ResponseEntity<DbDomainListResponse> response = dataBrowserController.getSurveyList();
        List<DbDomain> domains = response.getBody().getItems().stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList());
        assertThat(domains).containsExactly(DBDOMAIN_3, DBDOMAIN_4);
    }

    @Test
    public void testGetDbDomains() throws Exception{
        ResponseEntity<DbDomainListResponse> response = dataBrowserController.getDbDomains();
        List<DbDomain> domains = response.getBody().getItems().stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList());
        assertThat(domains)
                .containsExactly(DBDOMAIN_1, DBDOMAIN_2, DBDOMAIN_3, DBDOMAIN_4);
    }

    @Test
    public void testAllConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION)
                .minCount(0));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_1, CONCEPT_5, CONCEPT_6, CONCEPT_7)
        ;
    }

    @Test
    public void testConceptSearchMaxResults() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION)
                .maxResults(1)
                .minCount(0));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts.size()).isEqualTo(1);
    }

    @Test
    public void testCountConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .domain(Domain.CONDITION));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_1, CONCEPT_5, CONCEPT_6)
        ;
    }

    @Test
    public void testConceptSearchStandardConcept() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("002")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .doesNotContain(CONCEPT_2);
    }

    @Test
    public void testConceptSynonymSearch() throws Exception{
        assertResults(
                dataBrowserController.searchConcepts(new SearchConceptsRequest().domain(Domain.CONDITION).query("cstest")
                        .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),CLIENT_CONCEPT_7);
    }

    @Test
    public void testConceptSearchStandardCodeIdMatchFilter() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("")
                .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        List<Concept> standard_concepts = Arrays.asList(CONCEPT_1, CONCEPT_4, CONCEPT_5, CONCEPT_6, CONCEPT_7);
        assertThat(concepts)
                .doesNotContain(standard_concepts);
    }

    @Test
    public void testConceptSearchEmptyQuery() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        List<Concept> non_standard_concepts = Arrays.asList(CONCEPT_2, CONCEPT_3);
        assertThat(concepts)
                .doesNotContain(non_standard_concepts);
    }

    @Test
    public void testNonStandardEmptyQuerySearch() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest()
                .query("")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_2, CONCEPT_3);
    }

    @Test
    public void testConceptSearchNonStandardConcepts() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("7891")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        //Search on concept id fetches the non standard concept
        assertThat(concepts)
                .doesNotContain(CONCEPT_6);
    }


    @Test
    public void testConceptSearchEmptyCount() throws Exception{
        // We can't test limiting to count > 0 with a concept name search because the match function does not work in hibernate. So we make several concepts with same concept code and one with count 0. The limit > 0 works the same weather it is code or name match.
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_4, CONCEPT_6)
                .inOrder();
    }

    @Test
    public void testConceptIdSearch() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("456")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_2)
                .inOrder();
    }

    @Test
    public void testConceptSearchDomainFilter() throws Exception{
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004").domain(Domain.CONDITION));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_6)
                .inOrder();
    }


    @Test
    public void testConceptCodeMatch() throws Exception {
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("002")
                .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        List<org.pmiops.workbench.model.Concept> stds= response.getBody().getStandardConcepts();
        if(stds.size() > 0){
            List<Concept> std_concepts = response.getBody().getStandardConcepts().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
            assertThat(std_concepts)
                    .containsExactly(CONCEPT_5)
                    .inOrder();
        }

        assertThat(concepts)
                .containsExactly(CONCEPT_2)
                .inOrder();
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



    private static Concept makeConcept(Concept concept) {
        Concept result = new Concept();
        result.setConceptId(concept.getConceptId());
        result.setConceptName(concept.getConceptName());
        result.setStandardConcept(concept.getStandardConcept() == null ? null :
                (concept.getStandardConcept()));
        result.setConceptCode(concept.getConceptCode());
        result.setConceptClassId(concept.getConceptClassId());
        result.setVocabularyId(concept.getVocabularyId());
        result.setDomainId(concept.getDomainId());
        result.setCountValue(concept.getCountValue());
        result.setSourceCountValue(concept.getSourceCountValue());
        result.setPrevalence(concept.getPrevalence());
        result.setSynonyms(concept.getSynonyms());
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

    private static DbDomain makeDbDomain(DbDomain dbDomain){
        DbDomain dbd = new DbDomain();
        dbd.setDomainId(dbDomain.getDomainId());
        dbd.setDomainDisplay(dbDomain.getDomainDisplay());
        dbd.setDomainDesc(dbDomain.getDomainDesc());
        dbd.setDbType(dbDomain.getDbType());
        dbd.setDomainRoute(dbDomain.getDomainRoute());
        dbd.setConceptId(dbDomain.getConceptId());
        dbd.setStandardConceptCount(dbDomain.getStandardConceptCount());
        return dbd;
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

    private static ConceptSynonym makeConceptSynonym(ConceptSynonym conceptSynonym){
        ConceptSynonym cs = new ConceptSynonym();
        cs.setConceptId(conceptSynonym.getConceptId());
        cs.setConceptSynonymName(conceptSynonym.getConceptSynonymName());
        cs.setLanguageConceptId(conceptSynonym.getLanguageConceptId());
        return cs;
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

        dbDomainDao.save(DBDOMAIN_1);
        dbDomainDao.save(DBDOMAIN_2);
        dbDomainDao.save(DBDOMAIN_3);
        dbDomainDao.save(DBDOMAIN_4);

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

        conceptSynonymDao.save(CONCEPT_SYNONYM_1);
        conceptSynonymDao.save(CONCEPT_SYNONYM_2);
        conceptSynonymDao.save(CONCEPT_SYNONYM_3);
    }

    private void assertResults(ResponseEntity<ConceptListResponse> response,
                               Concept... expectedConcepts) {
        assertThat(response.getBody().getItems().equals(Arrays.asList(expectedConcepts)));
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
