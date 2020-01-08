package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import({TestJpaConfig.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ParticipantCohortStatusDaoTest {
  private static Long COHORT_REVIEW_ID = 1L;
  private static Date birthDate = new Date(System.currentTimeMillis());
  private static int PAGE = 0;
  private static int PAGE_SIZE = 25;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;
  @Autowired private ConceptDao conceptDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Before
  public void onSetup() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrDbName("");
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    DbParticipantCohortStatus status1 =
        new DbParticipantCohortStatus()
            .statusEnum(CohortStatus.INCLUDED)
            .participantKey(
                new DbParticipantCohortStatusKey()
                    .cohortReviewId(COHORT_REVIEW_ID)
                    .participantId(1))
            .genderConceptId(8507L)
            .birthDate(birthDate)
            .raceConceptId(8515L)
            .ethnicityConceptId(38003564L)
            .deceased(false);
    participantCohortStatusDao.save(status1);

    DbParticipantCohortStatus status2 =
        new DbParticipantCohortStatus()
            .statusEnum(CohortStatus.EXCLUDED)
            .participantKey(
                new DbParticipantCohortStatusKey()
                    .cohortReviewId(COHORT_REVIEW_ID)
                    .participantId(2))
            .genderConceptId(8507L)
            .birthDate(birthDate)
            .raceConceptId(8515L)
            .ethnicityConceptId(38003564L)
            .deceased(false);
    participantCohortStatusDao.save(status2);

    DbConcept male =
        new DbConcept()
            .conceptId(8507)
            .conceptName("MALE")
            .domainId("3")
            .vocabularyId("Gender")
            .conceptClassId("1")
            .standardConcept("c")
            .conceptCode("c")
            .count(1)
            .prevalence(1);
    conceptDao.save(male);

    DbConcept race =
        new DbConcept()
            .conceptId(8515)
            .conceptName("Asian")
            .domainId("3")
            .vocabularyId("Race")
            .conceptClassId("1")
            .standardConcept("c")
            .conceptCode("c")
            .count(1)
            .prevalence(1);
    conceptDao.save(race);

    DbConcept ethnicity =
        new DbConcept()
            .conceptId(38003564)
            .conceptName("Not Hispanic or Latino")
            .domainId("3")
            .vocabularyId("Ethnicity")
            .conceptClassId("1")
            .standardConcept("c")
            .conceptCode("c")
            .count(1)
            .prevalence(1);
    conceptDao.save(ethnicity);
  }

  @Test
  public void findByParticipantKeyCohortReviewIdAndParticipantKeyParticipantId() {
    DbParticipantCohortStatus participant1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    DbParticipantCohortStatus actualParticipant =
        participantCohortStatusDao
            .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                COHORT_REVIEW_ID, participant1.getParticipantKey().getParticipantId());
    participant1.setBirthDate(actualParticipant.getBirthDate());
    assertEquals(participant1, actualParticipant);
  }

  @Test
  public void findAllSaveParticipantCohortStatuses() {
    DbParticipantCohortStatusKey key1 =
        new DbParticipantCohortStatusKey().cohortReviewId(2).participantId(3);
    DbParticipantCohortStatusKey key2 =
        new DbParticipantCohortStatusKey().cohortReviewId(2).participantId(4);
    DbParticipantCohortStatus pcs1 =
        new DbParticipantCohortStatus()
            .participantKey(key1)
            .statusEnum(CohortStatus.INCLUDED)
            .birthDate(new Date(System.currentTimeMillis()))
            .ethnicityConceptId(1L)
            .genderConceptId(1L)
            .raceConceptId(1L)
            .deceased(false);
    DbParticipantCohortStatus pcs2 =
        new DbParticipantCohortStatus()
            .participantKey(key2)
            .statusEnum(CohortStatus.EXCLUDED)
            .birthDate(new Date(System.currentTimeMillis()))
            .ethnicityConceptId(1L)
            .genderConceptId(1L)
            .raceConceptId(1L)
            .deceased(false);

    participantCohortStatusDao.saveParticipantCohortStatusesCustom(Arrays.asList(pcs1, pcs2));

    String sql = "select count(*) from participant_cohort_status where cohort_review_id = ?";
    final Object[] sqlParams = {key1.getCohortReviewId()};
    final Integer expectedCount = new Integer("2");

    assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
  }

  @Test
  public void findAllNoMatchingConcept() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<DbParticipantCohortStatus> results =
        participantCohortStatusDao.findAll(COHORT_REVIEW_ID, pageRequest);

    assertEquals(2, results.size());
  }

  @Test
  public void findAllNoSearchCriteria() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    DbParticipantCohortStatus participant1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    participant1.setBirthDate(results.get(0).getBirthDate());
    DbParticipantCohortStatus participant2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.EXCLUDED);
    participant2.setBirthDate(results.get(1).getBirthDate());

    assertEquals(participant1, results.get(0));
    assertEquals(participant2, results.get(1));
  }

  @Test
  public void findAllSearchCriteriaEqual() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<Filter> filters = new ArrayList<>();
    filters.add(
        new Filter()
            .property(FilterColumns.PARTICIPANTID)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("1")));
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.EQUAL)
            .values(Arrays.asList(CohortStatus.INCLUDED.toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.EQUAL)
            .values(Arrays.asList(new Date(System.currentTimeMillis()).toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.GENDER)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("8507")));
    filters.add(
        new Filter()
            .property(FilterColumns.RACE)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("8515")));
    filters.add(
        new Filter()
            .property(FilterColumns.ETHNICITY)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("38003564")));
    pageRequest.filters(filters);
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(1, results.size());

    DbParticipantCohortStatus expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertEquals(expectedPCS, results.get(0));
  }

  @Test
  public void findAllSearchCriteriaIn() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<Filter> filters = new ArrayList<>();
    filters.add(
        new Filter()
            .property(FilterColumns.PARTICIPANTID)
            .operator(Operator.IN)
            .values(Arrays.asList("1", "2")));
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.IN)
            .values(
                Arrays.asList(CohortStatus.INCLUDED.toString(), CohortStatus.EXCLUDED.toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.IN)
            .values(Arrays.asList(new Date(System.currentTimeMillis()).toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.GENDER)
            .operator(Operator.IN)
            .values(Arrays.asList("8507", "8532")));
    filters.add(
        new Filter()
            .property(FilterColumns.RACE)
            .operator(Operator.IN)
            .values(Arrays.asList("8515", "8527")));
    filters.add(
        new Filter()
            .property(FilterColumns.ETHNICITY)
            .operator(Operator.IN)
            .values(Arrays.asList("38003564", "38003563")));
    pageRequest.filters(filters);
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    DbParticipantCohortStatus expectedPCS1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS1.setBirthDate(results.get(0).getBirthDate());
    DbParticipantCohortStatus expectedPCS2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS2.setBirthDate(results.get(0).getBirthDate());

    assertEquals(expectedPCS1, results.get(0));
    assertEquals(expectedPCS2, results.get(1));
  }

  @Test
  public void findAllPaging() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(1)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(1, results.size());

    DbParticipantCohortStatus expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertEquals(expectedPCS, results.get(0));

    pageRequest =
        new PageRequest()
            .page(1)
            .pageSize(1)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(1, results.size());

    expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertEquals(expectedPCS, results.get(0));
  }

  @Test
  public void findCount() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(1)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    Long results = participantCohortStatusDao.findCount(1L, pageRequest);

    assertEquals(2L, results.longValue());
  }

  @Test
  public void findAllParticipantIdSorting() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    DbParticipantCohortStatus expectedPCS1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS1.setBirthDate(results.get(0).getBirthDate());
    DbParticipantCohortStatus expectedPCS2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS2.setBirthDate(results.get(1).getBirthDate());

    assertEquals(expectedPCS1, results.get(0));
    assertEquals(expectedPCS2, results.get(1));

    pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.DESC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    expectedPCS1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS1.setBirthDate(results.get(0).getBirthDate());
    expectedPCS2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS2.setBirthDate(results.get(1).getBirthDate());

    assertEquals(expectedPCS1, results.get(0));
    assertEquals(expectedPCS2, results.get(1));
  }

  @Test
  public void findAllStatusSorting() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.STATUS.toString());
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    DbParticipantCohortStatus expectedPCS1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS1.setBirthDate(results.get(0).getBirthDate());
    DbParticipantCohortStatus expectedPCS2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS2.setBirthDate(results.get(1).getBirthDate());

    assertEquals(expectedPCS1, results.get(0));
    assertEquals(expectedPCS2, results.get(1));

    pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.DESC)
            .sortColumn(FilterColumns.STATUS.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertEquals(2, results.size());

    expectedPCS1 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS1.setBirthDate(results.get(0).getBirthDate());
    expectedPCS2 =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS2.setBirthDate(results.get(1).getBirthDate());

    assertEquals(expectedPCS1, results.get(0));
    assertEquals(expectedPCS2, results.get(1));
  }

  @Test
  public void findAllBadFilterTypes() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<Filter> filters = new ArrayList<>();

    filters.add(
        new Filter()
            .property(FilterColumns.PARTICIPANTID)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("z")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest, "Bad Request: Problems parsing PARTICIPANTID: For input string: \"z\"");

    filters.clear();
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("z")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest,
        "Bad Request: Problems parsing STATUS: No enum constant org.pmiops.workbench.model.CohortStatus.z");

    filters.clear();
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("z")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest, "Bad Request: Problems parsing BIRTHDATE: Unparseable date: \"z\"");
  }

  @Test
  public void findAllBadFilterValuesSize() {
    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    List<Filter> filters = new ArrayList<>();

    filters.add(
        new Filter()
            .property(FilterColumns.PARTICIPANTID)
            .operator(Operator.EQUAL)
            .values(Arrays.asList("1", "2")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest,
        "Bad Request: property PARTICIPANTID using operator EQUAL must have a single value.");

    filters.clear();
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.EQUAL)
            .values(new ArrayList<>()));
    pageRequest.filters(filters);
    assertBadRequest(pageRequest, "Bad Request: property STATUS is empty.");
  }

  private void assertBadRequest(PageRequest pageRequest, String expectedException) {
    try {
      participantCohortStatusDao.findAll(1L, pageRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException e) {
      assertEquals(expectedException, e.getMessage());
    }
  }

  private DbParticipantCohortStatus createExpectedPCS(
      DbParticipantCohortStatusKey key, CohortStatus status) {
    return new DbParticipantCohortStatus()
        .participantKey(key)
        .statusEnum(status)
        .ethnicityConceptId(38003564L)
        .genderConceptId(8507L)
        .raceConceptId(8515L);
  }
}
