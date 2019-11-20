package org.pmiops.workbench.cohortbuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
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
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class CriteriaLookupUtilTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  private CriteriaLookupUtil lookupUtil;

  @Before
  public void setUp() throws Exception {
    lookupUtil = new CriteriaLookupUtil(cbCriteriaDao);
  }

  private void saveCriteriaWithPath(String path, DbCriteria criteria) {
    cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.path(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    cbCriteriaDao.save(criteria);
  }

  @Test
  public void buildCriteriaLookupMapNoSearchParametersException() throws Exception {
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
  public void buildCriteriaLookupMapDrugCriteria_ATC() throws Exception {
    DbCriteria drugNode1 =
        new DbCriteria()
            .parentId(99999)
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .conceptId("21600002")
            .group(true)
            .selectable(true);
    saveCriteriaWithPath("0", drugNode1);
    DbCriteria drugNode2 =
        new DbCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("19069022")
            .group(false)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);
    DbCriteria drugNode3 =
        new DbCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("1036094")
            .group(false)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode3);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1036094, 1036094)");

    List<Long> childConceptIds = Arrays.asList(19069022L, 1036094L);
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
  public void buildCriteriaLookupMapDrugCriteria_RXNORM() throws Exception {
    DbCriteria drugNode1 =
        new DbCriteria()
            .parentId(99999)
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .conceptId("21600002")
            .group(true)
            .selectable(true);
    saveCriteriaWithPath("0", drugNode1);
    DbCriteria drugNode2 =
        new DbCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("19069022")
            .group(false)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 1666666)");

    List<Long> childConceptIds = Arrays.asList(1666666L);
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
  public void buildCriteriaLookupMapPPICriteria() throws Exception {
    DbCriteria surveyNode =
        new DbCriteria()
            .parentId(0)
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.SURVEY.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .conceptId("22");
    saveCriteriaWithPath("0", surveyNode);
    DbCriteria questionNode =
        new DbCriteria()
            .parentId(surveyNode.getId())
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.QUESTION.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .name("In what country were you born?")
            .conceptId("1586135")
            .synonyms("[SURVEY_rank1]");
    saveCriteriaWithPath(surveyNode.getPath(), questionNode);
    DbCriteria answerNode =
        new DbCriteria()
            .parentId(questionNode.getId())
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.ANSWER.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .name("USA")
            .conceptId("5")
            .synonyms("[SURVEY_rank1]");
    saveCriteriaWithPath(questionNode.getPath(), answerNode);

    // Survey search
    List<Long> childConceptIds = Arrays.asList(5L, 1586135L);
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
    childConceptIds = Arrays.asList(5L);
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
  public void buildCriteriaLookupMapConditionSnomedCriteria() throws Exception {
    DbCriteria snomedParent1 =
        new DbCriteria()
            .parentId(0)
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(true)
            .selectable(true)
            .standard(true)
            .conceptId("132277")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath("0", snomedParent1);
    DbCriteria snomedParent2 =
        new DbCriteria()
            .parentId(snomedParent1.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(true)
            .selectable(true)
            .standard(true)
            .conceptId("27835")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath(snomedParent1.getPath(), snomedParent2);
    DbCriteria snomedChild =
        new DbCriteria()
            .parentId(snomedParent2.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(false)
            .selectable(true)
            .standard(true)
            .conceptId("4099351")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath(snomedParent2.getPath(), snomedChild);

    List<Long> childConceptIds = Arrays.asList(27835L, 4099351L);
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
  public void buildCriteriaLookupMapConditionICD9Criteria() throws Exception {
    DbCriteria icd9Parent =
        new DbCriteria()
            .parentId(0)
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .conceptId("44829696")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath("0", icd9Parent);
    DbCriteria icd9Child1 =
        new DbCriteria()
            .parentId(icd9Parent.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .conceptId("44829697")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child1);
    DbCriteria icd9Child2 =
        new DbCriteria()
            .parentId(icd9Parent.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .conceptId("44835564")
            .synonyms("[CONDITION_rank1]");
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child2);

    List<Long> childConceptIds = Arrays.asList(44829697L, 44835564L);
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
