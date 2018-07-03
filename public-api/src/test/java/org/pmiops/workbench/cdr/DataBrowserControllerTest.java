package org.pmiops.workbench.publicapi;

import java.util.stream.Collectors;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import org.pmiops.workbench.model.Domain;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptRelationshipDao;
import org.pmiops.workbench.cdr.dao.DbDomainDao;
import org.pmiops.workbench.model.StandardConceptFilter;
//import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
//import org.pmiops.workbench.cdr.dao.AchillesResultDao;
//import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DbDomainListResponse;
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
                            .prevalence(concept.getPrevalence());
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
            .prevalence(0.2F);

    private static final Concept CLIENT_CONCEPT_2 = new Concept()
            .conceptId(456L)
            .conceptName("b concept")
            .conceptCode("002")
            .conceptClassId("classId2")
            .vocabularyId("V2")
            .domainId("Measurement")
            .count(456L)
            .sourceCountValue(25L)
            .prevalence(0.3F);

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
            .prevalence(0.4F);

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
            .prevalence(0.5F);

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
            .prevalence(0.9F);

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
            .prevalence(0.1F);

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
            .prevalence(0.0F);

    private static final DbDomain CLIENT_DB_DOMAIN_1 = new DbDomain()
            .domainId("Condition")
            .domainDisplay("Diagnoses")
            .domainDesc("Condition Domain")
            .dbType("domain_filter")
            .domainRoute("condition")
            .conceptId(19L)
            .countValue(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_2 = new DbDomain()
            .domainId("Drug")
            .domainDisplay("Medications")
            .domainDesc("Drug Domain")
            .dbType("domain_filter")
            .domainRoute("drug")
            .conceptId(13L)
            .countValue(0L);

    private static final DbDomain CLIENT_DB_DOMAIN_3 = new DbDomain()
            .domainId("Lifestyle")
            .domainDisplay("Lifestyle")
            .domainDesc("he Lifestyle module provides information on smoking, alcohol and recreational drug use")
            .dbType("survey")
            .domainRoute("ppi")
            .conceptId(1585855L)
            .countValue(568120L);

    private static final DbDomain CLIENT_DB_DOMAIN_4 = new DbDomain()
            .domainId("TheBasics")
            .domainDisplay("The Basics")
            .domainDesc("The Basics module provides demographics and economic information for participants")
            .dbType("survey")
            .domainRoute("ppi")
            .conceptId(1586134L)
            .countValue(567437L);

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
    ConceptRelationshipDao conceptRelationshipDao;
    @Autowired
    private DbDomainDao dbDomainDao;
    /*
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    */
    @PersistenceContext
    private EntityManager entityManager;


    private DataBrowserController dataBrowserController;


    @Before
    public void setUp() {
        ConceptService conceptService = new ConceptService(entityManager);
        dataBrowserController = new DataBrowserController(conceptService, conceptDao, dbDomainDao);
    }


    @Test
    public void testGetParentConcepts() throws Exception {
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.getParentConcepts(1234L);
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_5)
        ;
    }


    @Test
    public void testGetSourceConcepts() throws Exception {
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.getSourceConcepts(7890L, 15);
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_4, CONCEPT_2)
        ;
    }


    @Test
    public void testGetDomainFilters() throws Exception {
        saveData();
        assertDomains(
                dataBrowserController.getDomainFilters(), CLIENT_DB_DOMAIN_1, CLIENT_DB_DOMAIN_2
        );
    }

    @Test
    public void testGetSurveyList() throws Exception{
        saveData();
        assertDomains(
                dataBrowserController.getSurveyList(), CLIENT_DB_DOMAIN_3, CLIENT_DB_DOMAIN_4
        );
    }

    @Test
    public void testGetDbDomains() throws Exception{
        saveData();
        assertDomains(
                dataBrowserController.getDbDomains(), CLIENT_DB_DOMAIN_1, CLIENT_DB_DOMAIN_2, CLIENT_DB_DOMAIN_3, CLIENT_DB_DOMAIN_4
        );
    }

    @Test
    public void testConceptSearchWithEmptyQuery() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("")
        .domain(Domain.CONDITION));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_1, CONCEPT_5, CONCEPT_6)
        ;
    }

    @Test
    public void testConceptSearchStandardConcept() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("002")
        .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .doesNotContain(CONCEPT_2);
    }

    @Test
    public void testConceptSearchStandardCodeIdMatchFilter() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("")
                .standardConceptFilter(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        List<Concept> standard_concepts = Arrays.asList(CONCEPT_1, CONCEPT_4, CONCEPT_5, CONCEPT_6, CONCEPT_7);
        assertThat(concepts)
                .doesNotContain(standard_concepts);
    }

    @Test
    public void testConceptSearchNonStandardConcepts() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("7891")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        //Search on concept id fetches the non standard concept
        assertThat(concepts)
                .contains(CONCEPT_6);
    }


    @Test
    public void testConceptSearchEmptyCount() throws Exception{
        // We can't test limiting to count > 0 with a concept name search because the match function does not work in hibernate. So we make several concepts with same concept code and one with count 0. The limit > 0 works the same weather it is code or name match.
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_4, CONCEPT_6)
                .inOrder();
    }

    @Test
    public void testConceptIdSearch() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("456")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_2)
                .inOrder();
    }

    @Test
    public void testConceptSearchDomainFilter() throws Exception{
        saveData();
        ResponseEntity<ConceptListResponse> response = dataBrowserController.searchConcepts(new SearchConceptsRequest().query("004").domain(Domain.CONDITION));
        List<Concept> concepts = response.getBody().getItems().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList());
        assertThat(concepts)
                .containsExactly(CONCEPT_6)
                .inOrder();
    }


    @Test
    public void testConceptCodeMatch() throws Exception {
        saveData();
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
        dbd.setCountValue(dbDomain.getCountValue());
        return dbd;
    }

    private void saveData() {
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
    }

    private void assertDomains(ResponseEntity<DbDomainListResponse> response,
                               DbDomain... expectedDomains) {
        assertThat(response.getBody().getItems().equals(Arrays.asList(expectedDomains)));
    }
}
