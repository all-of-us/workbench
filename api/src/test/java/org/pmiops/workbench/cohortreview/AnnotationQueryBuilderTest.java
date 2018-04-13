package org.pmiops.workbench.cohortreview;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AnnotationQueryBuilderTest {

  @TestConfiguration
  @Import({
      AnnotationQueryBuilder.class
  })
  static class Configuration {
  }

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final long INCLUDED_PERSON_ID = 1L;
  private static final long EXCLUDED_PERSON_ID = 2L;

  private static final ImmutableList<CohortStatus> INCLUDED_ONLY =
      ImmutableList.of(CohortStatus.INCLUDED);

  private static final ImmutableList<CohortStatus> ALL_STATUSES =
      ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.EXCLUDED,
          CohortStatus.NEEDS_FURTHER_REVIEW);

  @Autowired
  private AnnotationQueryBuilder annotationQueryBuilder;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  private CohortReview cohortReview;
  private CohortAnnotationDefinition integerAnnotation;
  private CohortAnnotationDefinition stringAnnotation;
  private CohortAnnotationDefinition booleanAnnotation;
  private CohortAnnotationDefinition dateAnnotation;
  private CohortAnnotationDefinition enumAnnotation;
  private Map<String, CohortAnnotationEnumValue> enumValueMap;
  private ImmutableMap<String, Object> expectedResult1;
  private ImmutableMap<String, Object> expectedResult2;

  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersion(cdrVersion);

    Workspace workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    Cohort cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setName("males");
    cohort.setType("AOU");
    cohort.setCriteria("blah");
    cohortDao.save(cohort);


    cohortReview = new CohortReview();
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setCohortId(cohort.getCohortId());
    cohortReview.setMatchedParticipantCount(3);
    cohortReview.setReviewedCount(2);
    cohortReview.setReviewSize(3);
    cohortReviewDao.save(cohortReview);

    integerAnnotation = cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "integer annotation",
            AnnotationType.INTEGER));
    stringAnnotation = cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "string annotation",
            AnnotationType.STRING));
    booleanAnnotation = cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "boolean annotation",
            AnnotationType.BOOLEAN));
    dateAnnotation = cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "date annotation",
            AnnotationType.DATE));
    enumAnnotation = cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "enum annotation",
            AnnotationType.ENUM, "zebra", "aardvark"));
    enumValueMap = Maps.uniqueIndex(enumAnnotation.getEnumValues(), CohortAnnotationEnumValue::getName);

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
  }

  private CohortAnnotationDefinition makeAnnotationDefinition(long cohortId, String columnName,
      AnnotationType annotationType, String... enumValues) {
    CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition();
    cohortAnnotationDefinition.setAnnotationType(annotationType);
    cohortAnnotationDefinition.setCohortId(cohortId);
    cohortAnnotationDefinition.setColumnName(columnName);
    if (enumValues.length > 0) {
      for (int i = 0; i < enumValues.length; i++) {
        CohortAnnotationEnumValue enumValue = new CohortAnnotationEnumValue();
        enumValue.setOrder(i);
        enumValue.setName(enumValues[i]);
        cohortAnnotationDefinition.getEnumValues().add(enumValue);
      }
    }
    return cohortAnnotationDefinition;
  }

  private ParticipantCohortStatus makeStatus(long cohortReviewId, long participantId, CohortStatus status) {
    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey();
    key.setCohortReviewId(cohortReviewId);
    key.setParticipantId(participantId);
    ParticipantCohortStatus result = new ParticipantCohortStatus();
    result.setStatus(status);
    result.setParticipantKey(key);
    return result;
  }

  @Test
  public void testQueryEmptyReview() {
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, INCLUDED_ONLY,
        new AnnotationQuery(), 10, 0));
  }

  @Test
  public void testQueryOneIncluded() {
    saveReviewStatuses();
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, INCLUDED_ONLY,
        new AnnotationQuery(), 10, 0),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));

  }

  @Test
  public void testQueryAllStatuses() {
    saveReviewStatuses();
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        new AnnotationQuery(), 10, 0),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"),
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"));
  }

  @Test
  public void testQueryAllStatusesReviewStatusOrder() {
    saveReviewStatuses();
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("review_status"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0),
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));

  }

  @Test
  public void testQueryAllStatusesReviewPersonIdOrderDescending() {
    saveReviewStatuses();
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(person_id)"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0),
        ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
        ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"));

  }

  @Test
  public void testQueryIncludedWithAnnotations() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
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
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, INCLUDED_ONLY,
        new AnnotationQuery(), 10, 0), expectedResult);

  }

  @Test
  public void testQueryIncludedWithAnnotationsNoReviewStatus() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
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
    annotationQuery.setColumns(ImmutableList.of("person_id", "integer annotation", "string annotation",
        "boolean annotation", "date annotation", "enum annotation"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, INCLUDED_ONLY,
        annotationQuery, 10, 0), expectedResult);
  }

  @Test
  public void testQueryAllWithAnnotations() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");

    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        new AnnotationQuery(), 10, 0), expectedResult1, expectedResult2);
  }

  @Test
  public void testQueryAllWithAnnotationsLimit1() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");

    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        new AnnotationQuery(), 1, 0), expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsLimit1Offset1() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");

    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        new AnnotationQuery(), 1, 1), expectedResult2);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByIntegerDescending() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(integer annotation)", "person_id"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0), expectedResult2, expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByBoolean() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("boolean annotation", "person_id"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0), expectedResult2, expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByDateDescending() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("DESCENDING(date annotation)", "person_id"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0), expectedResult2, expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByString() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("string annotation", "person_id"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0), expectedResult2, expectedResult1);
  }

  @Test
  public void testQueryAllWithAnnotationsOrderByEnum() throws Exception {
    saveReviewStatuses();
    saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14","zebra");
    saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15","aardvark");
    AnnotationQuery annotationQuery = new AnnotationQuery();
    annotationQuery.setOrderBy(ImmutableList.of("enum annotation", "person_id"));
    assertResults(annotationQueryBuilder.materializeAnnotationQuery(cohortReview, ALL_STATUSES,
        annotationQuery, 10, 0), expectedResult2, expectedResult1);
  }

  private void saveReviewStatuses() {
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), INCLUDED_PERSON_ID, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), EXCLUDED_PERSON_ID, CohortStatus.EXCLUDED));
  }

  private void saveAnnotations(long personId, Integer integerValue, String stringValue,
      Boolean booleanValue, String dateValue, String enumValue) throws ParseException {
    if (integerValue != null) {
      ParticipantCohortAnnotation annotation = new ParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(integerAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueInteger(integerValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (stringValue != null) {
      ParticipantCohortAnnotation annotation = new ParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(stringAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueString(stringValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (booleanValue != null) {
      ParticipantCohortAnnotation annotation = new ParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(booleanAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setAnnotationValueBoolean(booleanValue);
      participantCohortAnnotationDao.save(annotation);
    }
    if (dateValue != null) {
      ParticipantCohortAnnotation annotation = new ParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(dateAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      Date date = new Date(DATE_FORMAT.parse(dateValue).getTime());
      annotation.setAnnotationValueDate(date);
      participantCohortAnnotationDao.save(annotation);
    }
    if (enumValue != null) {
      ParticipantCohortAnnotation annotation = new ParticipantCohortAnnotation();
      annotation.setCohortAnnotationDefinitionId(enumAnnotation.getCohortAnnotationDefinitionId());
      annotation.setParticipantId(personId);
      annotation.setCohortReviewId(cohortReview.getCohortReviewId());
      annotation.setCohortAnnotationEnumValue(enumValueMap.get(enumValue));
      participantCohortAnnotationDao.save(annotation);
    }
  }

  private void assertResults(Iterable<Map<String, Object>> results,
      ImmutableMap<String, Object>... expectedResults) {
    List<Map<String, Object>> actualResults = Lists.newArrayList(results);
    if (actualResults.size() != expectedResults.length) {
      fail("Expected " + expectedResults.length + ", got " + actualResults.size() + "; actual results: " +
          actualResults);
    }
    for (int i = 0; i < actualResults.size(); i++) {
      MapDifference<String, Object> difference =
          Maps.difference((Map<String, Object>) actualResults.get(i), expectedResults[i]);
      if (!difference.areEqual()) {
        fail("Result " + i + " had difference: " + difference.entriesDiffering()
            + "; unexpected entries: " + difference.entriesOnlyOnLeft()
            + "; missing entries: " + difference.entriesOnlyOnRight()
            + "; actual result: " + actualResults.get(i));
      }
    }
  }
}
