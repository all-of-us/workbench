package org.pmiops.workbench.workspaces.resources;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentlyModifiedResourceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRecentResourceServiceTest {

  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired DataSetDao datasetDao;
  @Autowired UserDao userDao;
  @Autowired UserRecentlyModifiedResourceDao userRecentlyModifiedResourceDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired UserRecentResourceService userRecentResourceService;

  private DbUser user;
  private DbWorkspace workspace;
  private DbCohort cohort;
  private DbCohortReview cohortReview;
  private DbConceptSet conceptSet;
  private DbDataset dataset;

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({UserRecentResourceServiceImpl.class})
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());

    workspace = workspaceDao.save(new DbWorkspace());

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort = cohortDao.save(cohort);

    cohortReview = new DbCohortReview();
    cohortReview.setCohortId(cohort.getCohortId());
    cohortReview = cohortReviewDao.save(cohortReview);

    DbConceptSetConceptId dbConceptSetConceptId =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(true).build();
    conceptSet = new DbConceptSet();
    conceptSet.setWorkspaceId(workspace.getWorkspaceId());
    conceptSet.setConceptSetConceptIds(ImmutableSet.of(dbConceptSetConceptId));
    conceptSet = conceptSetDao.save(conceptSet);

    DbDatasetValue dbDatasetValue = new DbDatasetValue();
    dbDatasetValue.setDomainId(Domain.OBSERVATION.toString());
    dbDatasetValue.setValue("Mock Value");

    dataset = new DbDataset();
    dataset.setName("Mock Data Set");
    dataset.setCohortIds(Collections.singletonList(cohort.getCohortId()));
    dataset.setConceptSetIds(Collections.singletonList(conceptSet.getConceptSetId()));
    dataset.setValues(Collections.singletonList(dbDatasetValue));
    dataset.setWorkspaceId(workspace.getWorkspaceId());
    dataset = datasetDao.save(dataset);
  }

  @Test
  public void testInsertCohortEntry() {
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);

    DbCohort newCohort = new DbCohort();
    newCohort.setWorkspaceId(workspace.getWorkspaceId());
    newCohort = cohortDao.save(newCohort);

    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), newCohort.getCohortId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(2);
  }

  @Test
  public void testInsertCohortReviewEntry() {
    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);

    DbCohortReview newCohortReview = new DbCohortReview();
    newCohortReview.setCohortId(cohortReview.getCohortId());
    newCohortReview = cohortReviewDao.save(newCohortReview);

    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), newCohortReview.getCohortReviewId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(2);
  }

  @Test
  public void testInsertConceptSetEntry() {
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    DbConceptSet newConceptSet = new DbConceptSet();
    newConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    newConceptSet = conceptSetDao.save(newConceptSet);
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), newConceptSet.getConceptSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(2);
  }

  @Test
  public void testInsertDatasetEntry() {
    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    DbDataset newDataset = new DbDataset();
    newDataset.setWorkspaceId(workspace.getWorkspaceId());
    newDataset = datasetDao.save(newDataset);
    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), newDataset.getDataSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(2);
  }

  @Test
  public void testInsertNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook2");
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(2);
  }

  @Test
  public void testUpdateCohortAccessTime() {
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    CLOCK.increment(20000);
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
  }

  @Test
  public void testUpdateCohortReviewAccessTime() {
    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    CLOCK.increment(20000);
    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
  }

  @Test
  public void testUpdateConceptSetAccessTime() {
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    CLOCK.increment(20000);
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
  }

  @Test
  public void testUpdateDatasetAccessTime() {
    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    CLOCK.increment(20000);
    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
  }

  @Test
  public void testUpdateNotebookAccessTime() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    CLOCK.increment(200);
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
  }

  @Test
  public void testUserLimit() {
    DbWorkspace newWorkspace = new DbWorkspace();
    newWorkspace.setWorkspaceId(2L);
    workspaceDao.save(newWorkspace);

    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);

    // record 3 recent resources

    int initialEntryCount = 3;
    String firstNotebook = "gs://someDirectory1/notebooks/notebook";
    userRecentResourceService.updateNotebookEntry(
        newWorkspace.getWorkspaceId(), user.getUserId(), firstNotebook);

    CLOCK.increment(2000);

    userRecentResourceService.updateNotebookEntry(
        newWorkspace.getWorkspaceId(), user.getUserId(), "notebooks");
    userRecentResourceService.updateCohortEntry(
        newWorkspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());

    // record enough recent resources to fill up the table

    int toAdd = UserRecentResourceService.USER_ENTRY_COUNT - initialEntryCount;
    while (toAdd-- > 0) {
      CLOCK.increment(2000);
      userRecentResourceService.updateNotebookEntry(
          newWorkspace.getWorkspaceId(),
          user.getUserId(),
          "gs://someDirectory1/notebooks/notebook" + toAdd);
    }

    CLOCK.increment(2000);
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(UserRecentResourceService.USER_ENTRY_COUNT);

    // add another and observe that it does not increase in size...

    userRecentResourceService.updateNotebookEntry(
        newWorkspace.getWorkspaceId(),
        user.getUserId(),
        "gs://someDirectory/notebooks/notebookExtra");
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(UserRecentResourceService.USER_ENTRY_COUNT);

    // ...because the first entry is removed

    DbUserRecentlyModifiedResource cache =
        userRecentlyModifiedResourceDao.getResource(
            newWorkspace.getWorkspaceId(),
            user.getUserId(),
            DbUserRecentlyModifiedResourceType.NOTEBOOK,
            firstNotebook);

    assertThat(cache).isNull();
  }

  @Test
  public void testDeleteNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.deleteNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteNonExistentNotebookEntry() {
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
    userRecentResourceService.deleteNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook");
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteCohortEntry() {
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.deleteCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteNonExistentCohortEntry() {
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
    userRecentResourceService.deleteCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteCohortReviewEntry() {
    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.deleteCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteNonExistentCohortReviewEntry() {
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
    userRecentResourceService.deleteCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteConceptSetEntry() {
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.deleteConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteNonExistentConceptSetEntry() {
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
    userRecentResourceService.deleteConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteDatasetEntry() {
    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(1);
    userRecentResourceService.deleteDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testDeleteNonExistentDatasetEntry() {
    long rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
    userRecentResourceService.deleteDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());
    rowsCount = userRecentlyModifiedResourceDao.count();
    assertThat(rowsCount).isEqualTo(0);
  }

  @Test
  public void testFindAllResources() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    CLOCK.increment(10000);

    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    CLOCK.increment(10000);

    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook2");

    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());

    List<DbUserRecentlyModifiedResource> resources =
        userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());

    assertThat(resources.size()).isEqualTo(4);
    assertThat(resources.get(0).getResourceId())
        .isEqualTo("gs://someDirectory1/notebooks/notebook2");
    assertThat(Long.parseLong(resources.get(1).getResourceId()))
        .isEqualTo(conceptSet.getConceptSetId());
    assertThat(Long.parseLong(resources.get(2).getResourceId())).isEqualTo(cohort.getCohortId());
    assertThat(resources.get(3).getResourceId())
        .isEqualTo("gs://someDirectory1/notebooks/notebook1");
  }

  @Test
  public void testDeleteDependentCohortReviewOnDeletingCohort() {
    // Confirm the table has no recent modified resources
    List<DbUserRecentlyModifiedResource> resources =
        userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());

    assertThat(resources.size()).isEqualTo(0);

    // Add the following entry in user_recently_modified_resource:
    // 1) Cohort
    // 2) Cohort Review that is using the Cohort
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    userRecentResourceService.updateCohortReviewEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohortReview.getCohortReviewId());

    // Deleting Cohort should delete the Cohort Review using it
    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());
    assertThat(resources.size()).isEqualTo(2);

    userRecentResourceService.deleteCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());

    assertThat(resources.size()).isEqualTo(0);
  }

  @Test
  public void testDeleteDependentDataSetOnDeletingCohort() {
    // Confirm the table has no recent modified resources
    List<DbUserRecentlyModifiedResource> resources =
        userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());

    assertThat(resources.size()).isEqualTo(0);

    // Add the following entry in user_recently_modified_resource:
    // 1) Cohort
    // 2) Data Set that is using the Cohort
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());

    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());

    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());
    assertThat(resources.size()).isEqualTo(2);

    // Deleting Cohort should delete the dataSet using it
    userRecentResourceService.deleteCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());

    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());
    assertThat(resources.size()).isEqualTo(0);
  }

  @Test
  public void testDeleteDependentDataSetOnDeletingConceptSet() {
    List<DbUserRecentlyModifiedResource> resources =
        userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());

    assertThat(resources.size()).isEqualTo(0);

    // Add the following entry in user_recently_modified_resource:
    // 1) Concept Set
    // 2) Data Set that is using the Concept Set
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());

    userRecentResourceService.updateDataSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), dataset.getDataSetId());

    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());
    assertThat(resources.size()).isEqualTo(2);

    // Deleting Concept Set should also delete the data Set entry that is using the Concept Set
    userRecentResourceService.deleteConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());

    resources = userRecentResourceService.findAllRecentlyModifiedResourcesByUser(user.getUserId());
    assertThat(resources.size()).isEqualTo(0);
  }
}
