package org.pmiops.workbench.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.CriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaAttribute;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortBuilderControllerTest {

  private static final String SUBTYPE_NONE = null;
  private static final String SUBTYPE_AGE = "AGE";
  private static final String SUBTYPE_LAB = "LAB";
  private static final String SUBTYPE_ATC = "ATC";
  private static final String SUBTYPE_BRAND = "BRAND";

  private CohortBuilderController controller;

  @Mock
  private BigQueryService bigQueryService;

  @Mock
  private CloudStorageService cloudStorageService;

  @Mock
  private ParticipantCounter participantCounter;

  @Mock
  private CdrVersionDao cdrVersionDao;

  @Mock
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

  @Mock
  private CdrVersionService cdrVersionService;

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private CriteriaAttributeDao criteriaAttributeDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Mock
  private Provider<WorkbenchConfig> configProvider;

  @Before
  public void setUp() {
    WorkbenchConfig testConfig = new WorkbenchConfig();
    testConfig.cohortbuilder = new WorkbenchConfig.CohortBuilderConfig();
    testConfig.cohortbuilder.enableListSearch = false;
    when(configProvider.get()).thenReturn(testConfig);

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(criteriaDao, cloudStorageService, configProvider);

    controller = new CohortBuilderController(bigQueryService,
      participantCounter, criteriaDao, criteriaAttributeDao,
      cdrVersionDao, genderRaceEthnicityConceptProvider, cdrVersionService,
      elasticSearchService, configProvider);

    jdbcTemplate.execute("delete from criteria");
  }

  @Test
  public void getCriteriaByTypeAndParentId() throws Exception {
    Criteria icd9CriteriaParent = criteriaDao.save(
      createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, 0L, "001", "name", DomainType.CONDITION.name(), null, true, true)
    );
    Criteria icd9CriteriaChild = criteriaDao.save(
      createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, icd9CriteriaParent.getId(), "001.1", "name", DomainType.CONDITION.name(), null, false, true)
    );

    assertEquals(
      createResponseCriteria(icd9CriteriaParent),
      controller
        .getCriteriaBy(1L, TreeType.ICD9.name(), null,  0L, null)
        .getBody()
        .getItems()
        .get(0)
    );
    assertEquals(
      createResponseCriteria(icd9CriteriaChild),
      controller
        .getCriteriaBy(1L, TreeType.ICD9.name(), null, icd9CriteriaParent.getId(), null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaByExceptions() throws Exception {
    try {
      controller
        .getCriteriaBy(1L, null, null,  null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertEquals("Bad Request: Please provide a valid criteria type. null is not valid.", bre.getMessage());
    }

    try {
      controller
        .getCriteriaBy(1L, "blah", null,  null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertEquals("Bad Request: Please provide a valid criteria type. blah is not valid.", bre.getMessage());
    }

    try {
      controller
        .getCriteriaBy(1L, TreeType.ICD9.name(), "blah",  null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertEquals("Bad Request: Please provide a valid criteria subtype. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void getCriteriaByTypeAndSubtypeAndParentId() throws Exception {
    jdbcTemplate.execute("delete from criteria where subtype = 'ATC'");
    Criteria drugATCCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "drugName", DomainType.DRUG.name(), "12345", true, true)
    );

    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getCriteriaBy(1L, TreeType.DRUG.name(), SUBTYPE_ATC, 0L, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaChildrenByTypeAndParentId() throws Exception {
    Criteria drugATCCriteriaChild = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP72636", "differentName", DomainType.DRUG.name(), "12345", false, true).synonyms("+drugN*")
    );

    assertEquals(
      createResponseCriteria(drugATCCriteriaChild),
      controller
        .getCriteriaBy(1L, TreeType.DRUG.name(), null, 2L, true)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaByTypeAndSubtype() throws Exception {
    Criteria demoCriteria = criteriaDao.save(
      createCriteria(TreeType.DEMO.name(), SUBTYPE_AGE, 0L, null, "age", null, null, true, true)
    );

    assertEquals(
      createResponseCriteria(demoCriteria),
      controller
        .getCriteriaBy(1L, TreeType.DEMO.name(), TreeSubType.AGE.name(), null, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaAutoCompleteNoSubtype() throws Exception {
    Criteria labMeasurement = criteriaDao.save(
      createCriteria(TreeType.MEAS.name(), SUBTYPE_LAB, 0L, "xxxLP12345", "name", DomainType.MEASUREMENT.name(), null, false, true).synonyms("LP12*\"[rank1]\"")
    );

    assertEquals(
      createResponseCriteria(labMeasurement),
      controller
        .getCriteriaAutoComplete(1L, TreeType.MEAS.name(),"LP12", null, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaAutoCompleteWithSubtype() throws Exception {
    Criteria drugATCCriteriaChild = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP72636", "differentName", DomainType.DRUG.name(), "12345", false, true).synonyms("drugN*\"[rank1]\"")
    );

    assertEquals(
      createResponseCriteria(drugATCCriteriaChild),
      controller
        .getCriteriaAutoComplete(1L, TreeType.DRUG.name(),"drugN", TreeSubType.ATC.name(), null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaAutoCompletePPI() throws Exception {
    Criteria ppiCriteriaParent = criteriaDao.save(
      createCriteria(TreeType.PPI.name(), TreeSubType.BASICS.name(), 0L, "324836",
        "Are you currently covered by any of the following types of health insurance or health coverage plans? Select all that apply from one group",
        DomainType.OBSERVATION.name(), "43529119", false, true).synonyms("covered*\"[rank1]\"")
    );

    assertEquals(
      createResponseCriteria(ppiCriteriaParent),
      controller
        .getCriteriaAutoComplete(1L, TreeType.PPI.name(),"covered", null, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void findCriteriaByDomainAndSearchTerm() throws Exception {
    Criteria criteria = new Criteria()
      .code("001")
      .count("10")
      .conceptId("123")
      .domainId(DomainType.MEASUREMENT.toString())
      .group(Boolean.TRUE)
      .selectable(Boolean.TRUE)
      .name("chol blah")
      .parentId(0)
      .type(CriteriaType.LOINC.toString())
      .attribute(Boolean.FALSE)
      .standard(true)
      .synonyms("LP12*\"[rank1]\"");
    Criteria labMeasurement = criteriaDao.save(criteria);

    assertEquals(
      createResponseCriteria(labMeasurement),
      controller
        .findCriteriaByDomainAndSearchTerm(1L, DomainType.MEASUREMENT.name(),"LP12", true, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getDrugBrandOrIngredientByName() throws Exception {
    Criteria drugATCCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "drugName", DomainType.DRUG.name(), "12345", true, true)
    );
    Criteria drugBrandCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_BRAND, 0L, "LP6789", "brandName", DomainType.DRUG.name(), "1235", true, true)
    );

    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getDrugBrandOrIngredientByValue(1L, "drugN", null)
        .getBody()
        .getItems()
        .get(0)
    );

    assertEquals(
      createResponseCriteria(drugBrandCriteria),
      controller
        .getDrugBrandOrIngredientByValue(1L, "brandN", null)
        .getBody()
        .getItems()
        .get(0)
    );

    assertEquals(
      createResponseCriteria(drugBrandCriteria),
      controller
        .getDrugBrandOrIngredientByValue(1L, "LP6789", null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getDrugIngredientByConceptId() throws Exception {
    Criteria drugATCCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "drugName", DomainType.DRUG.name(), "12345", true, true)
    );
    jdbcTemplate.execute("create table criteria_relationship (concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into criteria_relationship(concept_id_1, concept_id_2) values (1247, 12345)");
    conceptDao.save(new Concept().conceptId(12345).conceptClassId("Ingredient"));

    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getDrugIngredientByConceptId(1L, 1247L)
        .getBody()
        .getItems()
        .get(0)
    );

    jdbcTemplate.execute("drop table criteria_relationship");
  }

  @Test
  public void getCriteriaByType() throws Exception {
    Criteria drugATCCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "drugName", DomainType.DRUG.name(), "12345", true, true)
    );

    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getCriteriaBy(1L, drugATCCriteria.getType(), null, null, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaAttributeByConceptId() throws Exception {
    CriteriaAttribute criteriaAttributeMin = criteriaAttributeDao.save(
      new CriteriaAttribute().conceptId(1L).conceptName("MIN").estCount("10").type("NUM").valueAsConceptId(0L)
    );
    CriteriaAttribute criteriaAttributeMax = criteriaAttributeDao.save(
      new CriteriaAttribute().conceptId(1L).conceptName("MAX").estCount("100").type("NUM").valueAsConceptId(0L)
    );

    List<org.pmiops.workbench.model.CriteriaAttribute> attrs = controller
      .getCriteriaAttributeByConceptId(1L, criteriaAttributeMin.getConceptId())
      .getBody()
      .getItems();
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)));
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)));

    criteriaAttributeDao.delete(criteriaAttributeMin.getId());
    criteriaAttributeDao.delete(criteriaAttributeMax.getId());
  }

  @Test
  public void isApproximate() throws Exception {
    SearchParameter inSearchParameter = new SearchParameter();
    SearchParameter exSearchParameter = new SearchParameter();
    SearchGroupItem inSearchGroupItem = new SearchGroupItem()
      .addSearchParametersItem(inSearchParameter);
    SearchGroupItem exSearchGroupItem = new SearchGroupItem()
      .addSearchParametersItem(exSearchParameter);
    SearchGroup inSearchGroup = new SearchGroup()
      .addItemsItem(inSearchGroupItem);
    SearchGroup exSearchGroup = new SearchGroup()
      .addItemsItem(exSearchGroupItem);
    SearchRequest searchRequest = new SearchRequest()
      .addIncludesItem(inSearchGroup)
      .addExcludesItem(exSearchGroup);
    //Temporal includes
    inSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    //BP includes
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    //Deceased includes
    inSearchParameter.subtype(TreeSubType.DEC.toString());
    assertTrue(controller.isApproximate(searchRequest));
    //Temporal and BP includes
    inSearchGroup.temporal(true);
    inSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    //No temporal/BP/Decease
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(TreeSubType.HR_DETAIL.toString());
    assertFalse(controller.isApproximate(searchRequest));
    //Temporal excludes
    exSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    //BP excludes
    exSearchGroup.temporal(false);
    exSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    //Deceased excludes
    exSearchParameter.subtype(TreeSubType.DEC.toString());
    assertTrue(controller.isApproximate(searchRequest));
    //Temporal and BP excludes
    exSearchGroup.temporal(true);
    exSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
  }

  private Criteria createCriteria(String type, String subtype, long parentId, String code, String name, String domain, String conceptId, boolean group, boolean selectable) {
    return new Criteria()
      .parentId(parentId)
      .type(type)
      .subtype(subtype)
      .code(code)
      .name(name)
      .group(group)
      .selectable(selectable)
      .count("16")
      .domainId(domain)
      .conceptId(conceptId)
      .path("1.2.3.4");
  }

  private org.pmiops.workbench.model.Criteria createResponseCriteria(Criteria criteria) {
    return new org.pmiops.workbench.model.Criteria()
      .code(criteria.getCode())
      .conceptId(criteria.getConceptId() == null ? null : new Long(criteria.getConceptId()))
      .count(new Long(criteria.getCount()))
      .domainId(criteria.getDomainId())
      .group(criteria.getGroup())
      .hasAttributes(criteria.getAttribute())
      .id(criteria.getId())
      .name(criteria.getName())
      .parentId(criteria.getParentId())
      .selectable(criteria.getSelectable())
      .subtype(criteria.getSubtype())
      .type(criteria.getType())
      .path(criteria.getPath());
  }

  private org.pmiops.workbench.model.CriteriaAttribute createResponseCriteriaAttribute(CriteriaAttribute criteriaAttribute) {
    return new org.pmiops.workbench.model.CriteriaAttribute()
      .id(criteriaAttribute.getId())
      .valueAsConceptId(criteriaAttribute.getValueAsConceptId())
      .conceptName(criteriaAttribute.getConceptName())
      .type(criteriaAttribute.getType())
      .estCount(criteriaAttribute.getEstCount());
  }
}
