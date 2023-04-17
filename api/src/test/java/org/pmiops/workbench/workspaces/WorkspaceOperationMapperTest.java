package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.FakeClockConfiguration.NOW_TIME;

import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.db.model.DbWorkspaceOperation.DbWorkspaceOperationStatus;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.model.WorkspaceOperationStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class WorkspaceOperationMapperTest {
  @Autowired private WorkspaceOperationMapper workspaceOperationMapper;

  @Autowired private WorkspaceMapper workspaceMapper;
  @Autowired private WorkspaceDao workspaceDao;

  @MockBean private FireCloudService mockFirecloudService;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    FakeClockConfiguration.class,
    FireCloudServiceImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceOperationMapperImpl.class,
  })
  @MockBean({
    FirecloudApiClientFactory.class,
    FirecloudRetryHandler.class,
  })
  static class Configuration {}

  @Test
  public void test_toModelWithoutWorkspace() {
    Timestamp time1 = new Timestamp(NOW_TIME - 10000);
    Timestamp time2 = new Timestamp(NOW_TIME - 5000);

    DbWorkspaceOperation dbOperation =
        new DbWorkspaceOperation()
            .setId(1)
            .setCreatorId(2)
            .setWorkspaceId(3L)
            .setStatus(DbWorkspaceOperationStatus.PENDING)
            .setCreationTime(time1)
            .setLastModifiedTime(time2);
    WorkspaceOperation expected =
        new WorkspaceOperation().id(1L).status(WorkspaceOperationStatus.PENDING);
    assertThat(workspaceOperationMapper.toModelWithoutWorkspace(dbOperation)).isEqualTo(expected);
  }

  @Test
  public void test_toModelWithoutWorkspace_null() {
    assertThat(workspaceOperationMapper.toModelWithoutWorkspace(null)).isNull();
  }

  @Test
  public void test_toModelWithWorkspace() {
    Timestamp time1 = new Timestamp(NOW_TIME - 10000);
    Timestamp time2 = new Timestamp(NOW_TIME - 5000);

    DbWorkspace dbWorkspace =
        workspaceDao.save(
            new DbWorkspace().setWorkspaceNamespace("a").setName("b").setFirecloudName("c"));
    Workspace expectedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace);

    mockFirecloudGetWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    DbWorkspaceOperation dbOperation =
        new DbWorkspaceOperation()
            .setId(1)
            .setCreatorId(2)
            .setWorkspaceId(dbWorkspace.getWorkspaceId())
            .setStatus(DbWorkspaceOperationStatus.SUCCESS)
            .setCreationTime(time1)
            .setLastModifiedTime(time2);

    WorkspaceOperation expectedOperation =
        new WorkspaceOperation()
            .id(1L)
            .status(WorkspaceOperationStatus.SUCCESS)
            .workspace(expectedWorkspace);

    assertThat(
            workspaceOperationMapper.toModelWithWorkspace(
                dbOperation, workspaceDao, mockFirecloudService, workspaceMapper))
        .isEqualTo(expectedOperation);
  }

  @Test
  public void test_toModelWithWorkspace_null() {
    Timestamp time1 = new Timestamp(NOW_TIME - 10000);
    Timestamp time2 = new Timestamp(NOW_TIME - 5000);

    DbWorkspaceOperation dbOperation =
        new DbWorkspaceOperation()
            .setId(1)
            .setCreatorId(2)
            .setStatus(DbWorkspaceOperationStatus.SUCCESS)
            .setCreationTime(time1)
            .setLastModifiedTime(time2);

    WorkspaceOperation expectedOperation =
        new WorkspaceOperation().id(1L).status(WorkspaceOperationStatus.SUCCESS);

    assertThat(
            workspaceOperationMapper.toModelWithWorkspace(
                dbOperation, workspaceDao, mockFirecloudService, workspaceMapper))
        .isEqualTo(expectedOperation);
  }

  @Test
  public void test_toModelWithWorkspace_not_found() {
    Timestamp time1 = new Timestamp(NOW_TIME - 10000);
    Timestamp time2 = new Timestamp(NOW_TIME - 5000);

    DbWorkspaceOperation dbOperation =
        new DbWorkspaceOperation()
            .setId(1)
            .setCreatorId(2)
            .setWorkspaceId(-1L)
            .setStatus(DbWorkspaceOperationStatus.SUCCESS)
            .setCreationTime(time1)
            .setLastModifiedTime(time2);

    WorkspaceOperation expectedOperation =
        new WorkspaceOperation().id(1L).status(WorkspaceOperationStatus.SUCCESS);

    assertThat(
            workspaceOperationMapper.toModelWithWorkspace(
                dbOperation, workspaceDao, mockFirecloudService, workspaceMapper))
        .isEqualTo(expectedOperation);
  }

  @Test
  public void test_getWorkspaceMaybe() {
    DbWorkspace dbWorkspace =
        workspaceDao.save(
            new DbWorkspace().setWorkspaceNamespace("a").setName("b").setFirecloudName("c"));
    Workspace expectedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace);

    mockFirecloudGetWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    Optional<Workspace> maybeWorkspace =
        workspaceOperationMapper.getWorkspaceMaybe(
            dbWorkspace.getWorkspaceId(), workspaceDao, mockFirecloudService, workspaceMapper);

    assertThat(maybeWorkspace).hasValue(expectedWorkspace);
  }

  @Test
  public void test_getWorkspaceMaybe_not_found() {
    assertThat(
            workspaceOperationMapper.getWorkspaceMaybe(
                -1L, workspaceDao, mockFirecloudService, workspaceMapper))
        .isEmpty();
  }

  private void mockFirecloudGetWorkspace(String namespace, String firecloudName) {
    RawlsWorkspaceResponse mockResponse =
        new RawlsWorkspaceResponse()
            .workspace(new RawlsWorkspaceDetails().namespace(namespace).name(firecloudName));
    when(mockFirecloudService.getWorkspace(namespace, firecloudName)).thenReturn(mockResponse);
  }
}
