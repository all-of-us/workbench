package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortBuilderControllerTest {

  private CohortBuilderController controller;

  @Mock private BigQueryService bigQueryService;

  @Mock private CloudStorageService cloudStorageService;

  @Mock private CohortQueryBuilder cohortQueryBuilder;

  @Mock private CdrVersionDao cdrVersionDao;

  @Mock private CdrVersionService cdrVersionService;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired private ConceptDao conceptDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Mock private Provider<WorkbenchConfig> configProvider;

  private WorkbenchConfig testConfig;

  @Before
  public void setUp() {
    ElasticSearchService elasticSearchService =
        new ElasticSearchService(cbCriteriaDao, cloudStorageService, configProvider);

    controller =
        new CohortBuilderController(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaDao,
            cbCriteriaAttributeDao,
            cdrVersionDao,
            cdrVersionService,
            elasticSearchService,
            configProvider);
  }

  @Test
  public void getCriteriaBy() throws Exception {
    DbCriteria icd9CriteriaParent =
        new DbCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .count("0")
            .hierarchy(true)
            .standard(false)
            .parentId(0L);
    cbCriteriaDao.save(icd9CriteriaParent);
    DbCriteria icd9Criteria =
        new DbCriteria()
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
    DbCriteria demoCriteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
            .code("001")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.ICD9CM.toString())
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
    DbCriteria criteria =
        new DbCriteria()
            .code("00")
            .count("10")
            .conceptId("123")
            .domainId(DomainType.CONDITION.toString())
            .group(Boolean.TRUE)
            .selectable(Boolean.TRUE)
            .name("chol blah")
            .parentId(0)
            .type(CriteriaType.ICD9CM.toString())
            .attribute(Boolean.FALSE)
            .standard(false)
            .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "00", null)
            .getBody()
            .getItems();

    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesStandardCodeBrand() throws Exception {
    DbCriteria criteria1 =
        new DbCriteria()
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

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "672535", null)
            .getBody()
            .getItems();
    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria1), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesStandardCode() throws Exception {
    DbCriteria criteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
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
    DbCriteria criteria =
        new DbCriteria()
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
  public void getStandardCriteriaByDomainAndConceptId() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    DbCriteria criteria =
        new DbCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(true)
            .count("1")
            .conceptId("1")
            .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getStandardCriteriaByDomainAndConceptId(1L, DomainType.CONDITION.toString(), 12345L)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void getDrugBrandOrIngredientByName() throws Exception {
    DbCriteria drugATCCriteria =
        new DbCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .parentId(0L)
            .code("LP12345")
            .name("drugName")
            .conceptId("12345")
            .group(true)
            .selectable(true)
            .count("12");
    cbCriteriaDao.save(drugATCCriteria);
    DbCriteria drugBrandCriteria =
        new DbCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.BRAND.toString())
            .parentId(0L)
            .code("LP6789")
            .name("brandName")
            .conceptId("1235")
            .group(true)
            .selectable(true)
            .count("33");
    cbCriteriaDao.save(drugBrandCriteria);

    assertEquals(
        createResponseCriteria(drugATCCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "drugN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "brandN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "LP6789", null).getBody().getItems().get(0));
  }

  @Test
  public void getCriteriaAttributeByConceptId() throws Exception {
    DbCriteriaAttribute criteriaAttributeMin =
        cbCriteriaAttributeDao.save(
            new DbCriteriaAttribute()
                .conceptId(1L)
                .conceptName("MIN")
                .estCount("10")
                .type("NUM")
                .valueAsConceptId(0L));
    DbCriteriaAttribute criteriaAttributeMax =
        cbCriteriaAttributeDao.save(
            new DbCriteriaAttribute()
                .conceptId(1L)
                .conceptName("MAX")
                .estCount("100")
                .type("NUM")
                .valueAsConceptId(0L));

    List<CriteriaAttribute> attrs =
        controller
            .getCriteriaAttributeByConceptId(1L, criteriaAttributeMin.getConceptId())
            .getBody()
            .getItems();
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)));
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)));
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
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased includes
    inSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP includes
    inSearchGroup.temporal(true);
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // No temporal/BP/Decease
    inSearchGroup.temporal(false);
    inSearchParameter.type(CriteriaType.ETHNICITY.toString()).subtype(null);
    assertFalse(controller.isApproximate(searchRequest));
    // Temporal excludes
    exSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP excludes
    exSearchGroup.temporal(false);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased excludes
    exSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP excludes
    exSearchGroup.temporal(true);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
  }

  private Criteria createResponseCriteria(DbCriteria cbCriteria) {
    return new Criteria()
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

  private CriteriaAttribute createResponseCriteriaAttribute(DbCriteriaAttribute criteriaAttribute) {
    return new CriteriaAttribute()
        .id(criteriaAttribute.getId())
        .valueAsConceptId(criteriaAttribute.getValueAsConceptId())
        .conceptName(criteriaAttribute.getConceptName())
        .type(criteriaAttribute.getType())
        .estCount(criteriaAttribute.getEstCount());
  }
}
