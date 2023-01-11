package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.HashMap;
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
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
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
  public void testGetPD() throws ApiException {
    String createDate = "2021-08-06T16:57:29.827954Z";
    String pdName = "pdName";
    LeonardoGetPersistentDiskResponse getResponse =
        new LeonardoGetPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(LeonardoDiskType.STANDARD)
            .status(LeonardoDiskStatus.READY)
            .auditInfo(new LeonardoAuditInfo().createdDate(createDate).creator(user.getUsername()))
            .googleProject(GOOGLE_PROJECT_ID);

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
  public void testListPD() throws ApiException {
    // RStudio Disk: 3 are active, returns the newest one.
    LeonardoListPersistentDiskResponse oldRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
            NOW.minusSeconds(100).toString(),
            AppType.RSTUDIO);
    LeonardoListPersistentDiskResponse newestRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
            NOW.toString(),
            AppType.RSTUDIO);
    LeonardoListPersistentDiskResponse olderRstudioDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            LeonardoDiskStatus.READY,
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
    LeonardoListPersistentDiskResponse olderGceDisk =
        newListPdResponse(
            user.generatePDName(), LeonardoDiskStatus.READY, NOW.minusMillis(200).toString(), null);
    LeonardoListPersistentDiskResponse oldGceDisk =
        newListPdResponse(
            user.generatePDName(), LeonardoDiskStatus.READY, NOW.minusMillis(100).toString(), null);
    LeonardoListPersistentDiskResponse newerInactiveGceDisk =
        newListPdResponse(user.generatePDName(), LeonardoDiskStatus.DELETING, NOW.toString(), null);
    Disk expectedGceDisk =
        newDisk(
            oldGceDisk.getName(),
            DiskStatus.READY,
            oldGceDisk.getAuditInfo().getCreatedDate(),
            null);

    // Cromwell Disk: both are inactive, nothing to return.
    LeonardoListPersistentDiskResponse oldInactiveCromwellDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            LeonardoDiskStatus.DELETING,
            NOW.minusMillis(100).toString(),
            AppType.CROMWELL);
    LeonardoListPersistentDiskResponse newerCromwellDisk =
        newListPdResponse(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            LeonardoDiskStatus.DELETED,
            NOW.toString(),
            AppType.CROMWELL);

    when(mockLeonardoApiClient.listPersistentDiskByProject(GOOGLE_PROJECT_ID, false))
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
  public void testListPD_nameNotMatchingPrefix() throws ApiException {
    LeonardoListPersistentDiskResponse rstudioDisk =
        newListPdResponse(
            "rstudio1",
            LeonardoDiskStatus.READY,
            NOW.minusSeconds(100).toString(),
            AppType.RSTUDIO);

    rstudioDisk.auditInfo(rstudioDisk.getAuditInfo().creator("other@gmail.com"));
    when(mockLeonardoApiClient.listPersistentDiskByProject(GOOGLE_PROJECT_ID, false))
        .thenReturn(ImmutableList.of(rstudioDisk));

    assertThat(disksController.listDisksInWorkspace(WORKSPACE_NS).getBody()).isEmpty();
  }

  @Test
  public void testUpdateDisk() throws ApiException {
    int diskSize = 200;
    String diskName = user.generatePDName();
    disksController.updateDisk(WORKSPACE_NS, diskName, diskSize);
    verify(mockLeonardoApiClient).updatePersistentDisk(GOOGLE_PROJECT_ID, diskName, diskSize);
  }

  @Test
  public void testDeleteDisk() throws ApiException {
    String diskName = user.generatePDName();
    disksController.deleteDisk(WORKSPACE_NS, diskName);
    verify(mockLeonardoApiClient).deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);
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
            .googleProject(GOOGLE_PROJECT_ID);
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
