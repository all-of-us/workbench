package org.pmiops.workbench.cohortreview;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AnnotationQueryBuilderTest {

  @TestConfiguration
  @Import({FakeClockConfiguration.class, AnnotationQueryBuilder.class})
  static class Configuration {}

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final long INCLUDED_PERSON_ID = 1L;
  private static final long EXCLUDED_PERSON_ID = 2L;

  private static final ImmutableList<CohortStatus> INCLUDED_ONLY =
      ImmutableList.of(CohortStatus.INCLUDED);

  private static final ImmutableList<CohortStatus> ALL_STATUSES =
      ImmutableList.of(
          CohortStatus.INCLUDED, CohortStatus.EXCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW);

  @Autowired private AnnotationQueryBuilder annotationQueryBuilder;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CohortDao cohortDao;

  @Autowired private CohortReviewDao cohortReviewDao;

  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  private DbCohortReview cohortReview;
  private DbCohortAnnotationDefinition integerAnnotation;
  private DbCohortAnnotationDefinition stringAnnotation;
  private DbCohortAnnotationDefinition booleanAnnotation;
  private DbCohortAnnotationDefinition dateAnnotation;
  private DbCohortAnnotationDefinition enumAnnotation;
  private Map<String, DbCohortAnnotationEnumValue> enumValueMap;
  private ImmutableMap<String, Object> expectedResult1;
  private ImmutableMap<String, Object> expectedResult2;
  private List<String> allColumns;

  @BeforeEach
  public void setUp() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    DbWorkspace workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspaceDao.save(workspace);

    DbCohort cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setName("males");
    cohort.setType("AOU");
    cohort.setCriteria("blah");
    cohortDao.save(cohort);

    cohortReview = new DbCohortReview();
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setCohortId(cohort.getCohortId());
    cohortReview.setMatchedParticipantCount(3);
    cohortReview.setReviewedCount(2);
    cohortReview.setReviewSize(3);
    cohortReviewDao.save(cohortReview);

    integerAnnotation =
        cohortAnnotationDefinitionDao.save(
            makeAnnotationDefinition(
                cohort.getCohortId(), "integer annotation", AnnotationType.INTEGER));
    stringAnnotation =
        cohortAnnotationDefinitionDao.save(
            makeAnnotationDefinition(
                cohort.getCohortId(), "string annotation", AnnotationType.STRING));
    booleanAnnotation =
        cohortAnnotationDefinitionDao.save(
            makeAnnotationDefinition(
                cohort.getCohortId(), "boolean annotation", AnnotationType.BOOLEAN));
    dateAnnotation =
        cohortAnnotationDefinitionDao.save(
            makeAnnotationDefinition(cohort.getCohortId(), "date annotation", AnnotationType.DATE));
    enumAnnotation =
        cohortAnnotationDefinitionDao.save(
            makeAnnotationDefinition(
                cohort.getCohortId(), "enum annotation", AnnotationType.ENUM, "zebra", "aardvark"));
    enumValueMap =
        Maps.uniqueIndex(enumAnnotation.getEnumValues(), DbCohortAnnotationEnumValue::getName);

    expectedResult1 =
        ImmutableMap.<String, Object>builder()
            .put("person_id", INCLUDED_PERSON_ID)
            .put("review_status", "INCLUDED")
            .put("integer annotation", 123)
            .put("string annotation", "foo")
            .put("boolean annotation", true)
            .put("date annotation", "2017-02-14")
            .put("enum annotation", "zebra")
            .build();
    expectedResult2 =
        ImmutableMap.<String, Object>builder()
            .put("person_id", EXCLUDED_PERSON_ID)
            .put("review_status", "EXCLUDED")
            .put("integer annotation", 456)
            .put("boolean annotation", false)
            .put("date annotation", "2017-02-15")
            .put("enum annotation", "aardvark")
            .build();
    allColumns = ImmutableList.copyOf(expectedResult1.keySet());
  }

  private DbCohortAnnotationDefinition makeAnnotationDefinition(
      long cohortId, String columnName, AnnotationType annotationType, String... enumValues) {
    DbCohortAnnotationDefinition cohortAnnotationDefinition = new DbCohortAnnotationDefinition();
    cohortAnnotationDefinition.setAnnotationTypeEnum(annotationType);
    cohortAnnotationDefinition.setCohortId(cohortId);
    cohortAnnotationDefinition.setColumnName(columnName);
    if (enumValues.length > 0) {
      for (int i = 0; i < enumValues.length; i++) {
        DbCohortAnnotationEnumValue enumValue = new DbCohortAnnotationEnumValue();
        enumValue.setOrder(i);
        enumValue.setName(enumValues[i]);
        cohortAnnotationDefinition.getEnumValues().add(enumValue);
      }
    }
    return cohortAnnotationDefinition;
  }

  private DbParticipantCohortStatus makeStatus(
      long cohortReviewId, long participantId, CohortStatus status) {
    DbParticipantCohortStatusKey key = new DbParticipantCohortStatusKey();
    key.setCohortReviewId(cohortReviewId);
    key.setParticipantId(participantId);
    DbParticipantCohortStatus result = new DbParticipantCohortStatus();
    result.setStatusEnum(status);
    result.setParticipantKey(key);
    return result;
  }

  @Test
  public void testQueryEmptyReview() {
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, INCLUDED_ONLY, new AnnotationQuery(), 10, 0),
        allColumns);
  }

  @Test
  public void testQueryOneIncluded() {
    saveReviewStatuses();
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, INCLUDED_ONLY, new AnnotationQuery(), 10, 0),
        allColumns,
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));
  }

  @Test
  public void testQueryAllStatuses() {
    saveReviewStatuses();
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, new AnnotationQuery(), 10, 0),
        allColumns,
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"),
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"));
  }

  @Test
  public void testQueryAllStatusesReviewStatusOrder() {
    saveReviewStatuses();
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("review_status"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));
  }

  @Test
  public void testQueryAllStatusesReviewPersonIdOrderDescending() {
    saveReviewStatuses();
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(person_id)"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));
  }

  @Test
  public void testQueryIncludedWithAnnotations() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    ImmutableMap<String, Object> expectedResult =
        ImmutableMap.<String, Object>builder()
            .put("person_id", INCLUDED_PERSON_ID)
            .put("review_status", "INCLUDED")
            .put("integer annotation", 123)
            .put("string annotation", "foo")
            .put("boolean annotation", true)
            .put("date annotation", "2017-02-14")
            .put("enum annotation", "zebra")
            .build();
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, INCLUDED_ONLY, new AnnotationQuery(), 10, 0),
        allColumns,
        expectedResult);
  }

  @Test
  public void testQueryIncludedWithAnnotationsNoReviewStatus() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    ImmutableMap<String, Object> expectedResult =
        ImmutableMap.<String, Object>builder()
            .put("person_id", INCLUDED_PERSON_ID)
            .put("integer annotation", 123)
            .put("string annotation", "foo")
            .put("boolean annotation", true)
            .put("date annotation", "2017-02-14")
            .put("enum annotation", "zebra")
            .build();
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "integer annotation",
            "string annotation",
            "boolean annotation",
            "date annotation",
            "enum annotation"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, INCLUDED_ONLY, annotationQuery, 10, 0),
        annotationQuery.getColumns(),
        expectedResult);
  }

  @Test
  public void testQueryAllWithAnnotations() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");

    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, new AnnotationQuery(), 10, 0),
        allColumns,
        expectedResult1,
        expectedResult2);
  }

  @Test
  public void testQueryAllWithAnnotationsLimit1() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");

    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, new AnnotationQuery(), 1, 0),
        allColumns,
        expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsLimit1Offset1() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");

    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, new AnnotationQuery(), 1, 1),
        allColumns,
        expectedResult2);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByIntegerDescending() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(integer annotation)", "person_id"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        expectedResult2,
        expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByBoolean() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("boolean annotation", "person_id"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        expectedResult2,
        expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByDateDescending() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(date annotation)", "person_id"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        expectedResult2,
        expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByString() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("string annotation", "person_id"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        expectedResult2,
        expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByEnum() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("enum annotation", "person_id"));
    assertResults(
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
        allColumns,
        expectedResult2,
        expectedResult1);
  }

  private void saveReviewStatuses() {
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), INCLUDED_PERSON_ID, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), EXCLUDED_PERSON_ID, CohortStatus.EXCLUDED));
  }

  private void saveAnnotations(
      long personId,
      Integer integerValue,
      String stringValue,
      Boolean booleanValue,
      String dateValue,
      String enumValue)
      throws ParseException {
    if (integerValue != null) {
      DbParticipantCohortAnnotation annotation = new DbParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(
          integerAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueInteger(integerValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (stringValue != null) {
      DbParticipantCohortAnnotation annotation = new DbParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(
          stringAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueString(stringValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (booleanValue != null) {
      DbParticipantCohortAnnotation annotation = new DbParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(
          booleanAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueBoolean(booleanValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (dateValue != null) {
      DbParticipantCohortAnnotation annotation = new DbParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(dateAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      Date date = new Date(DATE_FORMAT.parse(dateValue).getTime());
      annotation.setAnnotationValueDate(date);
      participantCohortAnnotationDao.save(annotation);
    }
    if (enumValue != null) {
      DbParticipantCohortAnnotation annotation = new DbParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(enumAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setCohortAnnotationEnumValue(enumValueMap.get(enumValue));
      participantCohortAnnotationDao.save(annotation);
    }
  }

  private void assertResults(
      AnnotationQueryBuilder.AnnotationResults results,
      List<String> expectedColumns,
      ImmutableMap<String, Object>... expectedResults) {
    assertThat(results.getColumns()).isEqualTo(expectedColumns);
    List<Map<String, Object>> actualResults = Lists.newArrayList(results.getResults());
    if (actualResults.size() != expectedResults.length) {
      fail(
          "Expected "
              + expectedResults.length
              + ", got "
              + actualResults.size()
              + "; actual results: "
              + actualResults);
    }
    for (int i = 0; i < actualResults.size(); i++) {
      MapDifference<String, Object> difference =
          Maps.difference(actualResults.get(i), expectedResults[i]);
      if (!difference.areEqual()) {
        fail(
            "Result "
                + i
                + " had difference: "
                + difference.entriesDiffering()
                + "; unexpected entries: "
                + difference.entriesOnlyOnLeft()
                + "; missing entries: "
                + difference.entriesOnlyOnRight()
                + "; actual result: "
                + actualResults.get(i));
      }
    }
  }
}
