package org.pmiops.workbench.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptRelationshipDao;
import org.pmiops.workbench.cdr.dao.CriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

  private Criteria icd9CriteriaParent;
  private Criteria icd9CriteriaChild;
  private Criteria demoCriteria;
  private Criteria labMeasurement;
  private Criteria drugATCCriteria;
  private Criteria drugATCCriteriaChild;
  private Criteria drugBrandCriteria;
  private CriteriaAttribute criteriaAttributeMin;
  private CriteriaAttribute criteriaAttributeMax;

  @Autowired
  private CohortBuilderController controller;

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private CriteriaAttributeDao criteriaAttributeDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptRelationshipDao conceptRelationshipDao;

  @TestConfiguration
  @Import({
    CohortBuilderController.class
  })
  @MockBean({
    CdrVersionService.class,
    CdrVersionDao.class,
    BigQueryService.class,
    ParticipantCounter.class
  })
  static class Configuration {}

  @Before
  public void setUp() {
    icd9CriteriaParent = criteriaDao.save(
      createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, 0L, "001", "name", DomainType.CONDITION.name(), null, true)
    );
    icd9CriteriaChild = criteriaDao.save(
      createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, icd9CriteriaParent.getId(), "001.1", "name", DomainType.CONDITION.name(), null, false)
    );
    demoCriteria = criteriaDao.save(
      createCriteria(TreeType.DEMO.name(), SUBTYPE_AGE, 0L, null, "age", null, null, true)
    );
    labMeasurement = criteriaDao.save(
      createCriteria(TreeType.MEAS.name(), SUBTYPE_LAB, 0L, "xxxLP12345", "name", DomainType.MEASUREMENT.name(), null, true)
    );
    drugATCCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "drugName", DomainType.DRUG.name(), "12345", true)
    );
    drugBrandCriteria = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_BRAND, 0L, "LP12345", "brandName", DomainType.DRUG.name(), "1235", true)
    );
    drugATCCriteriaChild = criteriaDao.save(
      createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, 0L, "LP12345", "differentName", DomainType.DRUG.name(), "12345", false)
    );
    conceptDao.save(new Concept().conceptId(12345).conceptClassId("Ingredient"));
    conceptRelationshipDao.save(
      new ConceptRelationship().conceptRelationshipId(
        new ConceptRelationshipId()
          .relationshipId("1")
          .conceptId2(12345)
          .conceptId1(1247)
      )
    );
    criteriaAttributeMin = criteriaAttributeDao.save(
      new CriteriaAttribute().conceptId(1L).conceptName("MIN").estCount("10").type("NUM").valueAsConceptId(0L)
    );
    criteriaAttributeMax = criteriaAttributeDao.save(
      new CriteriaAttribute().conceptId(1L).conceptName("MAX").estCount("100").type("NUM").valueAsConceptId(0L)
    );
  }

  @Test
  public void getCriteriaByTypeAndParentId() throws Exception {
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
      assertEquals("Criteria type: null is not valid.", bre.getMessage());
    }

    try {
      controller
        .getCriteriaBy(1L, "blah", null,  null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertEquals("Criteria type: blah is not valid.", bre.getMessage());
    }

    try {
      controller
        .getCriteriaBy(1L, TreeType.ICD9.name(), "blah",  null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertEquals("Criteria subtype: blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void getCriteriaByTypeAndSubtypeAndParentId() throws Exception {
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
    assertEquals(
      createResponseCriteria(demoCriteria),
      controller
        .getCriteriaBy(1L, TreeType.DEMO.name(), SUBTYPE_AGE, null, null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaByTypeForCodeOrName() throws Exception {
    assertEquals(
      createResponseCriteria(labMeasurement),
      controller
        .getCriteriaAutoComplete(1L, TreeType.MEAS.name(),"LP12", null)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getDrugBrandOrIngredientByName() throws Exception {
    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getDrugBrandOrIngredientByName(1L, "drugN")
        .getBody()
        .getItems()
        .get(0)
    );

    assertEquals(
      createResponseCriteria(drugBrandCriteria),
      controller
        .getDrugBrandOrIngredientByName(1L, "brandN")
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getDrugIngredientByConceptId() throws Exception {
    assertEquals(
      createResponseCriteria(drugATCCriteria),
      controller
        .getDrugIngredientByConceptId(1L, 1247L)
        .getBody()
        .getItems()
        .get(0)
    );
  }

  @Test
  public void getCriteriaByType() throws Exception {
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
    List<org.pmiops.workbench.model.CriteriaAttribute> attrs = controller
      .getCriteriaAttributeByConceptId(1L, criteriaAttributeMin.getConceptId())
      .getBody()
      .getItems();
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)));
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)));
  }

  private Criteria createCriteria(String type, String subtype, long parentId, String code, String name, String domain, String conceptId, boolean group) {
    return new Criteria()
      .parentId(parentId)
      .type(type)
      .subtype(subtype)
      .code(code)
      .name(name)
      .group(group)
      .selectable(true)
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
      .conceptId(criteriaAttribute.getConceptId())
      .valueAsConceptId(criteriaAttribute.getValueAsConceptId())
      .conceptName(criteriaAttribute.getConceptName())
      .type(criteriaAttribute.getType())
      .estCount(criteriaAttribute.getEstCount());
  }
}
