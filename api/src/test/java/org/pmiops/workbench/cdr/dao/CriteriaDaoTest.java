package org.pmiops.workbench.cdr.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.model.TreeType;
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

  private static final String SUBTYPE_NONE = null;
  private static final String SUBTYPE_ICD10PCS = "ICD10PCS";
  private static final String SUBTYPE_RACE = "RACE";
  private static final String SUBTYPE_AGE = "AGE";
  private static final String SUBTYPE_BP = "BP";
  private static final String SUBTYPE_ATC = "ATC";
  private static final String SUBTYPE_BRAND = "BRAND";
  private static final String SUBTYPE_LAB = "LAB";

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
  private Criteria drugCriteriaIngredient1;
  private Criteria drugCriteriaBrand;
  private Criteria labCriteria;

  @Before
  public void setUp() {
    icd9Criteria1 = createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, "002", "blah chol", 0, false, true, null);
    icd9Criteria2 = createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, "001", "chol blah", 0, false, true, null);
    parentDemo = createCriteria(TreeType.DEMO.name(), SUBTYPE_RACE, "Race/Ethnicity", "Race/Ethnicity", 0, true, true, null);
    demoCriteria1 = createCriteria(TreeType.DEMO.name(), SUBTYPE_RACE, "AF", "African", parentDemo.getId(), false, true, null);
    demoCriteria1a = createCriteria(TreeType.DEMO.name(), SUBTYPE_RACE, "B", "African American", parentDemo.getId(), false, true, null);
    demoCriteria2 = createCriteria(TreeType.DEMO.name(), SUBTYPE_AGE, "Age", "demo age", 0, false, true, null);
    icd10Criteria1 = createCriteria(TreeType.ICD10.name(), SUBTYPE_NONE, "002", "icd10 test 1", 0, false, true, null);
    icd10Criteria2 = createCriteria(TreeType.ICD10.name(), SUBTYPE_NONE, "001", "icd10 test 2", 0, false, true, null);
    cptCriteria1 = createCriteria(TreeType.CPT.name(), SUBTYPE_NONE, "0039T", "zzzcptzzz", 0, false, true, null);
    cptCriteria2 = createCriteria(TreeType.CPT.name(), SUBTYPE_NONE, "0001T", "zzzCPTxxx", 0, false, true, null);
    parentIcd9 = createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, "003", "name", 0, true, true, null);
    parentIcd10 = createCriteria(TreeType.ICD10.name(), SUBTYPE_ICD10PCS, "003", "name", 0, true, true, null);
    pmCriteria = createCriteria(TreeType.PM.name(), SUBTYPE_BP, "1", "Hypotensive (Systolic <= 90 / Diastolic <= 60)", 0, false, true,
      "[{'name':'Systolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['90']},{'name':'Diastolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['60']}]");
    drugCriteriaIngredient = createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, "", "ACETAMIN", 0, false, true, "1.2.3.4").conceptId("1");
    drugCriteriaIngredient1 = createCriteria(TreeType.DRUG.name(), SUBTYPE_ATC, "", "MIN1", 0, false, true, "1.2.3.4").conceptId("2");
    drugCriteriaBrand = createCriteria(TreeType.DRUG.name(), SUBTYPE_BRAND, "", "BLAH", 0, false, true, "");
    labCriteria = createCriteria(TreeType.MEAS.name(), SUBTYPE_LAB, "LP1234", "mysearchname", 0, false, false, "0.12345").conceptId("123");

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
    childIcd9 = createCriteria(TreeType.ICD9.name(), SUBTYPE_NONE, "003.1", "name", parentIcd9.getId(), false, true, null);
    criteriaDao.save(childIcd9);
    childIcd10 = createCriteria(TreeType.ICD10.name(), SUBTYPE_ICD10PCS, "003.1", "name", parentIcd10.getId(), false, true, null);
    criteriaDao.save(childIcd10);
    criteriaDao.save(pmCriteria);
    criteriaDao.save(drugCriteriaIngredient);
    criteriaDao.save(drugCriteriaIngredient1);
    criteriaDao.save(drugCriteriaBrand);
    criteriaDao.save(labCriteria);

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
    criteriaDao.delete(drugCriteriaIngredient);
    criteriaDao.delete(drugCriteriaIngredient1);
    criteriaDao.delete(drugCriteriaBrand);
    criteriaDao.delete(labCriteria);
  }

  @Test
  public void findCriteriaByParentId() throws Exception {
    final List<Criteria> icd9List = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.ICD9.name(), 0L);
    assertEquals(icd9Criteria1, icd9List.get(0));
    assertEquals(icd9Criteria2, icd9List.get(1));

    final List<Criteria> icd10List = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.ICD10.name(), 0L);
    assertEquals(icd10Criteria1, icd10List.get(0));
    assertEquals(icd10Criteria2, icd10List.get(1));

    final List<Criteria> cptList = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.CPT.name(), 0L);
    assertEquals(cptCriteria1, cptList.get(0));
    assertEquals(cptCriteria2, cptList.get(1));

    final List<Criteria> pmList = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.PM.name(), 0L);
    assertEquals(pmCriteria, pmList.get(0));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc() throws Exception {
    final List<Criteria> drugList = criteriaDao.findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc(TreeType.DRUG.name(), SUBTYPE_ATC, 0L);
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaIngredient1, drugList.get(1));
  }

  @Test
  public void findCriteriaChildrenByTypeAndParentId() throws Exception {
    final List<Criteria> drugList = criteriaDao.findCriteriaChildrenByTypeAndParentId(TreeType.DRUG.name(), 2L);
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaIngredient1, drugList.get(1));
  }

  @Test
  public void findCriteriaByType() throws Exception {
    final List<Criteria> icd9List = criteriaDao.findCriteriaByType(TreeType.ICD9.name());
    final Set<String> typeList = icd9List.stream().map(Criteria::getType).collect(Collectors.toSet());
    assertEquals(1, typeList.size());
  }

  @Test
  public void findCriteriaByTypeForCodeOrName() throws Exception {
    //match on code
    List<Criteria> labs = criteriaDao.findCriteriaByTypeForCodeOrName(TreeType.MEAS.name(), "LP123", null);
    assertEquals(1, labs.size());
    assertEquals(labCriteria, labs.get(0));

    //match on name
    labs = criteriaDao.findCriteriaByTypeForCodeOrName(TreeType.MEAS.name(), "Mysearch", null);
    assertEquals(1, labs.size());
    assertEquals(labCriteria, labs.get(0));

    List<Criteria> cpts = criteriaDao.findCriteriaByTypeForCodeOrName(TreeType.CPT.name(), "zzz", 1L);
    assertEquals(1, cpts.size());
    assertEquals(cptCriteria2, cpts.get(0));

    cpts = criteriaDao.findCriteriaByTypeForCodeOrName(TreeType.CPT.name(), "zzz", null);
    assertEquals(2, cpts.size());
    assertEquals(cptCriteria2, cpts.get(0));
    assertEquals(cptCriteria1, cpts.get(1));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeOrderByIdAsc() throws Exception {
    final List<Criteria> demoList = criteriaDao.findCriteriaByTypeAndSubtypeOrderByIdAsc(TreeType.DEMO.name(), SUBTYPE_RACE);
    assertEquals(3, demoList.size());
    assertEquals(parentDemo, demoList.get(0));
    assertEquals(demoCriteria1, demoList.get(1));
    assertEquals(demoCriteria1a, demoList.get(2));
  }

  @Test
  public void findDrugBrandOrIngredientByName() throws Exception {
    List<Criteria> drugList = criteriaDao.findDrugBrandOrIngredientByName("ETAM", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("ACE", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("A", 1L);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("A", 3L);
    assertEquals(2, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaBrand, drugList.get(1));

    drugList = criteriaDao.findDrugBrandOrIngredientByName("BL", null);
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
    final List<String> icd9DomainList = criteriaDao.findCriteriaByTypeAndCode(TreeType.ICD9.name(), "003");

    assertEquals(1, icd9DomainList.size());
    assertEquals("Condition", icd9DomainList.get(0));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeAndCode() throws Exception {
    final List<String> icd10DomainList = criteriaDao.findCriteriaByTypeAndSubtypeAndCode(TreeType.ICD10.name(), SUBTYPE_ICD10PCS, "003");

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
                                  String path) {
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
      .path(path);
  }

}
