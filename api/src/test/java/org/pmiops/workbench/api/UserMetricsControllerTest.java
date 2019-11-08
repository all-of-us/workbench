package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
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
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.CohortMapper;
import org.pmiops.workbench.utils.CohortMapperImpl;
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

  private DbUser user;
  private DbUserRecentResource resource1;
  private DbUserRecentResource resource2;
  private DbWorkspace workspace2;

  @TestConfiguration
  @Import({CohortMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    user = new DbUser();
    user.setUserId(123L);

    DbCohort dbCohort = new DbCohort();
    dbCohort.setName("Cohort Name");
    dbCohort.setCreator(user);
    dbCohort.setCohortId(1L);
    dbCohort.setDescription("Cohort description");
    dbCohort.setLastModifiedTime(new Timestamp(fakeClock.millis()));
    dbCohort.setCreationTime(new Timestamp(fakeClock.millis()));

    DbWorkspace workspace1 = new DbWorkspace();
    workspace1.setWorkspaceId(1L);
    workspace1.setWorkspaceNamespace("workspaceNamespace1");
    workspace1.setFirecloudName("firecloudname1");

    workspace2 = new DbWorkspace();
    workspace2.setWorkspaceId(2L);
    workspace2.setWorkspaceNamespace("workspaceNamespace2");
    workspace2.setFirecloudName("firecloudName2");

    resource1 = new DbUserRecentResource();
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    resource1.setCohort(null);
    resource1.setLastAccessDate(new Timestamp(fakeClock.millis()));
    resource1.setUserId(user.getUserId());
    resource1.setWorkspaceId(workspace1.getWorkspaceId());

    when(mockWorkspaceService.findActiveByWorkspaceId(resource1.getWorkspaceId()))
        .thenReturn(Optional.of(workspace1));

    resource2 = new DbUserRecentResource();
    resource2.setNotebookName(null);
    resource2.setCohort(dbCohort);
    resource2.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    resource2.setUserId(user.getUserId());
    resource2.setWorkspaceId(workspace2.getWorkspaceId());
    when(mockWorkspaceService.findActiveByWorkspaceId(resource2.getWorkspaceId()))
        .thenReturn(Optional.of(workspace2));

    DbUserRecentResource resource3 = new DbUserRecentResource();
    resource3.setNotebookName("gs://bucketFile/notebooks/notebook2.ipynb");
    resource3.setCohort(null);
    resource3.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    resource3.setUserId(user.getUserId());
    resource3.setWorkspaceId(workspace2.getWorkspaceId());

    when(mockWorkspaceService.findActiveByWorkspaceId(resource3.getWorkspaceId()))
        .thenReturn(Optional.of(workspace2));

    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(workspace1.getFirecloudName());

    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace2 =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(workspace2.getFirecloudName());

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("OWNER");
    workspaceResponse.setWorkspace(fcWorkspace);

    WorkspaceResponse workspaceResponse2 = new WorkspaceResponse();
    workspaceResponse2.setAccessLevel("READER");
    workspaceResponse2.setWorkspace(fcWorkspace2);

    when(mockUserProvider.get()).thenReturn(user);

    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Arrays.asList(resource1, resource2, resource3));

    when(mockWorkspaceService.getRequired(
            workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName()))
        .thenReturn(workspace2);

    when(mockFireCloudService.getWorkspace(workspace1)).thenReturn(Optional.of(workspaceResponse));

    when(mockFireCloudService.getWorkspace(workspace2)).thenReturn(Optional.of(workspaceResponse2));

    when(mockCloudStorageService.blobsExist(anyList()))
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

    cohortMapper = new CohortMapperImpl();
  }

  @Test
  public void testGetUserRecentResourceFromRawBucket() {
    resource1.setNotebookName("gs://bucketFile/notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithDuplicatedNameInPath() {
    resource1.setNotebookName("gs://bucketFile/nb.ipynb/intermediate/nb.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(
        recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/nb.ipynb/intermediate/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "nb.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithSpacesInPath() {
    resource1.setNotebookName("gs://bucketFile/note books/My favorite notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/note books/");
    assertEquals(recentResources.get(0).getNotebook().getName(), "My favorite notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceInvalidURINotebookPath() {
    resource1.setNotebookName("my local notebook directory: notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.size(), 0);
  }

  @Test
  public void testGetUserRecentResourceNotebookPathEndsWithSlash() {
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook.ipynb/");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

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
    resource1.setNotebookName("gs://bkt/notebooks/notebook.ipynb");
    resource2.setCohort(null);
    resource2.setNotebookName("gs://bkt/notebooks/not-found.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(ImmutableList.of(resource1, resource2));
    when(mockCloudStorageService.blobsExist(anyList()))
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
    resource1.setWorkspaceId(9999); // missing workspace
    when(mockUserRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(ImmutableList.of(resource1, resource2));
    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertEquals(1, recentResources.size());
    RecentResource foundResource = recentResources.get(0);
    final Cohort cohort1 = foundResource.getCohort();
    final Cohort cohort2 = cohortMapper.dbModelToClient(resource2.getCohort());

    // Clear out fields that aren't included in the DB Cohort class so
    // direct equals comparison can be used.
    cohort1.setEtag(null);
    assertEquals(cohort1, cohort2);

    assertEquals(foundResource.getWorkspaceId(), (Long) resource2.getWorkspaceId());
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
    request.setNotebookName(resource1.getNotebookName());
    userMetricsController.deleteRecentResource(
        workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName(), request);
    verify(mockUserRecentResourceService)
        .deleteNotebookEntry(
            workspace2.getWorkspaceId(), user.getUserId(), resource1.getNotebookName());
  }

  @Test
  public void testUpdateRecentResource() {
    Timestamp now = new Timestamp(fakeClock.instant().toEpochMilli());
    DbUserRecentResource mockUserRecentResource = new DbUserRecentResource();
    mockUserRecentResource.setCohort(null);
    mockUserRecentResource.setWorkspaceId(workspace2.getWorkspaceId());
    mockUserRecentResource.setUserId(user.getUserId());
    mockUserRecentResource.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");
    mockUserRecentResource.setLastAccessDate(now);
    when(mockUserRecentResourceService.updateNotebookEntry(
            workspace2.getWorkspaceId(),
            user.getUserId(),
            "gs://newBucket/notebooks/notebook.ipynb"))
        .thenReturn(mockUserRecentResource);

    RecentResourceRequest request = new RecentResourceRequest();
    request.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");

    RecentResource addedEntry =
        userMetricsController
            .updateRecentResource(
                workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName(), request)
            .getBody();

    assertNotNull(addedEntry);
    assertEquals((long) addedEntry.getWorkspaceId(), workspace2.getWorkspaceId());
    assertNull(addedEntry.getCohort());
    assertNotNull(addedEntry.getNotebook());
    assertEquals(addedEntry.getNotebook().getName(), "notebook.ipynb");
    assertEquals(addedEntry.getNotebook().getPath(), "gs://newBucket/notebooks/");
  }
}
