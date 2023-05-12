package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.leonardo.model.LeonardoAppStatus;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCloudProvider;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesError;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoListAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.model.AppStatus;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.KubernetesError;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(LeonardoMapperImpl.class)
@SpringJUnitConfig
public class LeonardoMapperTest {
  @Autowired private LeonardoMapper mapper;

  private static final String MACHINE_TYPE = "n1-standard-1";
  private static final String APP_NAME = "all-of-us-123-cromwell";
  private static final String DISK_NAME = "disk name";
  private static final String GOOGLE_PROJECT = "google project";

  private UserAppEnvironment app;
  private KubernetesRuntimeConfig kubernetesRuntimeConfig;
  private LeonardoKubernetesRuntimeConfig leonardoKubernetesRuntimeConfig;
  private PersistentDiskRequest persistentDiskRequest;
  private LeonardoPersistentDiskRequest leonardoPersistentDiskRequest;
  private LeonardoAuditInfo leonardoAuditInfo;
  private List<KubernetesError> kubernetesErrors = new ArrayList<>();
  private List<LeonardoKubernetesError> leonardoKubernetesErrors = new ArrayList<>();
  private Map<String, String> proxyUrls = new HashMap<>();
  private Map<String, String> labels = new HashMap<>();

  @BeforeEach
  public void setUp() {
    kubernetesRuntimeConfig =
        new KubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    leonardoKubernetesRuntimeConfig =
        new LeonardoKubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    persistentDiskRequest = new PersistentDiskRequest().diskType(DiskType.STANDARD).size(10);
    leonardoPersistentDiskRequest =
        new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.STANDARD).size(10);
    leonardoAuditInfo =
        new LeonardoAuditInfo()
            .createdDate("2022-10-10")
            .creator("bob@gmail.com")
            .dateAccessed("2022-10-10");

    kubernetesErrors.add(new KubernetesError().errorMessage("error1").googleErrorCode(404));
    kubernetesErrors.add(new KubernetesError().errorMessage("error2").googleErrorCode(401));
    leonardoKubernetesErrors.add(
        new LeonardoKubernetesError().errorMessage("error1").googleErrorCode(404));
    leonardoKubernetesErrors.add(
        new LeonardoKubernetesError().errorMessage("error2").googleErrorCode(401));

    proxyUrls.put("cromwell", "cromwell url");
    proxyUrls.put("rstudio", "rstudio url");
    labels.put("label key 1", "label value 1");
    labels.put("label key 2", "label value 2");

