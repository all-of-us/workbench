package org.pmiops.workbench.cohortbuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class CriteriaLookupUtilTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  private CriteriaLookupUtil lookupUtil;

  @Before
  public void setUp() {
    lookupUtil = new CriteriaLookupUtil(cbCriteriaDao);
  }

  private void saveCriteriaWithPath(String path, DbCriteria criteria) {
    cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.setPath(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    cbCriteriaDao.save(criteria);
  }

  @Test
  public void buildCriteriaLookupMapNoSearchParametersException() {
    SearchRequest searchRequest =
        new SearchRequest().addIncludesItem(new SearchGroup().addItemsItem(new SearchGroupItem()));
    try {
      lookupUtil.buildCriteriaLookupMap(searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: search parameters are empty.", bre.getMessage());
    }
  }

  @Test
  public void buildCriteriaLookupMapDrugCriteria_ATC() {
    DbCriteria drugNode1 =
        DbCriteria.builder()
            .addParentId(99999)
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.ATC.toString())
            .addConceptId("21600002")
            .addGroup(true)
            .addSelectable(true)
            .build();
    saveCriteriaWithPath("0", drugNode1);
    DbCriteria drugNode2 =
        DbCriteria.builder()
            .addParentId(drugNode1.getId())
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.RXNORM.toString())
            .addConceptId("19069022")
            .addGroup(false)
            .addSelectable(true)
            .build();
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);
    DbCriteria drugNode3 =
        DbCriteria.builder()
            .addParentId(drugNode1.getId())
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.RXNORM.toString())
            .addConceptId("1036094")
            .addGroup(false)
            .addSelectable(true)
            .build();
    saveCriteriaWithPath(drugNode1.getPath(), drugNode3);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1036094, 1036094)");

    List<Long> childConceptIds = ImmutableList.of(19069022L, 1036094L);
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(true)
            .ancestorData(true)
            .conceptId(21600002L);
    SearchRequest searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void buildCriteriaLookupMapDrugCriteria_RXNORM() {
    DbCriteria drugNode1 =
        DbCriteria.builder()
            .addParentId(99999)
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.ATC.toString())
            .addConceptId("21600002")
            .addGroup(true)
            .addSelectable(true)
            .build();
    saveCriteriaWithPath("0", drugNode1);
    DbCriteria drugNode2 =
        DbCriteria.builder()
            .addParentId(drugNode1.getId())
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.RXNORM.toString())
            .addConceptId("19069022")
            .addGroup(false)
            .addSelectable(true)
            .build();
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 1666666)");

    List<Long> childConceptIds = ImmutableList.of(1666666L);
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .group(true)
            .ancestorData(true)
            .conceptId(19069022L);
    SearchRequest searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void buildCriteriaLookupMapPPICriteria() {
    DbCriteria surveyNode =
        DbCriteria.builder()
            .addParentId(0)
            .addDomainId(DomainType.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.SURVEY.toString())
            .addGroup(true)
            .addSelectable(true)
            .addStandard(false)
            .addConceptId("22")
            .build();
    saveCriteriaWithPath("0", surveyNode);
    DbCriteria questionNode =
        DbCriteria.builder()
            .addParentId(surveyNode.getId())
            .addDomainId(DomainType.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.QUESTION.toString())
            .addGroup(true)
            .addSelectable(true)
            .addStandard(false)
            .addName("In what country were you born?")
            .addConceptId("1586135")
            .addSynonyms("[SURVEY_rank1]")
            .build();
    saveCriteriaWithPath(surveyNode.getPath(), questionNode);
    DbCriteria answerNode =
        DbCriteria.builder()
            .addParentId(questionNode.getId())
            .addDomainId(DomainType.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.ANSWER.toString())
            .addGroup(false)
            .addSelectable(true)
            .addStandard(false)
            .addName("USA")
            .addConceptId("5")
            .addSynonyms("[SURVEY_rank1]")
            .build();
    saveCriteriaWithPath(questionNode.getPath(), answerNode);

    // Survey search
    List<Long> childConceptIds = ImmutableList.of(5L, 1586135L);
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.SURVEY.toString())
            .group(true)
            .standard(false)
            .ancestorData(false)
            .conceptId(22L);
    SearchRequest searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));

    // Question search
    childConceptIds = ImmutableList.of(5L);
    searchParameter =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .group(true)
            .standard(false)
            .ancestorData(false)
            .conceptId(1586135L);
    searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));

    // Answer search
    searchParameter =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .group(false)
            .standard(false)
            .ancestorData(false)
            .conceptId(5L);
    searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertTrue(lookupUtil.buildCriteriaLookupMap(searchRequest).isEmpty());
  }

  @Test
  public void buildCriteriaLookupMapConditionSnomedCriteria() {
    DbCriteria snomedParent1 =
        DbCriteria.builder()
            .addParentId(0)
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addGroup(true)
            .addSelectable(true)
            .addStandard(true)
            .addConceptId("132277")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath("0", snomedParent1);
    DbCriteria snomedParent2 =
        DbCriteria.builder()
            .addParentId(snomedParent1.getId())
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addGroup(true)
            .addSelectable(true)
            .addStandard(true)
            .addConceptId("27835")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath(snomedParent1.getPath(), snomedParent2);
    DbCriteria snomedChild =
        DbCriteria.builder()
            .addParentId(snomedParent2.getId())
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addGroup(false)
            .addSelectable(true)
            .addStandard(true)
            .addConceptId("4099351")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath(snomedParent2.getPath(), snomedChild);

    List<Long> childConceptIds = ImmutableList.of(27835L, 4099351L);
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(true)
            .standard(true)
            .ancestorData(false)
            .conceptId(132277L);
    SearchRequest searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));
  }

  @Test
  public void buildCriteriaLookupMapConditionICD9Criteria() {
    DbCriteria icd9Parent =
        DbCriteria.builder()
            .addParentId(0)
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addGroup(true)
            .addSelectable(true)
            .addStandard(false)
            .addConceptId("44829696")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath("0", icd9Parent);
    DbCriteria icd9Child1 =
        DbCriteria.builder()
            .addParentId(icd9Parent.getId())
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addGroup(false)
            .addSelectable(true)
            .addStandard(false)
            .addConceptId("44829697")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child1);
    DbCriteria icd9Child2 =
        DbCriteria.builder()
            .addParentId(icd9Parent.getId())
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addGroup(false)
            .addSelectable(true)
            .addStandard(false)
            .addConceptId("44835564")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child2);

    List<Long> childConceptIds = ImmutableList.of(44829697L, 44835564L);
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(true)
            .standard(false)
            .ancestorData(false)
            .conceptId(44829696L);
    SearchRequest searchRequest =
        new SearchRequest()
            .addIncludesItem(
                new SearchGroup()
                    .addItemsItem(new SearchGroupItem().addSearchParametersItem(searchParameter)));
    assertEquals(
        ImmutableMap.of(searchParameter, new HashSet<>(childConceptIds)),
        lookupUtil.buildCriteriaLookupMap(searchRequest));
  }
}
