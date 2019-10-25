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
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserMetricsControllerTest {

  @Mock private CloudStorageService cloudStorageService;
  @Mock private UserRecentResourceService userRecentResourceService;
  @Mock private Provider<User> userProvider;
  @Mock private FireCloudService fireCloudService;
  @Mock private WorkspaceService workspaceService;

  private UserMetricsController userMetricsController;
  private static final Instant NOW = Instant.now();
  private FakeClock clock = new FakeClock(NOW);

  private User user;
  private UserRecentResource resource1;
  private UserRecentResource resource2;
  private Workspace workspace2;

  @Before
  public void setUp() {
    user = new User();
    user.setUserId(123L);

    Cohort cohort = new Cohort();
    cohort.setName("Cohort Name");
    cohort.setCohortId(1L);
    cohort.setDescription("Cohort description");
    cohort.setLastModifiedTime(new Timestamp(clock.millis()));
    cohort.setCreationTime(new Timestamp(clock.millis()));

    Workspace workspace1 = new Workspace();
    workspace1.setWorkspaceId(1L);
    workspace1.setWorkspaceNamespace("workspaceNamespace1");
    workspace1.setFirecloudName("firecloudname1");

    workspace2 = new Workspace();
    workspace2.setWorkspaceId(2L);
    workspace2.setWorkspaceNamespace("workspaceNamespace2");
    workspace2.setFirecloudName("firecloudName2");

    resource1 = new UserRecentResource();
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    resource1.setCohort(null);
    resource1.setLastAccessDate(new Timestamp(clock.millis()));
    resource1.setUserId(user.getUserId());
    resource1.setWorkspaceId(workspace1.getWorkspaceId());

    resource2 = new UserRecentResource();
    resource2.setNotebookName(null);
    resource2.setCohort(cohort);
    resource2.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource2.setUserId(user.getUserId());
    resource2.setWorkspaceId(workspace2.getWorkspaceId());

    UserRecentResource resource3 = new UserRecentResource();
    resource3.setNotebookName("gs://bucketFile/notebooks/notebook2.ipynb");
    resource3.setCohort(null);
    resource3.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource3.setUserId(user.getUserId());
    resource3.setWorkspaceId(workspace2.getWorkspaceId());

    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(workspace1.getFirecloudName());

    FirecloudWorkspace fcWorkspace2 = new FirecloudWorkspace();
    fcWorkspace.setNamespace(workspace2.getFirecloudName());

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("OWNER");
    workspaceResponse.setWorkspace(fcWorkspace);

    WorkspaceResponse workspaceResponse2 = new WorkspaceResponse();
    workspaceResponse2.setAccessLevel("READER");
    workspaceResponse2.setWorkspace(fcWorkspace2);

    when(userProvider.get()).thenReturn(user);

    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Arrays.asList(resource1, resource2, resource3));

    when(workspaceService.findByWorkspaceId(workspace1.getWorkspaceId())).thenReturn(workspace1);

    when(workspaceService.findByWorkspaceId(workspace2.getWorkspaceId())).thenReturn(workspace2);

    when(workspaceService.getRequired(
            workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName()))
        .thenReturn(workspace2);

    when(fireCloudService.getWorkspace(
            workspace1.getWorkspaceNamespace(), workspace1.getFirecloudName()))
        .thenReturn(workspaceResponse);

    when(fireCloudService.getWorkspace(
            workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName()))
        .thenReturn(workspaceResponse2);

    when(cloudStorageService.blobsExist(anyList()))
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
            userProvider,
            userRecentResourceService,
            workspaceService,
            fireCloudService,
            cloudStorageService,
            clock);
    userMetricsController.setDistinctWorkspaceLimit(5);
  }

  @Test
  public void testGetUserRecentResourceFromRawBucket() {
    resource1.setNotebookName("gs://bucketFile/notebook.ipynb");
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
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
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
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
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
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
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(Collections.singletonList(resource1));

    RecentResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(recentResources.size(), 0);
  }

  @Test
  public void testGetUserRecentResourceNotebookPathEndsWithSlash() {
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook.ipynb/");
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
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
    when(userRecentResourceService.findAllResourcesByUser(user.getUserId()))
        .thenReturn(ImmutableList.of(resource1, resource2));
    when(cloudStorageService.blobsExist(anyList()))
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
    verify(userRecentResourceService)
        .deleteNotebookEntry(
            workspace2.getWorkspaceId(), user.getUserId(), resource1.getNotebookName());
  }

  @Test
  public void testUpdateRecentResource() {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    UserRecentResource mockUserRecentResource = new UserRecentResource();
    mockUserRecentResource.setCohort(null);
    mockUserRecentResource.setWorkspaceId(workspace2.getWorkspaceId());
    mockUserRecentResource.setUserId(user.getUserId());
    mockUserRecentResource.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");
    mockUserRecentResource.setLastAccessDate(now);
    when(userRecentResourceService.updateNotebookEntry(
            workspace2.getWorkspaceId(),
            user.getUserId(),
            "gs://newBucket/notebooks/notebook.ipynb",
            now))
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