    app =
        new UserAppEnvironment()
            .createdDate("2022-10-10")
            .dateAccessed("2022-10-10")
            .kubernetesRuntimeConfig(kubernetesRuntimeConfig)
            .googleProject("google project")
            .appType(AppType.CROMWELL)
            .errors(kubernetesErrors)
            .proxyUrls(proxyUrls)
            .diskName(DISK_NAME)
            .appName(APP_NAME)
            .status(AppStatus.RUNNING)
            .googleProject(GOOGLE_PROJECT)
            .labels(labels)
            .creator("bob@gmail.com");
  }

  @Test
  public void testToKubernetesRuntimeConfig() {
    assertThat(mapper.toKubernetesRuntimeConfig(leonardoKubernetesRuntimeConfig))
        .isEqualTo(kubernetesRuntimeConfig);
  }

  @Test
  public void testToLeoKubernetesRuntimeConfig() {
    assertThat(mapper.toLeonardoKubernetesRuntimeConfig(kubernetesRuntimeConfig))
        .isEqualTo(leonardoKubernetesRuntimeConfig);
  }

  @Test
  public void testToPersistentDiskRequest() {
    assertThat(mapper.toPersistentDiskRequest(leonardoPersistentDiskRequest))
        .isEqualTo(persistentDiskRequest);
  }

  @Test
  public void testToLeoPersistentDiskRequest() {
    assertThat(mapper.toLeonardoPersistentDiskRequest(persistentDiskRequest))
        .isEqualTo(leonardoPersistentDiskRequest);
  }

  @Test
  public void testToApiAppType() {
    assertThat(mapper.toApiAppType(LeonardoAppType.CROMWELL)).isEqualTo(AppType.CROMWELL);
    assertThat(mapper.toApiAppType(LeonardoAppType.CUSTOM)).isEqualTo(AppType.RSTUDIO);
    assertThat(mapper.toApiAppType(LeonardoAppType.GALAXY)).isNull();
  }

  @Test
  public void testToLeonardoAppType() {
    assertThat(mapper.toLeonardoAppType(AppType.RSTUDIO)).isEqualTo(LeonardoAppType.CUSTOM);
    assertThat(mapper.toLeonardoAppType(AppType.CROMWELL)).isEqualTo(LeonardoAppType.CROMWELL);
  }

  @Test
  public void testToAppFromGetResponse() {
    LeonardoGetAppResponse getAppResponse =
        new LeonardoGetAppResponse()
            .appType(LeonardoAppType.CROMWELL)
            .status(LeonardoAppStatus.RUNNING)
            .auditInfo(leonardoAuditInfo)
            .diskName(DISK_NAME)
            .kubernetesRuntimeConfig(leonardoKubernetesRuntimeConfig)
            .appName(APP_NAME)
            .errors(leonardoKubernetesErrors)
            .proxyUrls(proxyUrls)
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT))
            .labels(labels);
    assertThat(mapper.toApiApp(getAppResponse)).isEqualTo(app);
  }

  @Test
  public void testToAppFromListResponse() {
    LeonardoListAppResponse listAppResponse =
        new LeonardoListAppResponse()
            .appType(LeonardoAppType.CROMWELL)
            .status(LeonardoAppStatus.RUNNING)
            .auditInfo(leonardoAuditInfo)
            .diskName(DISK_NAME)
            .kubernetesRuntimeConfig(leonardoKubernetesRuntimeConfig)
            .errors(leonardoKubernetesErrors)
            .proxyUrls(proxyUrls)
            .labels(labels)
            .appName(APP_NAME)
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT));
    assertThat(mapper.toApiApp(listAppResponse)).isEqualTo(app);
  }

  @Test
  public void testToApiDiskFromListDiskResponse() {
    LeonardoListPersistentDiskResponse listPersistentDiskResponse =
        new LeonardoListPersistentDiskResponse()
            .diskType(LeonardoDiskType.SSD)
            .auditInfo(leonardoAuditInfo)
            .status(LeonardoDiskStatus.READY);

    Disk disk =
        new Disk()
            .diskType(DiskType.SSD)
            .isGceRuntime(true)
            .creator(leonardoAuditInfo.getCreator())
            .dateAccessed(leonardoAuditInfo.getDateAccessed())
            .createdDate(leonardoAuditInfo.getCreatedDate())
            .status(DiskStatus.READY);
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse)).isEqualTo(disk);

    // RSTUDIO
    Map<String, String> rstudioLabel = new HashMap<>();
    rstudioLabel.put(LEONARDO_LABEL_APP_TYPE, "rstudio");
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse.labels(rstudioLabel)))
        .isEqualTo(disk.appType(AppType.RSTUDIO).isGceRuntime(false));
  }

  @Test
  public void testToApiDiskFromGetDiskResponse() {
    LeonardoGetPersistentDiskResponse getPersistentDiskResponse =
        new LeonardoGetPersistentDiskResponse()
            .diskType(LeonardoDiskType.SSD)
            .auditInfo(leonardoAuditInfo)
            .status(LeonardoDiskStatus.READY);

    Disk disk =
        new Disk()
            .diskType(DiskType.SSD)
            .isGceRuntime(true)
            .creator(leonardoAuditInfo.getCreator())
            .dateAccessed(leonardoAuditInfo.getDateAccessed())
            .createdDate(leonardoAuditInfo.getCreatedDate())
            .status(DiskStatus.READY);
    assertThat(mapper.toApiGetDiskResponse(getPersistentDiskResponse)).isEqualTo(disk);

    // RSTUDIO
    Map<String, String> rstudioLabel = new HashMap<>();
    rstudioLabel.put(LEONARDO_LABEL_APP_TYPE, "rstudio");
    assertThat(mapper.toApiGetDiskResponse(getPersistentDiskResponse.labels(rstudioLabel)))
        .isEqualTo(disk.appType(AppType.RSTUDIO).isGceRuntime(false));
  }
}
