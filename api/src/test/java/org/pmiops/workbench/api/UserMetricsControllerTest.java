package org.pmiops.workbench.api;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CohortMapper;
import org.pmiops.workbench.utils.mappers.CohortMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserMetricsControllerTest {

  @Mock private CloudStorageService mockCloudStorageService;
  @Mock private UserRecentResourceService mockUserRecentResourceService;
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private FireCloudService mockFireCloudService;
  @Mock private WorkspaceService mockWorkspaceService;

  private UserMetricsController userMetricsController;
  private static final Instant NOW = Instant.now();
  @Autowired private CohortMapper cohortMapper;

  private FakeClock fakeClock = new FakeClock(NOW);

  private DbUser dbUser;
  private DbUserRecentResource dbUserRecentResource1;
  private DbUserRecentResource dbUserRecentResource2;
  private DbUserRecentResource dbUserRecentResource3;

  private Workspace fcWorkspace1;
  private Workspace fcWorkspace2;

  private DbWorkspace dbWorkspace1;
  private DbWorkspace dbWorkspace2;

  @TestConfiguration
  @Import({CohortMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    dbUser = new DbUser();
    dbUser.setUserId(123L);

    DbCohort dbCohort = new DbCohort();
    dbCohort.setName("Cohort Name");
    dbCohort.setCreator(dbUser);
    dbCohort.setCohortId(1L);
    dbCohort.setDescription("Cohort description");
    dbCohort.setLastModifiedTime(new Timestamp(fakeClock.millis()));
    dbCohort.setCreationTime(new Timestamp(fakeClock.millis()));

    dbWorkspace1 = new DbWorkspace();
    dbWorkspace1.setWorkspaceId(1L);
    dbWorkspace1.setWorkspaceNamespace("workspaceNamespace1");
    dbWorkspace1.setFirecloudName("firecloudname1");

    dbWorkspace2 = new DbWorkspace();
    dbWorkspace2.setWorkspaceId(2L);
    dbWorkspace2.setWorkspaceNamespace("workspaceNamespace2");
    dbWorkspace2.setFirecloudName("firecloudName2");

    dbUserRecentResource1 = new DbUserRecentResource();
    dbUserRecentResource1.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    dbUserRecentResource1.setCohort(null);
    dbUserRecentResource1.setLastAccessDate(new Timestamp(fakeClock.millis()));
    dbUserRecentResource1.setUserId(dbUser.getUserId());
    dbUserRecentResource1.setWorkspaceId(dbWorkspace1.getWorkspaceId());

    when(mockWorkspaceService.findActiveByWorkspaceId(dbUserRecentResource1.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace1));

    dbUserRecentResource2 = new DbUserRecentResource();
    dbUserRecentResource2.setNotebookName(null);
    dbUserRecentResource2.setCohort(dbCohort);
    dbUserRecentResource2.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    dbUserRecentResource2.setUserId(dbUser.getUserId());
    dbUserRecentResource2.setWorkspaceId(dbWorkspace2.getWorkspaceId());
    when(mockWorkspaceService.findActiveByWorkspaceId(dbUserRecentResource2.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace2));

    dbUserRecentResource3 = new DbUserRecentResource();
    dbUserRecentResource3.setNotebookName("gs://bucketFile/notebooks/notebook2.ipynb");
    dbUserRecentResource3.setCohort(null);
    dbUserRecentResource3.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    dbUserRecentResource3.setUserId(dbUser.getUserId());
    dbUserRecentResource3.setWorkspaceId(dbWorkspace2.getWorkspaceId());

    when(mockWorkspaceService.findActiveByWorkspaceId(dbUserRecentResource3.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace2));

    fcWorkspace1 = new Workspace();
    fcWorkspace1.setNamespace(dbWorkspace1.getFirecloudName());

    fcWorkspace2 = new Workspace();
    fcWorkspace1.setNamespace(dbWorkspace2.getFirecloudName());

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("OWNER");
    workspaceResponse.setWorkspace(fcWorkspace1);

    WorkspaceResponse workspaceResponse2 = new WorkspaceResponse();
    workspaceResponse2.setAccessLevel("READER");
    workspaceResponse2.setWorkspace(fcWorkspace2);

    when(mockUserProvider.get()).thenReturn(dbUser);

    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(
            Arrays.asList(dbUserRecentResource1, dbUserRecentResource2, dbUserRecentResource3));

    when(mockWorkspaceService.getRequired(
            dbWorkspace2.getWorkspaceNamespace(), dbWorkspace2.getFirecloudName()))
        .thenReturn(dbWorkspace2);

    when(mockFireCloudService.getWorkspace(dbWorkspace1))
        .thenReturn(Optional.of(workspaceResponse));

    when(mockFireCloudService.getWorkspace(dbWorkspace2))
        .thenReturn(Optional.of(workspaceResponse2));

    when(mockCloudStorageService.getExistingBlobIdsIn(anyList()))
        .then(
            (i) -> {
              List<BlobId> ids = i.getArgument(0);
              if (ids.contains(null)) {
                throw new NullPointerException();
              }
              return ImmutableSet.copyOf(ids);
            });

    userMetricsController =
        new UserMetricsController(
            mockUserProvider,
            mockUserRecentResourceService,
            mockWorkspaceService,
            mockFireCloudService,
            mockCloudStorageService,
            fakeClock);
    userMetricsController.setDistinctWorkspaceLimit(5);
  }

  @Test
  public void testGetUserRecentResourceFromRawBucket() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithDuplicatedNameInPath() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/nb.ipynb/intermediate/nb.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(
        recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/nb.ipynb/intermediate/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "nb.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithSpacesInPath() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/note books/My favorite notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/note books/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "My favorite notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceInvalidURINotebookPath() {
    dbUserRecentResource1.setNotebookName("my local notebook directory: notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.size(), 0);
  }

  @Test
  public void testGetUserRecentResourceNotebookPathEndsWithSlash() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/notebooks/notebook.ipynb/");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertNotNull(recentResources.get(0).getNotebook());
    assertEquals(
        recentResources.get(0).getNotebook().getPath(),
        "gs://bucketFile/notebooks/notebook.ipynb/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "");
  }

  @Test
  public void testGetUserRecentResourceNonexistentNotebook() {
    dbUserRecentResource1.setNotebookName("gs://bkt/notebooks/notebook.ipynb");
    dbUserRecentResource2.setCohort(null);
    dbUserRecentResource2.setNotebookName("gs://bkt/notebooks/not-found.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(ImmutableList.of(dbUserRecentResource1, dbUserRecentResource2));
    when(mockCloudStorageService.getExistingBlobIdsIn(anyList()))
        .thenReturn(ImmutableSet.of(BlobId.of("bkt", "notebooks/notebook.ipynb")));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.size(), 1);
    assertNotNull(recentResources.get(0).getNotebook());
    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResource() {
    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(3, recentResources.size());
    assertNull(recentResources.get(0).getCohort());
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/");

    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook1.ipynb");
    assertNotNull(recentResources.get(1).getCohort());
    assertEquals(recentResources.get(1).getCohort().getName(), "Cohort Name");
  }

  @Test
  public void testGetUserRecentResources_missingWorkspace() {
    dbUserRecentResource1.setWorkspaceId(9999); // missing workspace
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(ImmutableList.of(dbUserRecentResource1, dbUserRecentResource2));
    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertEquals(1, recentResources.size());
    RecentResource foundResource = recentResources.get(0);
    final Cohort cohort1 = foundResource.getCohort();
    final Cohort cohort2 = cohortMapper.dbModelToClient(dbUserRecentResource2.getCohort());

    // Clear out fields that aren't included in the DB Cohort class so
    // direct equals comparison can be used.
    cohort1.setEtag(null);
    assertEquals(cohort1, cohort2);

    assertEquals(foundResource.getWorkspaceId(), (Long) dbUserRecentResource2.getWorkspaceId());
  }

  @Test
  public void testWorkspaceLimit() {
    userMetricsController.setDistinctWorkspaceLimit(1);
    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();

    assertNotNull(recentResources);
    assertEquals(1, recentResources.size());
    assertNull(recentResources.get(0).getCohort());
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/");
  }

  @Test
  public void testDeleteResource() {
    RecentResourceRequest request = new RecentResourceRequest();
    request.setNotebookName(dbUserRecentResource1.getNotebookName());
    userMetricsController.deleteRecentResource(
        dbWorkspace2.getWorkspaceNamespace(), dbWorkspace2.getFirecloudName(), request);
    verify(mockUserRecentResourceService)
        .deleteNotebookEntry(
            dbWorkspace2.getWorkspaceId(),
            dbUser.getUserId(),
            dbUserRecentResource1.getNotebookName());
  }

  @Test
  public void testUpdateRecentResource() {
    Timestamp now = new Timestamp(fakeClock.instant().toEpochMilli());
    DbUserRecentResource mockUserRecentResource = new DbUserRecentResource();
    mockUserRecentResource.setCohort(null);
    mockUserRecentResource.setWorkspaceId(dbWorkspace2.getWorkspaceId());
    mockUserRecentResource.setUserId(dbUser.getUserId());
    mockUserRecentResource.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");
    mockUserRecentResource.setLastAccessDate(now);
    when(mockUserRecentResourceService.updateNotebookEntry(
            dbWorkspace2.getWorkspaceId(),
            dbUser.getUserId(),
            "gs://newBucket/notebooks/notebook.ipynb"))
        .thenReturn(mockUserRecentResource);

    RecentResourceRequest request = new RecentResourceRequest();
    request.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");

    RecentResource addedEntry =
        userMetricsController
            .updateRecentResource(
                dbWorkspace2.getWorkspaceNamespace(), dbWorkspace2.getFirecloudName(), request)
            .getBody();

    assertNotNull(addedEntry);
    assertEquals((long) addedEntry.getWorkspaceId(), dbWorkspace2.getWorkspaceId());
    assertNull(addedEntry.getCohort());
    assertNotNull(addedEntry.getNotebook());
    assertEquals(addedEntry.getNotebook().getName(), "notebook.ipynb");
    assertEquals(addedEntry.getNotebook().getPath(), "gs://newBucket/notebooks/");
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_nullNotebookName_passes() {
    dbUserRecentResource1.setNotebookName(null);
    assertTrue(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1));
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_validNotebookName_passes() {
    assertTrue(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1));
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_invalidNotebookName_fails() {
    dbUserRecentResource1.setNotebookName("invalid-notebook@name");
    assertFalse(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1));
  }
}
