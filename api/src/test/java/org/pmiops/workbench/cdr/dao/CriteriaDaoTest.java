package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Before
  public void setUp() {
    jdbcTemplate.execute("delete from criteria");
  }

  @Test
  public void findCriteriaByTypeAndId() throws Exception {
    Criteria icd9Criteria1 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "002", "blah chol", 0, false, false, null);
    criteriaDao.save(icd9Criteria1);
    Criteria criteria = criteriaDao.findCriteriaByTypeAndConceptIdAndSelectable(icd9Criteria1.getType(), icd9Criteria1.getConceptId(), false);
    assertEquals(icd9Criteria1, criteria);
  }

  @Test
  public void findCriteriaByParentId() throws Exception {
    Criteria icd9Criteria1 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "002", "blah chol", 0, false, true, null);
    Criteria icd9Criteria2 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "001", "chol blah", 0, true, true, null).conceptId("123").synonyms("001");
    Criteria icd10Criteria1 = createCriteria(TreeType.ICD10.name(), TreeSubType.CM.name(), "002", "icd10 test 1", 0, false, true, null);
    Criteria icd10Criteria2 = createCriteria(TreeType.ICD10.name(), TreeSubType.CM.name(), "001", "icd10 test 2", 0, false, true, null);
    Criteria cptCriteria1 = createCriteria(TreeType.CPT.name(), TreeSubType.CPT4.name(), "0039T", "zzzcptzzz", 0, false, true, null);
    Criteria cptCriteria2 = createCriteria(TreeType.CPT.name(), TreeSubType.CPT4.name(), "0001T", "zzzCPTxxx", 0, false, true, null);
    Criteria pmCriteria = createCriteria(TreeType.PM.name(), TreeSubType.BP.name(), "1", "Hypotensive (Systolic <= 90 / Diastolic <= 60)", 0, false, true, "1.2.3.4");
    criteriaDao.save(icd9Criteria1);
    criteriaDao.save(icd9Criteria2);
    criteriaDao.save(icd10Criteria1);
    criteriaDao.save(icd10Criteria2);
    criteriaDao.save(cptCriteria1);
    criteriaDao.save(cptCriteria2);
    criteriaDao.save(pmCriteria);

    final List<Criteria> icd9List =
      criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.ICD9.name(), 0L);
    assertEquals(icd9Criteria1, icd9List.get(0));
    assertEquals(icd9Criteria2, icd9List.get(1));

    final List<Criteria> icd10List =
      criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.ICD10.name(), 0L);
    assertEquals(icd10Criteria1, icd10List.get(0));
    assertEquals(icd10Criteria2, icd10List.get(1));

    final List<Criteria> cptList =
      criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.CPT.name(), 0L);
    assertEquals(cptCriteria1, cptList.get(0));
    assertEquals(cptCriteria2, cptList.get(1));

    final List<Criteria> pmList =
      criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(TreeType.PM.name(), 0L);
    assertEquals(pmCriteria, pmList.get(0));
  }

  @Test
  public void getPPICriteriaParent() throws Exception {
    Criteria ppiCriteriaParent = createCriteria(TreeType.PPI.name(), TreeSubType.BASICS.name(), "", "Race", 3272493, true, false, "3272493").conceptId("1586140");
    criteriaDao.save(ppiCriteriaParent);
    Criteria ppiCriteriaChild = createCriteria(TreeType.PPI.name(), TreeSubType.BASICS.name(), "1586146", "White Alone", ppiCriteriaParent.getId(), false, true, ppiCriteriaParent.getPath() + "." + ppiCriteriaParent.getId()).conceptId("1586140");
    criteriaDao.save(ppiCriteriaChild);
    Criteria criteria = criteriaDao.findCriteriaByTypeAndConceptIdAndSelectable(ppiCriteriaChild.getType(), ppiCriteriaChild.getConceptId(), false);
    assertEquals(ppiCriteriaParent, criteria);
  }

  @Test
  public void findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc() throws Exception {
    Criteria drugCriteriaIngredient = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "1", "ACETAMIN", 0, false, true, "1.2.3.4").conceptId("1");
    Criteria drugCriteriaIngredient1 = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "2", "MIN1", 0, false, true, "1.2.3.4").conceptId("2");
    criteriaDao.save(drugCriteriaIngredient);
    criteriaDao.save(drugCriteriaIngredient1);

    final List<Criteria> drugList =
      criteriaDao.findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0L);
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaIngredient1, drugList.get(1));
  }

  @Test
  public void findCriteriaChildrenByTypeAndParentId() throws Exception {
    Criteria drugCriteriaIngredient = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "1", "ACETAMIN", 0, false, true, "1.2.3.4").conceptId("1");
    Criteria drugCriteriaIngredient1 = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "2", "MIN1", 0, false, true, "1.2.3.4").conceptId("2");
    criteriaDao.save(drugCriteriaIngredient);
    criteriaDao.save(drugCriteriaIngredient1);

    final List<Criteria> drugList =
      criteriaDao.findCriteriaChildrenByTypeAndParentId(TreeType.DRUG.name(), 2L);
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaIngredient1, drugList.get(1));
  }

  @Test
  public void findCriteriaByType() throws Exception {
    Criteria icd9Criteria1 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "002", "blah chol", 0, false, true, null);
    criteriaDao.save(icd9Criteria1);

    final List<Criteria> icd9List =
      criteriaDao.findCriteriaByType(TreeType.ICD9.name());
    final Set<String> typeList = icd9List.stream().map(Criteria::getType).collect(Collectors.toSet());
    assertEquals(1, typeList.size());
  }

  @Test
  public void findCriteriaByTypeForCodeOrName() throws Exception {
    Criteria labCriteria = createCriteria(TreeType.MEAS.name(), TreeSubType.LAB.name(), "xxx", "mysearchname", 0, false, false, "0.12345").conceptId("123").synonyms("LP123");
    Criteria labCriteria1 = createCriteria(TreeType.MEAS.name(), TreeSubType.LAB.name(), "LP1234", "mysearchname", 0, false, false, "0.12345").conceptId("123");
    criteriaDao.save(labCriteria);
    criteriaDao.save(labCriteria1);

    //match on code
    List<Criteria> labs = criteriaDao.findCriteriaByTypeForCodeOrName(TreeType.MEAS.name(), "LP123", "LP1234", new PageRequest(0, 10));
    assertEquals(2, labs.size());
    assertEquals(labCriteria1, labs.get(0));
    assertEquals(labCriteria, labs.get(1));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeForCodeOrName() throws Exception {
    Criteria icd9Criteria1 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "002", "blah chol", 0, false, true, null);
    Criteria icd9Criteria2 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "001", "chol blah", 0, true, true, null).conceptId("123").synonyms("001");
    criteriaDao.save(icd9Criteria1);
    criteriaDao.save(icd9Criteria2);

    //match on code
    List<Criteria> conditions =
      criteriaDao.findCriteriaByTypeAndSubtypeForCodeOrName(TreeType.ICD9.name(), TreeSubType.CM.name(),"001", "001", new PageRequest(0, 10));
    assertEquals(1, conditions.size());
    assertEquals(icd9Criteria2, conditions.get(0));

    conditions =
      criteriaDao.findCriteriaByTypeAndSubtypeForCodeOrName(TreeType.ICD9.name(), TreeSubType.CM.name(),"002", "002", new PageRequest(0, 10));
    assertEquals(1, conditions.size());
    assertEquals(icd9Criteria1, conditions.get(0));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeForName() throws Exception {
    //match on code
    Criteria icd9Criteria2 = createCriteria(TreeType.ICD9.name(), TreeSubType.CM.name(), "001", "chol blah", 0, true, true, null).conceptId("123").synonyms("001");
    criteriaDao.save(icd9Criteria2);
    List<Criteria> conditions =
      criteriaDao.findCriteriaByTypeAndSubtypeForName(TreeType.ICD9.name(), TreeSubType.CM.name(),"001", new PageRequest(0, 10));
    assertEquals(1, conditions.size());
    assertEquals(icd9Criteria2, conditions.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTerm() throws Exception {
    //match on code
    Criteria criteria = new Criteria()
      .code("001")
      .count("10")
      .conceptId("123")
      .domainId(DomainType.MEASUREMENT.toString())
      .group(true)
      .selectable(true)
      .name("chol blah")
      .parentId(0)
      .type(CriteriaType.CPT4.toString())
      .attribute(Boolean.FALSE)
      .standard(true)
      .synonyms("001");
    criteriaDao.save(criteria);
    List<Criteria> conditions =
      criteriaDao.findCriteriaByDomainAndSearchTerm(DomainType.CONDITION.toString(), true,"001", new PageRequest(0, 10));
    assertEquals(1, conditions.size());
    assertEquals(criteria, conditions.get(0));
  }

  @Test
  public void findCriteriaByTypeAndSubtypeOrderByIdAsc() throws Exception {
    Criteria parentDemo = createCriteria(TreeType.DEMO.name(), TreeSubType.RACE.name(), "Race/Ethnicity", "Race/Ethnicity", 0, true, true, null);
    Criteria demoCriteria1 = createCriteria(TreeType.DEMO.name(), TreeSubType.RACE.name(), "AF", "African", parentDemo.getId(), false, true, null);
    Criteria demoCriteria1a = createCriteria(TreeType.DEMO.name(), TreeSubType.RACE.name(), "B", "African American", parentDemo.getId(), false, true, null);
    criteriaDao.save(parentDemo);
    criteriaDao.save(demoCriteria1);
    criteriaDao.save(demoCriteria1a);

    final List<Criteria> demoList =
      criteriaDao.findCriteriaByTypeAndSubtypeOrderByIdAsc(TreeType.DEMO.name(), TreeSubType.RACE.name());
    assertEquals(3, demoList.size());
    assertEquals(parentDemo, demoList.get(0));
    assertEquals(demoCriteria1, demoList.get(1));
    assertEquals(demoCriteria1a, demoList.get(2));
  }

  @Test
  public void findDrugBrandOrIngredientByName() throws Exception {
    Criteria drugCriteriaIngredient = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "1", "ACETAMIN", 0, false, true, "1.2.3.4").conceptId("1");
    Criteria drugCriteriaIngredient1 = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "2", "MIN1", 0, false, true, "1.2.3.4").conceptId("2");
    Criteria drugCriteriaBrand = createCriteria(TreeType.DRUG.name(), TreeSubType.BRAND.name(), "3", "BLAH", 0, false, true, "");
    criteriaDao.save(drugCriteriaIngredient);
    criteriaDao.save(drugCriteriaIngredient1);
    criteriaDao.save(drugCriteriaBrand);

    List<Criteria> drugList = criteriaDao.findDrugBrandOrIngredientByValue("ETAM", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByValue("ACE", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByValue("A", 1L);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByValue("A", 3L);
    assertEquals(2, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaBrand, drugList.get(1));

    drugList = criteriaDao.findDrugBrandOrIngredientByValue("BL", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaBrand, drugList.get(0));

    drugList = criteriaDao.findDrugBrandOrIngredientByValue("2", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient1, drugList.get(0));
  }

  @Test
  public void findDrugIngredientsByConceptId() throws Exception {
    jdbcTemplate.execute("create table criteria_relationship (concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    Criteria drugCriteriaIngredient = createCriteria(TreeType.DRUG.name(), TreeSubType.ATC.name(), "1", "ACETAMIN", 0, false, true, "1.2.3.4").conceptId("1");
    criteriaDao.save(drugCriteriaIngredient);
    conceptDao.save(new Concept().conceptId(1L).conceptClassId("Ingredient"));

    List<Criteria> drugList = criteriaDao.findDrugIngredientByConceptId(12345L);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    jdbcTemplate.execute("drop table criteria_relationship");
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
      .conceptId(parentId == 0 ? null : "1000")
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
