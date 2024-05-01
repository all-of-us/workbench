package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserRecentlyModifiedResourceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.notebooks.NotebookUtils;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapper;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class UserMetricsControllerTest {

  @Mock private CloudStorageClient mockCloudStorageClient;
  @Mock private UserRecentResourceService mockUserRecentResourceService;
  @Mock private CohortService mockCohortService;
  @Mock private CohortReviewService mockCohortReviewService;
  @Mock private ConceptSetService mockConceptSetService;
  @Mock private DataSetService mockDataSetService;
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private FireCloudService mockFireCloudService;
  @Mock private WorkspaceAuthService workspaceAuthService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private FakeClock fakeClock;
  @Autowired private UserRecentlyModifiedResourceDao userRecentlyModifiedResourceDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceResourceMapper workspaceResourceMapper;

  private UserMetricsController userMetricsController;

  private DbUser dbUser;

  private DbCohort dbCohort;
  private DbConceptSet dbConceptSet;

  private DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource1;
  private DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource2;
  private DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource3;

  private DbWorkspace dbWorkspace1;
  private DbWorkspace dbWorkspace2;

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    WorkspaceResourceMapperImpl.class,
  })
  @MockBean({
    CohortService.class,
    ConceptSetService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    DbCdrVersion dbCdrVersion = createDefaultCdrVersion();

    accessTierDao.save(dbCdrVersion.getAccessTier());
    dbCdrVersion = cdrVersionDao.save(dbCdrVersion);

    dbUser = new DbUser().setUserId(123L);

    dbWorkspace1 =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceId(1L)
                .setWorkspaceNamespace("workspaceNamespace1")
                .setFirecloudName("firecloudname1")
                .setCdrVersion(dbCdrVersion));

    dbWorkspace2 =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceId(2L)
                .setWorkspaceNamespace("workspaceNamespace2")
                .setFirecloudName("firecloudName2")
                .setCdrVersion(dbCdrVersion));

    dbCohort =
        new DbCohort()
            .setName("Cohort Name")
            .setCreator(dbUser)
            .setCohortId(1L)
            .setDescription("Cohort description")
            .setLastModifiedTime(new Timestamp(fakeClock.millis()))
            .setCreationTime(new Timestamp(fakeClock.millis()));
    stubCohort(dbCohort);

    dbConceptSet =
        new DbConceptSet()
            .setName("Concept Set")
            .setDescription("This is a Condition Concept Set")
            .setConceptSetId(1L)
            .setDomainEnum(Domain.CONDITION)
            .setLastModifiedTime(new Timestamp(fakeClock.millis()))
            .setCreationTime(new Timestamp(fakeClock.millis()));
    stubConceptSet(dbConceptSet, dbWorkspace2.getWorkspaceId());

    dbUserRecentlyModifiedResource1 =
        userRecentlyModifiedResourceDao.save(
            new DbUserRecentlyModifiedResource()
                .setResourceType(
                    DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK)
                .setResourceId("gs://bucketFile/notebooks/notebook1.ipynb")
                .setLastAccessDate(new Timestamp(fakeClock.millis()))
                .setUserId(dbUser.getUserId())
                .setWorkspaceId(dbWorkspace1.getWorkspaceId()));

    dbUserRecentlyModifiedResource2 =
        userRecentlyModifiedResourceDao.save(
            new DbUserRecentlyModifiedResource()
                .setResourceType(
                    DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.COHORT)
                .setResourceId(dbCohort.getCohortId() + "")
                .setLastAccessDate(new Timestamp(fakeClock.millis() - 10000))
                .setUserId(dbUser.getUserId())
                .setWorkspaceId(dbWorkspace2.getWorkspaceId()));

    dbUserRecentlyModifiedResource3 =
        userRecentlyModifiedResourceDao.save(
            new DbUserRecentlyModifiedResource()
                .setResourceType(
                    DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.CONCEPT_SET)
                .setResourceId(dbConceptSet.getConceptSetId() + "")
                .setLastAccessDate(new Timestamp(fakeClock.millis() - 10000))
                .setUserId(dbUser.getUserId())
                .setWorkspaceId(dbWorkspace2.getWorkspaceId()));

    final RawlsWorkspaceDetails fcWorkspace1 = new RawlsWorkspaceDetails();
    fcWorkspace1.setNamespace(dbWorkspace1.getFirecloudName());

    final RawlsWorkspaceDetails fcWorkspace2 = new RawlsWorkspaceDetails();
    fcWorkspace1.setNamespace(dbWorkspace2.getFirecloudName());

    final RawlsWorkspaceResponse workspaceResponse1 = new RawlsWorkspaceResponse();
    workspaceResponse1.setAccessLevel(RawlsWorkspaceAccessLevel.OWNER);
    workspaceResponse1.setWorkspace(fcWorkspace1);

    final RawlsWorkspaceResponse workspaceResponse2 = new RawlsWorkspaceResponse();
    workspaceResponse2.setAccessLevel(RawlsWorkspaceAccessLevel.READER);
    workspaceResponse2.setWorkspace(fcWorkspace2);

    mockResponsesForWorkspace(dbWorkspace1, workspaceResponse1);
    mockResponsesForWorkspace(dbWorkspace2, workspaceResponse2);

    when(mockUserProvider.get()).thenReturn(dbUser);

    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(
            Arrays.asList(
                dbUserRecentlyModifiedResource1,
                dbUserRecentlyModifiedResource2,
                dbUserRecentlyModifiedResource3));

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
            mockCloudStorageClient,
            mockCohortService,
            mockCohortReviewService,
            mockConceptSetService,
            mockDataSetService,
            mockFireCloudService,
            mockUserProvider,
            mockUserRecentResourceService,
            workspaceAuthService,
            workspaceDao,
            workspaceResourceMapper);
    userMetricsController.setDistinctWorkspaceLimit(5);
  }

  private void stubCohort(DbCohort dbCohort) {
    when(mockCohortService.findByCohortId(dbCohort.getCohortId()))
        .thenReturn(Optional.of(dbCohort));
    when(mockCohortService.findByCohortIdOrThrow(dbCohort.getCohortId())).thenReturn(dbCohort);
  }

  private void stubConceptSet(DbConceptSet dbConceptSet, long workspaceId) {
    when(mockConceptSetService.getDbConceptSet(workspaceId, dbConceptSet.getConceptSetId()))
        .thenReturn(dbConceptSet);
    when(mockConceptSetService.maybeGetDbConceptSet(workspaceId, dbConceptSet.getConceptSetId()))
        .thenReturn(Optional.of(dbConceptSet));
  }

  private void mockResponsesForWorkspace(DbWorkspace dbWorkspace, RawlsWorkspaceResponse response) {

    when(mockFireCloudService.getWorkspace(dbWorkspace)).thenReturn(Optional.of(response));

    when(mockFireCloudService.getWorkspace(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName()))
        .thenReturn(response);
  }

  @Test
  public void testGetUserRecentResourceFromRawBucket() {
    dbUserRecentlyModifiedResource1.setResourceId("gs://bucketFile/notebook.ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath()).isEqualTo("gs://bucketFile/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("notebook.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithDuplicatedNameInPath() {
    dbUserRecentlyModifiedResource1.setResourceId("gs://bucketFile/nb.ipynb/intermediate/nb.ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/nb.ipynb/intermediate/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("nb.ipynb");
  }

  @Test
  public void testGetUserRecentResourceWithSpacesInPath() {
    dbUserRecentlyModifiedResource1.setResourceId(
        "gs://bucketFile/note books/My favorite notebook.ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

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
    dbUserRecentlyModifiedResource1.setResourceId("my local notebook directory: notebook.ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();
    assertThat(recentResources.size()).isEqualTo(0);
  }

  @Test
  public void testGetUserRecentResourceNotebookPathEndsWithSlash() {
    dbUserRecentlyModifiedResource1.setResourceId("gs://bucketFile/notebooks/notebook.ipynb/");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources.get(0).getNotebook()).isNotNull();
    assertThat(recentResources.get(0).getNotebook().getPath())
        .isEqualTo("gs://bucketFile/notebooks/notebook.ipynb/");
    assertThat(recentResources.get(0).getNotebook().getName()).isEqualTo("");
  }

  // RW-7498 regression test
  @Test
  public void testGetUserRecentResource_notebookNameWithParen() {
    dbUserRecentlyModifiedResource1.setResourceId("gs://bucketFile/notebooks/notebook :).ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(Collections.singletonList(dbUserRecentlyModifiedResource1));

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources.get(0).getNotebook())
        .isEqualTo(new FileDetail().path("gs://bucketFile/notebooks/").name("notebook :).ipynb"));
  }

  @Test
  public void testGetUserRecentResourceNonexistentNotebook() {
    dbUserRecentlyModifiedResource1.setResourceId("gs://bkt/notebooks/notebook.ipynb");
    dbUserRecentlyModifiedResource2.setResourceType(
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK);
    dbUserRecentlyModifiedResource2.setResourceId("gs://bkt/notebooks/not-found.ipynb");
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(
            ImmutableList.of(dbUserRecentlyModifiedResource1, dbUserRecentlyModifiedResource2));
    when(mockCloudStorageClient.getExistingBlobIdsIn(anyList()))
        .thenReturn(
            ImmutableSet.of(BlobId.of("bkt", NotebookUtils.withNotebookPath("notebook.ipynb"))));

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
    assertThat(recentResources.get(1).getCohort().getName()).isEqualTo(dbCohort.getName());
    assertThat(recentResources.get(2).getConceptSet()).isNotNull();
    assertThat(recentResources.get(2).getConceptSet().getName()).isEqualTo(dbConceptSet.getName());
  }

  @Test
  public void testGetUserRecentResources_missingWorkspace() {
    dbUserRecentlyModifiedResource1.setWorkspaceId(9999); // missing workspace
    when(mockUserRecentResourceService.findAllRecentlyModifiedResourcesByUser(dbUser.getUserId()))
        .thenReturn(
            ImmutableList.of(dbUserRecentlyModifiedResource1, dbUserRecentlyModifiedResource2));
    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources.size()).isEqualTo(1);
    WorkspaceResource foundResource = recentResources.get(0);
    final Cohort cohort1 = foundResource.getCohort();
    final Long cohort2Id = Long.parseLong(dbUserRecentlyModifiedResource2.getResourceId());

    assertThat(cohort1.getId()).isEqualTo(cohort2Id);
    assertThat(foundResource.getWorkspaceId())
        .isEqualTo(dbUserRecentlyModifiedResource2.getWorkspaceId());
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
  public void testUpdateRecentResource() {
    Timestamp now = new Timestamp(fakeClock.instant().toEpochMilli());
    DbUserRecentlyModifiedResource mockUserRecentResource = new DbUserRecentlyModifiedResource();
    mockUserRecentResource.setWorkspaceId(dbWorkspace2.getWorkspaceId());
    mockUserRecentResource.setUserId(dbUser.getUserId());
    mockUserRecentResource.setResourceId("gs://newBucket/notebooks/notebook.ipynb");
    mockUserRecentResource.setResourceType(DbUserRecentlyModifiedResourceType.NOTEBOOK);
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
  public void testGetUserRecentResource_deleted_cohort() {
    // input resources: Notebook, Cohort, Concept Set

    // simulate a deleted cohort
    when(mockCohortService.findByCohortId(dbCohort.getCohortId())).thenReturn(Optional.empty());
    // and confirm that the recent-resource DB entry is not empty
    assertThat(userRecentlyModifiedResourceDao.findById(dbUserRecentlyModifiedResource2.getId()))
        .hasValue(dbUserRecentlyModifiedResource2);

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();

    // expected resources: Notebook and Concept Set (Cohort was deleted)

    assertThat(recentResources.size()).isEqualTo(2);
    assertThat(recentResources.get(0).getNotebook()).isNotNull();
    assertThat(recentResources.get(1).getConceptSet()).isNotNull();
  }

  @Test
  public void testGetUserRecentResource_deleted_conceptSet() {
    // input resources: Notebook, Cohort, Concept Set

    // simulate a deleted concept set
    when(mockConceptSetService.maybeGetDbConceptSet(any(), any())).thenReturn(Optional.empty());
    // and confirm that the recent-resource DB entry is not empty
    assertThat(userRecentlyModifiedResourceDao.findById(dbUserRecentlyModifiedResource3.getId()))
        .hasValue(dbUserRecentlyModifiedResource3);

    WorkspaceResourceResponse recentResources =
        userMetricsController.getUserRecentResources().getBody();
    assertThat(recentResources).isNotNull();

    // expected resources: Notebook and Cohort (Concept Set was deleted)

    assertThat(recentResources.size()).isEqualTo(2);
    assertThat(recentResources.get(0).getNotebook()).isNotNull();
    assertThat(recentResources.get(1).getCohort()).isNotNull();
  }

  @Test
  public void test_isValidResource_IfNotebookNamePresent_nullNotebookName_passes() {
    dbUserRecentlyModifiedResource1.setResourceId(null);
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource1)).isTrue();
  }

  @Test
  public void test_isValidResource_IfNotebookNamePresent_validNotebookName_passes() {
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource1)).isTrue();
  }

  @Test
  public void test_isValidResource_IfNotebookNamePresent_invalidNotebookName_fails() {
    dbUserRecentlyModifiedResource1.setResourceId("invalid-notebook@name");
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource1)).isFalse();
  }

  @Test
  public void test_isValidResource_deleted_cohort_fails() {
    assertThat(dbUserRecentlyModifiedResource2.getResourceType())
        .isEqualTo(DbUserRecentlyModifiedResourceType.COHORT);
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource2)).isTrue();

    when(mockCohortService.findByCohortId(dbCohort.getCohortId())).thenReturn(Optional.empty());
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource2)).isFalse();
  }

  @Test
  public void test_isValidResource_deleted_conceptSet_fails() {
    assertThat(dbUserRecentlyModifiedResource3.getResourceType())
        .isEqualTo(DbUserRecentlyModifiedResourceType.CONCEPT_SET);
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource3)).isTrue();

    when(mockConceptSetService.maybeGetDbConceptSet(
            dbWorkspace2.getWorkspaceId(), dbConceptSet.getConceptSetId()))
        .thenReturn(Optional.empty());
    assertThat(userMetricsController.isValidResource(dbUserRecentlyModifiedResource3)).isFalse();
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
