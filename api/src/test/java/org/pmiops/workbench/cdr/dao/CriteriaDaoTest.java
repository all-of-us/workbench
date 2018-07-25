package org.pmiops.workbench.cdr.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CriteriaDaoTest {

  private static final String TYPE_ICD9 = "ICD9";
  private static final String TYPE_ICD10 = "ICD10";
  private static final String TYPE_CPT = "CPT";
  private static final String TYPE_DEMO = "DEMO";
  private static final String TYPE_PM = "PM";
  private static final String TYPE_DRUG = "DRUG";
  private static final String SUBTYPE_NONE = null;
  private static final String SUBTYPE_ICD10PCS = "ICD10PCS";
  private static final String SUBTYPE_RACE = "RACE";
  private static final String SUBTYPE_AGE = "AGE";
  private static final String SUBTYPE_BP = "BP";
  private static final String SUBTYPE_ATC = "ATC";
  private static final String SUBTYPE_BRAND = "BRAND";

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptRelationshipDao conceptRelationshipDao;

  private Criteria icd9Criteria1;
  private Criteria icd9Criteria2;
  private Criteria demoCriteria1;
  private Criteria demoCriteria1a;
  private Criteria demoCriteria2;
  private Criteria icd10Criteria1;
  private Criteria icd10Criteria2;
  private Criteria cptCriteria1;
  private Criteria cptCriteria2;
  private Criteria parentIcd9;
  private Criteria childIcd9;
  private Criteria parentDemo;
  private Criteria parentIcd10;
  private Criteria childIcd10;
  private Criteria pmCriteria;
  private Criteria drugCriteriaIngredient;
  private Criteria drugCriteriaBrand;

  @Before
  public void setUp() {
    icd9Criteria1 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "002", "blah chol", 0, false, true, null);
    icd9Criteria2 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "001", "chol blah", 0, false, true, null);
    parentDemo = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "Race/Ethnicity", "Race/Ethnicity", 0, true, true, null);
    demoCriteria1 = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "AF", "African", parentDemo.getId(), false, true, null);
    demoCriteria1a = createCriteria(TYPE_DEMO, SUBTYPE_RACE, "B", "African American", parentDemo.getId(), false, true, null);
    demoCriteria2 = createCriteria(TYPE_DEMO, SUBTYPE_AGE, "Age", "demo age", 0, false, true, null);
    icd10Criteria1 = createCriteria(TYPE_ICD10, SUBTYPE_NONE, "002", "icd10 test 1", 0, false, true, null);
    icd10Criteria2 = createCriteria(TYPE_ICD10, SUBTYPE_NONE, "001", "icd10 test 2", 0, false, true, null);
    cptCriteria1 = createCriteria(TYPE_CPT, SUBTYPE_NONE, "0039T", "zzzcptzzz", 0, false, true, null);
    cptCriteria2 = createCriteria(TYPE_CPT, SUBTYPE_NONE, "0001T", "zzzCPTxxx", 0, false, true, null);
    parentIcd9 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "003", "name", 0, true, true, null);
    parentIcd10 = createCriteria(TYPE_ICD10, SUBTYPE_ICD10PCS, "003", "name", 0, true, true, null);
    pmCriteria = createCriteria(TYPE_PM, SUBTYPE_BP, "1", "Hypotensive (Systolic <= 90 / Diastolic <= 60)", 0, false, true,
      "[{'name':'Systolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['90']},{'name':'Diastolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['60']}]");
    drugCriteriaIngredient = createCriteria(TYPE_DRUG, SUBTYPE_ATC, "", "ACETAMIN", 0, false, true, "").conceptId("1");
    drugCriteriaBrand = createCriteria(TYPE_DRUG, SUBTYPE_BRAND, "", "BLAH", 0, false, true, "");

    criteriaDao.save(icd9Criteria1);
    criteriaDao.save(icd9Criteria2);
    criteriaDao.save(parentDemo);
    criteriaDao.save(demoCriteria1);
    criteriaDao.save(demoCriteria1a);
    criteriaDao.save(demoCriteria2);
    criteriaDao.save(icd10Criteria1);
    criteriaDao.save(icd10Criteria2);
    criteriaDao.save(cptCriteria1);
    criteriaDao.save(cptCriteria2);
    criteriaDao.save(parentIcd9);
    criteriaDao.save(parentIcd10);
    childIcd9 = createCriteria(TYPE_ICD9, SUBTYPE_NONE, "003.1", "name", parentIcd9.getId(), false, true, null);
    criteriaDao.save(childIcd9);
    childIcd10 = createCriteria(TYPE_ICD10, SUBTYPE_ICD10PCS, "003.1", "name", parentIcd10.getId(), false, true, null);
    criteriaDao.save(childIcd10);
    criteriaDao.save(pmCriteria);
    criteriaDao.save(drugCriteriaIngredient);
    criteriaDao.save(drugCriteriaBrand);

    conceptDao.save(new Concept().conceptId(1L).conceptClassId("Ingredient"));
    conceptRelationshipDao.save(
      new ConceptRelationship().conceptRelationshipId(
        new ConceptRelationshipId().relationshipId("1").conceptId1(12345L).conceptId2(1L)
      )
    );
  }

  @After
  public void tearDown() {
    criteriaDao.delete(icd9Criteria1);
    criteriaDao.delete(icd9Criteria2);
    criteriaDao.delete(demoCriteria1);
    criteriaDao.delete(demoCriteria1a);
    criteriaDao.delete(demoCriteria2);
    criteriaDao.delete(icd10Criteria1);
    criteriaDao.delete(icd10Criteria2);
    criteriaDao.delete(cptCriteria1);
    criteriaDao.delete(cptCriteria2);
    criteriaDao.delete(parentIcd9);
    criteriaDao.delete(childIcd9);
    criteriaDao.delete(parentDemo);
    criteriaDao.delete(parentIcd10);
    criteriaDao.delete(childIcd10);
    criteriaDao.delete(pmCriteria);
  }

  @Test
  public void findCriteriaByParentId() throws Exception {
    final List<Criteria> icd9List = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TYPE_ICD9, 0L);
    assertEquals(icd9Criteria1, icd9List.get(0));
    assertEquals(icd9Criteria2, icd9List.get(1));

    final List<Criteria> icd10List = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TYPE_ICD10, 0L);
    assertEquals(icd10Criteria1, icd10List.get(0));
    assertEquals(icd10Criteria2, icd10List.get(1));

    final List<Criteria> cptList = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TYPE_CPT, 0L);
    assertEquals(cptCriteria1, cptList.get(0));
    assertEquals(cptCriteria2, cptList.get(1));

    final List<Criteria> pmList = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TYPE_PM, 0L);
    assertEquals(pmCriteria, pmList.get(0));
  }

  @Test
  public void findCriteriaByType() throws Exception {
    final List<Criteria> icd9List = criteriaDao.findCriteriaByType(TYPE_ICD9);
    final Set<String> typeList = icd9List.stream().map(Criteria::getType).collect(Collectors.toSet());
    assertEquals(typeList.size(), 1);
  }

  @Test
  public void findCriteriaByTypeAndSubtypeOrderByNameAsc() throws Exception {
    final List<Criteria> demoList = criteriaDao.findCriteriaByTypeAndSubtypeOrderByIdAsc(TYPE_DEMO, SUBTYPE_RACE);
    assertEquals(2, demoList.size());
    assertEquals(demoCriteria1, demoList.get(0));
    assertEquals(demoCriteria1a, demoList.get(1));
  }

  @Test
  public void findDrugBrandOrIngredientByName() throws Exception {
    List<Criteria> drugList = criteriaDao.findDrugBrandOrIngredientByName("ETAM");
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("ACE");
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("BL");
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaBrand, drugList.get(0));
  }

  @Test
  public void findDrugIngredientsByConceptId() throws Exception {
    List<Criteria> drugList = criteriaDao.findDrugIngredientByConceptId(12345L);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
  }

  @Test
  public void findCriteriaByTypeAndCode() throws Exception {
    final List<String> icd9DomainList = criteriaDao.findCriteriaByTypeAndCode(TYPE_ICD9, "003");

    assertEquals(1, icd9DomainList.size());
    assertEquals("Condition", icd9DomainList.get(0));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeAndCode() throws Exception {
    final List<String> icd10DomainList = criteriaDao.findCriteriaByTypeAndSubtypeAndCode(TYPE_ICD10, SUBTYPE_ICD10PCS, "003");

    assertEquals(1, icd10DomainList.size());
    assertEquals("Condition", icd10DomainList.get(0));
  }

  private Criteria createCriteria(String type,
                                  String subtype,
                                  String code,
                                  String name,
                                  long parentId,
                                  boolean group,
                                  boolean selectable,
                                  String predefinedAttributes) {
    return new Criteria()
      .code(code)
      .count("10")
      .conceptId("1000")
      .domainId("Condition")
      .group(group)
      .selectable(selectable)
      .name(name)
      .parentId(parentId)
      .type(type)
      .subtype(subtype)
      .attribute(Boolean.FALSE)
      .predefinedAttributes(predefinedAttributes);
  }

}
