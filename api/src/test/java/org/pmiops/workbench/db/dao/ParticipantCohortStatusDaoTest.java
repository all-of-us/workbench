package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import({FakeClockConfiguration.class, TestJpaConfig.class, CommonConfig.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringJUnitConfig
public class ParticipantCohortStatusDaoTest {
  private static final Long COHORT_REVIEW_ID = 1L;
  private static final Date birthDate = new Date(System.currentTimeMillis());
  private static final int PAGE = 0;
  private static final int PAGE_SIZE = 25;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void onSetup() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrDbName("");
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    DbParticipantCohortStatus status1 =
        new DbParticipantCohortStatus()
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.INCLUDED))
            .participantKey(
                new DbParticipantCohortStatusKey()
                    .cohortReviewId(COHORT_REVIEW_ID)
                    .participantId(1))
            .genderConceptId(8507L)
            .birthDate(birthDate)
            .raceConceptId(8515L)
            .ethnicityConceptId(38003564L)
            .sexAtBirthConceptId(8507L)
            .deceased(false);
    participantCohortStatusDao.save(status1);

    DbParticipantCohortStatus status2 =
        new DbParticipantCohortStatus()
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.EXCLUDED))
            .participantKey(
                new DbParticipantCohortStatusKey()
                    .cohortReviewId(COHORT_REVIEW_ID)
                    .participantId(2))
            .genderConceptId(8507L)
            .birthDate(birthDate)
            .raceConceptId(8515L)
            .ethnicityConceptId(38003564L)
            .sexAtBirthConceptId(8507L)
            .deceased(false);
    participantCohortStatusDao.save(status2);
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
    assertThat(actualParticipant).isEqualTo(participant1);
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
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.INCLUDED))
            .birthDate(new Date(System.currentTimeMillis()))
            .ethnicityConceptId(1L)
            .genderConceptId(1L)
            .raceConceptId(1L)
            .deceased(false);
    DbParticipantCohortStatus pcs2 =
        new DbParticipantCohortStatus()
            .participantKey(key2)
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.EXCLUDED))
            .birthDate(new Date(System.currentTimeMillis()))
            .ethnicityConceptId(1L)
            .genderConceptId(1L)
            .raceConceptId(1L)
            .deceased(false);

    participantCohortStatusDao.saveParticipantCohortStatusesCustom(ImmutableList.of(pcs1, pcs2));

    String sql = "select count(*) from participant_cohort_status where cohort_review_id = ?";
    final Object[] sqlParams = {key1.getCohortReviewId()};
    final Integer expectedCount = new Integer("2");

    assertThat(jdbcTemplate.queryForObject(sql, sqlParams, Integer.class)).isEqualTo(expectedCount);
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

    assertThat(results.size()).isEqualTo(2);
  }

  @Test
  public void findByParticipantKeyCohortReviewId() {
    List<DbParticipantCohortStatus> results =
        participantCohortStatusDao.findByParticipantKey_CohortReviewId(COHORT_REVIEW_ID);

    assertThat(results.size()).isEqualTo(2);
  }

  @Test
  public void findByParticipantKeyCohortReviewIdList() {
    List<DbParticipantCohortStatus> results =
        participantCohortStatusDao.findByParticipantKey_CohortReviewId(COHORT_REVIEW_ID);
    List<Long> participantIds =
        results.stream()
            .map(pcs -> pcs.getParticipantKey().getParticipantId())
            .collect(Collectors.toList());

    assertThat(participantIds.size()).isEqualTo(2);
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

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(participant1);
    assertThat(results.get(1)).isEqualTo(participant2);
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
            .values(ImmutableList.of("1")));
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of(CohortStatus.INCLUDED.toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of(new Date(System.currentTimeMillis()).toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.GENDER)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of("8507")));
    filters.add(
        new Filter()
            .property(FilterColumns.RACE)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of("8515")));
    filters.add(
        new Filter()
            .property(FilterColumns.ETHNICITY)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of("38003564")));
    pageRequest.filters(filters);
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(1);

    DbParticipantCohortStatus expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertThat(results.get(0)).isEqualTo(expectedPCS);
  }

  @Test
  public void findAllParticipantIdBetween() {
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
            .operator(Operator.BETWEEN)
            .values(ImmutableList.of("1", "2")));
    pageRequest.setFilters(filters);

    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(expectedPCS1);
    assertThat(results.get(1)).isEqualTo(expectedPCS2);
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
            .values(ImmutableList.of("1", "2")));
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.IN)
            .values(
                ImmutableList.of(
                    CohortStatus.INCLUDED.toString(), CohortStatus.EXCLUDED.toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.IN)
            .values(ImmutableList.of(new Date(System.currentTimeMillis()).toString())));
    filters.add(
        new Filter()
            .property(FilterColumns.GENDER)
            .operator(Operator.IN)
            .values(ImmutableList.of("8507", "8532")));
    filters.add(
        new Filter()
            .property(FilterColumns.RACE)
            .operator(Operator.IN)
            .values(ImmutableList.of("8515", "8527")));
    filters.add(
        new Filter()
            .property(FilterColumns.ETHNICITY)
            .operator(Operator.IN)
            .values(ImmutableList.of("38003564", "38003563")));
    pageRequest.filters(filters);
    List<DbParticipantCohortStatus> results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(expectedPCS1);
    assertThat(results.get(1)).isEqualTo(expectedPCS2);
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

    assertThat(results.size()).isEqualTo(1);

    DbParticipantCohortStatus expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
            CohortStatus.INCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertThat(results.get(0)).isEqualTo(expectedPCS);

    pageRequest =
        new PageRequest()
            .page(1)
            .pageSize(1)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(1);

    expectedPCS =
        createExpectedPCS(
            new DbParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
            CohortStatus.EXCLUDED);
    expectedPCS.setBirthDate(results.get(0).getBirthDate());

    assertThat(results.get(0)).isEqualTo(expectedPCS);
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

    assertThat(results).isEqualTo(2L);
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

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(expectedPCS1);
    assertThat(results.get(1)).isEqualTo(expectedPCS2);

    pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.DESC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(expectedPCS1);
    assertThat(results.get(1)).isEqualTo(expectedPCS2);
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

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(0)).isEqualTo(expectedPCS1);
    assertThat(results.get(1)).isEqualTo(expectedPCS2);

    pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(2)
            .sortOrder(SortOrder.DESC)
            .sortColumn(FilterColumns.STATUS.toString());
    results = participantCohortStatusDao.findAll(1L, pageRequest);

    assertThat(results.size()).isEqualTo(2);

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

    assertThat(results.get(1)).isEqualTo(expectedPCS2);
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
            .values(ImmutableList.of("z")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest, "Bad Request: Problems parsing PARTICIPANTID: For input string: \"z\"");

    filters.clear();
    filters.add(
        new Filter()
            .property(FilterColumns.STATUS)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of("z")));
    pageRequest.filters(filters);
    assertBadRequest(
        pageRequest,
        "Bad Request: Problems parsing STATUS: No enum constant org.pmiops.workbench.model.CohortStatus.z");

    filters.clear();
    filters.add(
        new Filter()
            .property(FilterColumns.BIRTHDATE)
            .operator(Operator.EQUAL)
            .values(ImmutableList.of("z")));
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
            .values(ImmutableList.of("1", "2")));
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
      assertThat(e.getMessage()).isEqualTo(expectedException);
    }
  }

  private DbParticipantCohortStatus createExpectedPCS(
      DbParticipantCohortStatusKey key, CohortStatus status) {
    return new DbParticipantCohortStatus()
        .participantKey(key)
        .status(DbStorageEnums.cohortStatusToStorage(status))
        .ethnicityConceptId(38003564L)
        .genderConceptId(8507L)
        .raceConceptId(8515L)
        .sexAtBirthConceptId(8507L);
  }
}
