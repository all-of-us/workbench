package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.CriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortBuilderControllerTest {

  private CohortBuilderController controller;

  @Mock private BigQueryService bigQueryService;

  @Mock private CloudStorageService cloudStorageService;

  @Mock private CohortQueryBuilder cohortQueryBuilder;

  @Mock private CdrVersionDao cdrVersionDao;

  @Mock private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

  @Mock private CdrVersionService cdrVersionService;

  @Autowired private CriteriaDao criteriaDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CriteriaAttributeDao criteriaAttributeDao;

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired private ConceptDao conceptDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Mock private Provider<WorkbenchConfig> configProvider;

  private WorkbenchConfig testConfig;

  @Before
  public void setUp() {
    testConfig = new WorkbenchConfig();
    testConfig.cohortbuilder = new WorkbenchConfig.CohortBuilderConfig();
    testConfig.cohortbuilder.enableListSearch = false;
    when(configProvider.get()).thenReturn(testConfig);

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(criteriaDao, cloudStorageService, configProvider);

    controller =
        new CohortBuilderController(
            bigQueryService,
            cohortQueryBuilder,
            criteriaDao,
            cbCriteriaDao,
            criteriaAttributeDao,
            cbCriteriaAttributeDao,
            cdrVersionDao,
            genderRaceEthnicityConceptProvider,
            cdrVersionService,
            elasticSearchService,
            configProvider);

    jdbcTemplate.execute("delete from criteria");
  }

  @Test
  public void getCriteriaBy() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria icd9CriteriaParent =
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .count("0")
            .hierarchy(true)
            .standard(false)
            .parentId(0L);
    cbCriteriaDao.save(icd9CriteriaParent);
    CBCriteria icd9Criteria =
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .count("0")
            .hierarchy(true)
            .standard(false)
            .parentId(icd9CriteriaParent.getId());
    cbCriteriaDao.save(icd9Criteria);

    assertEquals(
        createResponseCriteria(icd9CriteriaParent),
        controller
            .getCriteriaBy(
                1L, DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)
            .getBody()
            .getItems()
            .get(0));
    assertEquals(
        createResponseCriteria(icd9Criteria),
        controller
            .getCriteriaBy(
                1L,
                DomainType.CONDITION.toString(),
                CriteriaType.ICD9CM.toString(),
                false,
                icd9CriteriaParent.getId())
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaByExceptions() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    try {
      controller.getCriteriaBy(1L, null, null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaBy(1L, "blah", null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaBy(1L, "blah", "blah", false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaBy(1L, DomainType.CONDITION.toString(), "blah", false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void getCriteriaByDemo() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria demoCriteria =
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .count("0")
            .parentId(0L);
    cbCriteriaDao.save(demoCriteria);

    assertEquals(
        createResponseCriteria(demoCriteria),
        controller
            .getCriteriaBy(
                1L, DomainType.PERSON.toString(), CriteriaType.AGE.toString(), false, null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteMatchesSynonyms() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria criteria =
        new CBCriteria()
            .domainId(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .count("0")
            .hierarchy(true)
            .standard(true)
            .synonyms("LP12*[MEASUREMENT_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteMatchesCode() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria criteria =
        new CBCriteria()
            .domainId(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .count("0")
            .hierarchy(true)
            .standard(true)
            .code("LP123")
            .synonyms("+[MEASUREMENT_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteSnomed() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria criteria =
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .count("0")
            .hierarchy(true)
            .standard(true)
            .synonyms("LP12*[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.CONDITION.toString(),
                "LP12",
                CriteriaType.SNOMED.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteExceptions() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    try {
      controller.getCriteriaAutoComplete(1L, null, "blah", null, null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaAutoComplete(1L, "blah", "blah", null, null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaAutoComplete(1L, "blah", "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaAutoComplete(
          1L, DomainType.CONDITION.toString(), "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSourceCode() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .code("001")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.LOINC.toString())
            .attribute(Boolean.FALSE)
            .standard(false)
            .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "001", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermLikeSourceCode() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .code("001")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.LOINC.toString())
            .attribute(Boolean.FALSE)
            .standard(false)
            .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "00", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesStandardCode() throws Exception {
    CBCriteria criteria1 =
        new CBCriteria()
            .code("672535")
            .count("-1")
            .conceptId("19001487")
            .domainId(DomainType.DRUG.toString())
            .group(Boolean.FALSE)
            .selectable(Boolean.TRUE)
            .name("4-Way")
            .parentId(0)
            .type(CriteriaType.BRAND.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("[DRUG_rank1]");
    cbCriteriaDao.save(criteria1);
    CBCriteria criteria2 =
        new CBCriteria()
            .code("8163")
            .count("2")
            .conceptId("1135766")
            .domainId(DomainType.DRUG.toString())
            .group(Boolean.FALSE)
            .selectable(Boolean.TRUE)
            .name("Phenylephrine")
            .parentId(0)
            .type(CriteriaType.RXNORM.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("[DRUG_rank1]");
    cbCriteriaDao.save(criteria2);
    CBCriteria criteria3 =
        new CBCriteria()
            .code("8163")
            .count("87551")
            .conceptId("1135767")
            .domainId(DomainType.DRUG.toString())
            .group(Boolean.FALSE)
            .selectable(Boolean.TRUE)
            .name("Phenylephrine1")
            .parentId(0)
            .type(CriteriaType.RXNORM.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("[DRUG_rank1]");
    cbCriteriaDao.save(criteria3);
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (19001487, 1135766)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (19001487, 1135767)");
    Concept concept1 = new Concept().conceptId(1135766).conceptClassId("Ingredient");
    conceptDao.save(concept1);
    Concept concept2 = new Concept().conceptId(1135767).conceptClassId("Ingredient");
    conceptDao.save(concept2);

    List<org.pmiops.workbench.model.Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "672535", null)
            .getBody()
            .getItems();
    assertEquals(2, results.size());
    assertEquals(createResponseCriteria(criteria3), results.get(0));
    assertEquals(createResponseCriteria(criteria2), results.get(1));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesStandardCode() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .code("LP12")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.LOINC.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSynonyms() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .code("001")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.LOINC.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("LP12*[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesSynonyms() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    CBCriteria criteria =
        new CBCriteria()
            .code("001")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.DRUG.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.ATC.toString())
            .attribute(Boolean.FALSE)
            .standard(true)
            .synonyms("LP12*[DRUG_rank1]");
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void getDrugBrandOrIngredientByName() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria drugATC =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .code("LP12345")
            .name("drugName")
            .group(true)
            .selectable(true)
            .count("0");
    CBCriteria drugBrand =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.BRAND.toString())
            .code("LP6789")
            .name("brandName")
            .group(true)
            .selectable(true)
            .count("0");
    cbCriteriaDao.save(drugATC);
    cbCriteriaDao.save(drugBrand);

    assertEquals(
        createResponseCriteria(drugATC),
        controller.getDrugBrandOrIngredientByValue(1L, "drugN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrand),
        controller.getDrugBrandOrIngredientByValue(1L, "brandN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrand),
        controller.getDrugBrandOrIngredientByValue(1L, "LP6789", null).getBody().getItems().get(0));
  }

  @Test
  public void getDrugIngredientByConceptId() throws Exception {
    testConfig.cohortbuilder.enableListSearch = true;
    CBCriteria drug =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .code("LP12345")
            .name("drugName")
            .conceptId("12345")
            .group(true)
            .selectable(true)
            .count("0");
    cbCriteriaDao.save(drug);
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (1247, 12345)");
    conceptDao.save(new Concept().conceptId(12345).conceptClassId("Ingredient"));

    assertEquals(
        createResponseCriteria(drug),
        controller.getDrugIngredientByConceptId(1L, 1247L).getBody().getItems().get(0));

    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void getCriteriaAttributeByConceptId() throws Exception {
    CriteriaAttribute criteriaAttributeMin =
        criteriaAttributeDao.save(
            new CriteriaAttribute()
                .conceptId(1L)
                .conceptName("MIN")
                .estCount("10")
                .type("NUM")
                .valueAsConceptId(0L));
    CriteriaAttribute criteriaAttributeMax =
        criteriaAttributeDao.save(
            new CriteriaAttribute()
                .conceptId(1L)
                .conceptName("MAX")
                .estCount("100")
                .type("NUM")
                .valueAsConceptId(0L));

    List<org.pmiops.workbench.model.CriteriaAttribute> attrs =
        controller
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
    SearchGroupItem inSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(inSearchParameter);
    SearchGroupItem exSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(exSearchParameter);
    SearchGroup inSearchGroup = new SearchGroup().addItemsItem(inSearchGroupItem);
    SearchGroup exSearchGroup = new SearchGroup().addItemsItem(exSearchGroupItem);
    SearchRequest searchRequest =
        new SearchRequest().addIncludesItem(inSearchGroup).addExcludesItem(exSearchGroup);
    // Temporal includes
    inSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP includes
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased includes
    inSearchParameter.subtype(TreeSubType.DEC.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP includes
    inSearchGroup.temporal(true);
    inSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // No temporal/BP/Decease
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(TreeSubType.HR_DETAIL.toString());
    assertFalse(controller.isApproximate(searchRequest));
    // Temporal excludes
    exSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP excludes
    exSearchGroup.temporal(false);
    exSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased excludes
    exSearchParameter.subtype(TreeSubType.DEC.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP excludes
    exSearchGroup.temporal(true);
    exSearchParameter.subtype(TreeSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
  }

  private Criteria createCriteria(
      String type,
      String subtype,
      long parentId,
      String code,
      String name,
      String domain,
      String conceptId,
      boolean group,
      boolean selectable) {
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

  // TODO:Remove freemabd
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

  private org.pmiops.workbench.model.Criteria createResponseCriteria(CBCriteria cbCriteria) {
    return new org.pmiops.workbench.model.Criteria()
        .code(cbCriteria.getCode())
        .conceptId(cbCriteria.getConceptId() == null ? null : new Long(cbCriteria.getConceptId()))
        .count(new Long(cbCriteria.getCount()))
        .domainId(cbCriteria.getDomainId())
        .group(cbCriteria.getGroup())
        .hasAttributes(cbCriteria.getAttribute())
        .id(cbCriteria.getId())
        .name(cbCriteria.getName())
        .parentId(cbCriteria.getParentId())
        .selectable(cbCriteria.getSelectable())
        .subtype(cbCriteria.getSubtype())
        .type(cbCriteria.getType())
        .path(cbCriteria.getPath())
        .hasAncestorData(cbCriteria.getAncestorData())
        .hasHierarchy(cbCriteria.getHierarchy())
        .isStandard(cbCriteria.getStandard())
        .value(cbCriteria.getValue());
  }

  private org.pmiops.workbench.model.CriteriaAttribute createResponseCriteriaAttribute(
      CriteriaAttribute criteriaAttribute) {
    return new org.pmiops.workbench.model.CriteriaAttribute()
        .id(criteriaAttribute.getId())
        .valueAsConceptId(criteriaAttribute.getValueAsConceptId())
        .conceptName(criteriaAttribute.getConceptName())
        .type(criteriaAttribute.getType())
        .estCount(criteriaAttribute.getEstCount());
  }
}
