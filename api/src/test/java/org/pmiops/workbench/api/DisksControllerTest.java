package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoApiClient;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateDiskRequest;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class DisksControllerTest {
  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String WORKSPACE_ID = "myfirstworkspace";
  private static final String WORKSPACE_NAME = "My First Workspace";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final Instant NOW = Instant.now();

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  @TestConfiguration
  @Import({
    DisksController.class,
    FakeClockConfiguration.class,
    LeonardoApiHelper.class,
    LeonardoMapperImpl.class,
    NewLeonardoMapperImpl.class,
  })
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }
  }

  @Captor private ArgumentCaptor<LeonardoUpdateDiskRequest> updateDiskRequestCaptor;

  @MockBean NewLeonardoApiClient mockNewLeonardoApiClient;
  @MockBean WorkspaceService mockWorkspaceService;

  @Autowired UserDao userDao;
  @Autowired DisksController disksController;

  @BeforeEach
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();

    user = new DbUser();
    user.setUsername(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);

    DbWorkspace testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID);
    doReturn(testWorkspace).when(mockWorkspaceService).lookupWorkspaceByNamespace((WORKSPACE_NS));
  }

  @Test
  public void test_getDisk() {
    String createDate = "2021-08-06T16:57:29.827954Z";
    String pdName = "pdName";
    GetPersistentDiskResponse getResponse =
        new GetPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .status(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY)
            .auditInfo(new AuditInfo().createdDate(createDate).creator(user.getUsername()));

    Disk disk =
        new Disk()
            .name(pdName)
            .size(300)
            .diskType(DiskType.STANDARD)
            .status(DiskStatus.READY)
            .createdDate(createDate)
            .creator(user.getUsername())
            .isGceRuntime(true);

    when(mockNewLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, pdName))
        .thenReturn(getResponse);
    assertThat(disksController.getDisk(WORKSPACE_NS, pdName).getBody()).isEqualTo(disk);
  }

  @Test
  public void test_getDisk_nullProject() {
    String createDate = "2021-08-06T16:57:29.827954Z";
    String pdName = "pdName";
    GetPersistentDiskResponse getResponse =
        new GetPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .status(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY)
            .auditInfo(new AuditInfo().createdDate(createDate).creator(user.getUsername()));

    when(mockNewLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, pdName))
        .thenReturn(getResponse);

    when(mockWorkspaceService.lookupWorkspaceByNamespace(null)).thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> disksController.getDisk(null, pdName));
  }

  @Test
  public void test_getDisk_nullDisk() {
    when(mockNewLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, null))
        .thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> disksController.getDisk(WORKSPACE_NS, null));
  }

  @Test
  public void test_listDisksInWorkspace() throws ApiException {
    // RStudio Disk: 3 are active, returns the newest one.
    ListPersistentDiskResponse oldRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY,
            NOW.minusSeconds(100).toString(),
            AppType.RSTUDIO);
    ListPersistentDiskResponse newestRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY,
            NOW.toString(),
            AppType.RSTUDIO);
    ListPersistentDiskResponse olderRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY,
            NOW.minusSeconds(200).toString(),
            AppType.RSTUDIO);
    Disk expectedRStudioDisk =
        newDisk(
            newestRstudioDisk.getName(),
            DiskStatus.READY,
            newestRstudioDisk.getAuditInfo().getCreatedDate(),
            AppType.RSTUDIO);

    // GCE Disk: 3 disks in total, 2 are active, newer one is inactive, returns the most recent
    // active ones.
    ListPersistentDiskResponse olderGceDisk =
        newListPdResponse(
            user.generatePDName(),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY,
            NOW.minusMillis(200).toString(),
            null);
    ListPersistentDiskResponse oldGceDisk =
        newListPdResponse(
            user.generatePDName(),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY,
            NOW.minusMillis(100).toString(),
            null);
    ListPersistentDiskResponse newerInactiveGceDisk =
        newListPdResponse(
            user.generatePDName(),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.DELETING,
            NOW.toString(),
            null);
    Disk expectedGceDisk =
        newDisk(
            oldGceDisk.getName(),
            DiskStatus.READY,
            oldGceDisk.getAuditInfo().getCreatedDate(),
            null);

    // Cromwell Disk: both are inactive, nothing to return.
    ListPersistentDiskResponse oldInactiveCromwellDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.DELETING,
            NOW.minusMillis(100).toString(),
            AppType.CROMWELL);
    ListPersistentDiskResponse newerCromwellDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.DELETED,
            NOW.toString(),
            AppType.CROMWELL);

    when(mockNewLeonardoApiClient.listPersistentDiskByProjectCreatedByCreator(
            GOOGLE_PROJECT_ID, false))
        .thenReturn(
            ImmutableList.of(
                oldRstudioDisk,
                newestRstudioDisk,
                olderRstudioDisk,
                olderGceDisk,
                oldGceDisk,
                newerInactiveGceDisk,
                oldInactiveCromwellDisk,
                newerCromwellDisk));

    assertThat(disksController.listDisksInWorkspace(WORKSPACE_NS).getBody())
        .containsExactly(expectedGceDisk, expectedRStudioDisk);
  }

  @Test
  public void updateDisk() throws ApiException {
    int diskSize = 200;
    String diskName = user.generatePDName();
    disksController.updateDisk(WORKSPACE_NS, diskName, diskSize);
    verify(mockNewLeonardoApiClient).updatePersistentDisk(GOOGLE_PROJECT_ID, diskName, diskSize);
  }

  @Test
  public void updateDisk_notFound() {
    int diskSize = 200;
    String diskName = user.generatePDName();

    doThrow(new NotFoundException())
        .when(mockNewLeonardoApiClient)
        .updatePersistentDisk(GOOGLE_PROJECT_ID, diskName, diskSize);

    assertThrows(
        NotFoundException.class,
        () -> disksController.updateDisk(WORKSPACE_NS, diskName, diskSize));
  }

  @Test
  public void updateDisk_workspaceNotFound() {
    int diskSize = 200;
    String diskName = user.generatePDName();

    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS))
        .thenThrow(new NotFoundException());

    assertThrows(
        NotFoundException.class,
        () -> disksController.updateDisk(WORKSPACE_NS, diskName, diskSize));
  }

  @Test
  public void deleteDisk() throws ApiException {
    String diskName = user.generatePDName();
    disksController.deleteDisk(WORKSPACE_NS, diskName);
    verify(mockNewLeonardoApiClient).deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);
  }

  @Test
  public void deleteDisk_notFound() {
    String diskName = user.generatePDName();

    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS))
        .thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> disksController.deleteDisk(WORKSPACE_NS, diskName));
  }

  @Test
  public void deleteDisk_workspaceNotFound() {
    String diskName = user.generatePDName();

    doThrow(new NotFoundException())
        .when(mockNewLeonardoApiClient)
        .deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);

    assertThrows(NotFoundException.class, () -> disksController.deleteDisk(WORKSPACE_NS, diskName));
  }

  private ListPersistentDiskResponse newListPdResponse(
      String pdName,
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus status,
      String date,
      @Nullable AppType appType) {
    ListPersistentDiskResponse response =
        new ListPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .status(status)
            .auditInfo(new AuditInfo().createdDate(date).creator(user.getUsername()));
    if (appType != null) {
      Map<String, String> label = new HashMap<>();
      label.put(LEONARDO_LABEL_APP_TYPE, appType.toString().toLowerCase());
      response.labels(label);
    }
    return response;
  }

  private Disk newDisk(String pdName, DiskStatus status, String date, @Nullable AppType appType) {

    Disk disk =
        new Disk()
            .name(pdName)
            .size(300)
            .diskType(DiskType.STANDARD)
            .status(status)
            .createdDate(date)
            .creator(user.getUsername());
    if (appType != null) {
      disk.appType(appType);
    } else {
      disk.isGceRuntime(true);
    }
    return disk;
  }
}
