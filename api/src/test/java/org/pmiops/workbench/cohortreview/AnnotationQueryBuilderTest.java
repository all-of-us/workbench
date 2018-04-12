package org.pmiops.workbench.cohortreview;

import static org.junit.Assert.fail;
import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
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

  private static final ImmutableList<CohortStatus> INCLUDED_ONLY =
      ImmutableList.of(CohortStatus.INCLUDED);

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

    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), 1L, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), 2L, CohortStatus.EXCLUDED));

    cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "integer annotation",
            AnnotationType.INTEGER));
    cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "string annotation",
            AnnotationType.STRING));
    cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "boolean annotation",
            AnnotationType.BOOLEAN));
    cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "date annotation",
            AnnotationType.DATE));
    cohortAnnotationDefinitionDao.save(
        makeAnnotationDefinition(cohort.getCohortId(), "enum annotation",
            AnnotationType.ENUM, "zebra", "aardvark"));
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
            + "; missing entries: " + difference.entriesOnlyOnRight());
      }
    }
  }
}
