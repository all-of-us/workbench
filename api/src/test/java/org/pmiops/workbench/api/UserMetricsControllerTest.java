package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Clock;
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
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserMetricsControllerTest {

  @Mock private CloudStorageClient mockCloudStorageClient;
  @Mock private UserRecentResourceService mockUserRecentResourceService;
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private FireCloudService mockFireCloudService;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private WorkspaceAuthService workspaceAuthService;

  private UserMetricsController userMetricsController;
  private static final Instant NOW = Instant.now();
  @Autowired private CohortMapper cohortMapper;
  @Autowired private CommonMappers commonMappers;
  @Autowired private FirecloudMapper firecloudMapper;

  private FakeClock fakeClock = new FakeClock(NOW);

  private DbUser dbUser;
  private DbUserRecentResource dbUserRecentResource1;
  private DbUserRecentResource dbUserRecentResource2;
  private DbUserRecentResource dbUserRecentResource3;

  private FirecloudWorkspace fcWorkspace1;
  private FirecloudWorkspace fcWorkspace2;

  private DbWorkspace dbWorkspace1;
  private DbWorkspace dbWorkspace2;

  @TestConfiguration
  @Import({CohortMapperImpl.class, CommonMappers.class, FirecloudMapperImpl.class})
  @MockBean({Clock.class})
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

    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setName("Concept Set");
    dbConceptSet.setDescription("This is a Condition Concept Set");
    dbConceptSet.setConceptSetId(1L);
    dbConceptSet.setDomainEnum(Domain.CONDITION);
    dbConceptSet.setLastModifiedTime(new Timestamp(fakeClock.millis()));
    dbConceptSet.setCreationTime(new Timestamp(fakeClock.millis()));

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

    when(workspaceDao.findActiveByWorkspaceId(dbUserRecentResource1.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace1));

    dbUserRecentResource2 = new DbUserRecentResource();
    dbUserRecentResource2.setNotebookName(null);
    dbUserRecentResource2.setCohort(dbCohort);
    dbUserRecentResource2.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    dbUserRecentResource2.setUserId(dbUser.getUserId());
    dbUserRecentResource2.setWorkspaceId(dbWorkspace2.getWorkspaceId());
    when(workspaceDao.findActiveByWorkspaceId(dbUserRecentResource2.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace2));

    dbUserRecentResource3 = new DbUserRecentResource();
    dbUserRecentResource3.setNotebookName("gs://bucketFile/notebooks/notebook2.ipynb");
    dbUserRecentResource3.setCohort(null);
    dbUserRecentResource3.setConceptSet(dbConceptSet);
    dbUserRecentResource3.setLastAccessDate(new Timestamp(fakeClock.millis() - 10000));
    dbUserRecentResource3.setUserId(dbUser.getUserId());
    dbUserRecentResource3.setWorkspaceId(dbWorkspace2.getWorkspaceId());

    when(workspaceDao.findActiveByWorkspaceId(dbUserRecentResource3.getWorkspaceId()))
        .thenReturn(Optional.of(dbWorkspace2));

    fcWorkspace1 = new FirecloudWorkspace();
    fcWorkspace1.setNamespace(dbWorkspace1.getFirecloudName());

    fcWorkspace2 = new FirecloudWorkspace();
    fcWorkspace1.setNamespace(dbWorkspace2.getFirecloudName());

    FirecloudWorkspaceResponse workspaceResponse = new FirecloudWorkspaceResponse();
    workspaceResponse.setAccessLevel("OWNER");
    workspaceResponse.setWorkspace(fcWorkspace1);

    FirecloudWorkspaceResponse workspaceResponse2 = new FirecloudWorkspaceResponse();
    workspaceResponse2.setAccessLevel("READER");
    workspaceResponse2.setWorkspace(fcWorkspace2);

    when(mockUserProvider.get()).thenReturn(dbUser);

    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(
            Arrays.asList(dbUserRecentResource1, dbUserRecentResource2, dbUserRecentResource3));

    when(workspaceDao.getRequired(
            dbWorkspace2.getWorkspaceNamespace(), dbWorkspace2.getFirecloudName()))
        .thenReturn(dbWorkspace2);

    when(mockFireCloudService.getWorkspace(dbWorkspace1))
        .thenReturn(Optional.of(workspaceResponse));

    when(mockFireCloudService.getWorkspace(dbWorkspace2))
        .thenReturn(Optional.of(workspaceResponse2));

    when(mockCloudStorageClient.getExistingBlobIdsIn(anyList()))
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
            workspaceDao,
            workspaceAuthService,
            mockFireCloudService,
            mockCloudStorageClient,
            commonMappers,
            firecloudMapper);
    userMetricsController.setDistinctWorkspaceLimit(5);
  }

  @Test
  public void testGetUserRecentResourceFromRawBucket() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath()).isEqualTo("gs://bucketFile/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithDuplicatedNameInPath() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/nb.ipynb/intermediate/nb.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/nb.ipynb/intermediate/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("nb.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithSpacesInPath() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/note books/My favorite notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/note books/");
    assertThat(recentResources.get(0).getNotebook().getName())
        .isEqualTo("My favorite notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceInvalidURINotebookPath() {
    dbUserRecentResource1.setNotebookName("my local notebook directory: notebook.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(0);
  }

  @Test
  public void testGetUserRecentResourceNotebookPathEndsWithSlash() {
    dbUserRecentResource1.setNotebookName("gs://bucketFile/notebooks/notebook.ipynb/");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources.get(0).getNotebook()).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/notebooks/notebook.ipynb/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("");
  }

  @Test
  public void testGetUserRecentResourceNonexistentNotebook() {
    dbUserRecentResource1.setNotebookName("gs://bkt/notebooks/notebook.ipynb");
    dbUserRecentResource2.setCohort(null);
    dbUserRecentResource2.setNotebookName("gs://bkt/notebooks/not-found.ipynb");
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(ImmutableList.of(dbUserRecentResource1, dbUserRecentResource2));
    when(mockCloudStorageClient.getExistingBlobIdsIn(anyList()))
        .thenReturn(ImmutableSet.of(BlobId.of("bkt", "notebooks/notebook.ipynb")));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(1);
    assertThat(recentResources.get(0).getNotebook()).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResource() {
    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(3);
    assertThat(recentResources.get(0).getCohort()).isNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/notebooks/");

    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("notebook1.ipynb");
    assertThat(recentResources.get(1).getCohort()).isNotNull();
    assertThat(recentResources.get(1).getCohort().getName()).isEqualTo("Cohort Name");
  }

  @Test
  public void testGetUserRecentResources_missingWorkspace() {
    dbUserRecentResource1.setWorkspaceId(9999); // missing workspace
    when(mockUserRecentResourceService.findAllResourcesByUser(dbUser.getUserId()))
        .thenReturn(ImmutableList.of(dbUserRecentResource1, dbUserRecentResource2));
    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources.size()).isEqualTo(1);
    WorkspaceResource foundResource = recentResources.get(0);
    final Cohort cohort1 = foundResource.getCohort();
    final Cohort cohort2 = cohortMapper.dbModelToClient(dbUserRecentResource2.getCohort());

    assertThat(cohort1).isEqualTo(cohort2);
    assertThat(foundResource.getWorkspaceId()).isEqualTo(dbUserRecentResource2.getWorkspaceId());
  }

  @Test
  public void testWorkspaceLimit() {
    userMetricsController.setDistinctWorkspaceLimit(1);
    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();

    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(1);
    assertThat(recentResources.get(0).getCohort()).isNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/notebooks/");
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

    WorkspaceResource addedEntry =
        userMetricsController
            .updateRecentResource(
                dbWorkspace2.getWorkspaceNamespace(), dbWorkspace2.getFirecloudName(), request)
            .getBody();

    assertThat(addedEntry).isNotNull();
    assertThat(addedEntry.getWorkspaceId()).isEqualTo(dbWorkspace2.getWorkspaceId());
    assertThat(addedEntry.getCohort()).isNull();
    assertThat(addedEntry.getNotebook()).isNotNull();
    assertThat(addedEntry.getNotebook().getName()).isEqualTo("notebook.ipynb");
    assertThat(addedEntry.getNotebook().getPath()).isEqualTo("gs://newBucket/notebooks/");
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_nullNotebookName_passes() {
    dbUserRecentResource1.setNotebookName(null);
    assertThat(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1))
        .isTrue();
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_validNotebookName_passes() {
    assertThat(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1))
        .isTrue();
  }

  @Test
  public void testHasValidBlobIdIfNotebookNamePresent_invalidNotebookName_fails() {
    dbUserRecentResource1.setNotebookName("invalid-notebook@name");
    assertThat(userMetricsController.hasValidBlobIdIfNotebookNamePresent(dbUserRecentResource1))
        .isFalse();
  }

  @Test
  public void testUserRecentConceptResourceHasDomainInformation() {
    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();

    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(3);
    assertThat(recentResources.get(2).getConceptSet()).isNotNull();
    assertThat(recentResources.get(2).getConceptSet().getDomain()).isEqualTo(Domain.CONDITION);
  }
}
