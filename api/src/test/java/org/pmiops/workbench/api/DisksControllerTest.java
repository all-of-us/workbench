package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.utils.TestMockFactory.createLeonardoListPersistentDiskResponse;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCloudProvider;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
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

  @MockBean LeonardoApiClient mockLeonardoApiClient;
  @MockBean WorkspaceService mockWorkspaceService;
  @MockBean DiskService mockDiskService;

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
    LeonardoGetPersistentDiskResponse getResponse =
        new LeonardoGetPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(LeonardoDiskType.STANDARD)
            .status(LeonardoDiskStatus.READY)
            .auditInfo(new LeonardoAuditInfo().createdDate(createDate).creator(user.getUsername()))
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID));

    Disk disk =
        new Disk()
            .name(pdName)
            .size(300)
            .diskType(DiskType.STANDARD)
            .status(DiskStatus.READY)
            .createdDate(createDate)
            .creator(user.getUsername())
            .isGceRuntime(true);

    when(mockLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, pdName))
        .thenReturn(getResponse);
    assertThat(disksController.getDisk(WORKSPACE_NS, pdName).getBody()).isEqualTo(disk);
  }

  @Test
  public void test_getDisk_nullProject() {
    String createDate = "2021-08-06T16:57:29.827954Z";
    String pdName = "pdName";
    LeonardoGetPersistentDiskResponse getResponse =
        new LeonardoGetPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(LeonardoDiskType.STANDARD)
            .status(LeonardoDiskStatus.READY)
            .auditInfo(new LeonardoAuditInfo().createdDate(createDate).creator(user.getUsername()))
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID));

    when(mockLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, pdName))
        .thenReturn(getResponse);

    when(mockWorkspaceService.lookupWorkspaceByNamespace(null)).thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> disksController.getDisk(null, pdName));
  }

  @Test
  public void test_getDisk_nullDisk() {
    when(mockLeonardoApiClient.getPersistentDisk(GOOGLE_PROJECT_ID, null))
        .thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> disksController.getDisk(WORKSPACE_NS, null));
  }

  @Test
  public void test_listOwnedDisksInWorkspace() {
    // RStudio Disk: 3 are active, returns the newest one.
    LeonardoListPersistentDiskResponse oldRstudioDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
            NOW.minusSeconds(100).toString(),
            AppType.RSTUDIO,
            GOOGLE_PROJECT_ID,
            user);
    LeonardoListPersistentDiskResponse newestRstudioDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
            NOW.toString(),
            AppType.RSTUDIO,
            GOOGLE_PROJECT_ID,
            user);
    LeonardoListPersistentDiskResponse olderRstudioDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
            NOW.minusSeconds(200).toString(),
            AppType.RSTUDIO,
            GOOGLE_PROJECT_ID,
            user);
    Disk expectedRStudioDisk =
        newDisk(
            newestRstudioDisk.getName(),
            DiskStatus.READY,
            newestRstudioDisk.getAuditInfo().getCreatedDate(),
            AppType.RSTUDIO);

    // GCE Disk: 3 disks in total, 2 are active, newer one is inactive, returns the most recent
    // active ones.
    LeonardoListPersistentDiskResponse olderGceDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.READY,
            NOW.minusMillis(200).toString(),
            null,
            GOOGLE_PROJECT_ID,
            user);
    LeonardoListPersistentDiskResponse oldGceDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.READY,
            NOW.minusMillis(100).toString(),
            null,
            GOOGLE_PROJECT_ID,
            user);
    LeonardoListPersistentDiskResponse newerInactiveGceDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.DELETING,
            NOW.toString(),
            null,
            GOOGLE_PROJECT_ID,
            user);
    Disk expectedGceDisk =
        newDisk(
            oldGceDisk.getName(),
            DiskStatus.READY,
            oldGceDisk.getAuditInfo().getCreatedDate(),
            null);

    // Cromwell Disk: both are inactive, nothing to return.
    LeonardoListPersistentDiskResponse oldInactiveCromwellDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            LeonardoDiskStatus.DELETING,
            NOW.minusMillis(100).toString(),
            AppType.CROMWELL,
            GOOGLE_PROJECT_ID,
            user);
    LeonardoListPersistentDiskResponse newerCromwellDisk =
        createLeonardoListPersistentDiskResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            LeonardoDiskStatus.DELETED,
            NOW.toString(),
            AppType.CROMWELL,
            GOOGLE_PROJECT_ID,
            user);

    when(mockLeonardoApiClient.listPersistentDiskByProjectCreatedByCreator(
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

    assertThat(disksController.listOwnedDisksInWorkspace(WORKSPACE_NS).getBody())
        .containsExactly(expectedGceDisk, expectedRStudioDisk);
  }

  @Test
  public void updateDisk() {
    int diskSize = 200;
    String diskName = user.generatePDName();
    disksController.updateDisk(WORKSPACE_NS, diskName, diskSize);
    verify(mockLeonardoApiClient).updatePersistentDisk(GOOGLE_PROJECT_ID, diskName, diskSize);
  }

  @Test
  public void updateDisk_notFound() {
    int diskSize = 200;
    String diskName = user.generatePDName();

    doThrow(new NotFoundException())
        .when(mockLeonardoApiClient)
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
  public void deleteDisk() {
    String diskName = user.generatePDName();
    disksController.deleteDisk(WORKSPACE_NS, diskName);
    verify(mockLeonardoApiClient).deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);
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
        .when(mockLeonardoApiClient)
        .deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);

    assertThrows(NotFoundException.class, () -> disksController.deleteDisk(WORKSPACE_NS, diskName));
  }

  @Test
  public void listDisksInWorkspace() {
    Disk rStudioDisk =
        newDisk(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            DiskStatus.DELETING,
            NOW.minusMillis(100).toString(),
            AppType.RSTUDIO);

    Disk cromwellDisk =
        newDisk(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            DiskStatus.READY,
            NOW.toString(),
            AppType.CROMWELL);

    List<Disk> serviceResponse = new ArrayList<>(Arrays.asList(rStudioDisk, cromwellDisk));

    when(mockDiskService.findByWorkspaceNamespace(anyString())).thenReturn(serviceResponse);
    assertThat(disksController.listDisksInWorkspace(WORKSPACE_NS).getBody())
        .containsExactly(rStudioDisk, cromwellDisk);
  }

  private LeonardoListPersistentDiskResponse newListPdResponse(
      String pdName, LeonardoDiskStatus status, String date, @Nullable AppType appType) {
    LeonardoListPersistentDiskResponse response =
        new LeonardoListPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(LeonardoDiskType.STANDARD)
            .status(status)
            .auditInfo(new LeonardoAuditInfo().createdDate(date).creator(user.getUsername()))
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID));
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
