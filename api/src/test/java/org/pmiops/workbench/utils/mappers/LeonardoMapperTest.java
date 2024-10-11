package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAllowedChartName;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAppStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAppType;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAuditInfo;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetAppResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoKubernetesError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListAppResponse;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
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
  private org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
      leonardoPersistentDiskRequest;
  private org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo leonardoAuditInfo;
  private LeonardoAuditInfo legacyLeonardoAuditInfo;
  private List<KubernetesError> kubernetesErrors = new ArrayList<>();
  private List<LeonardoKubernetesError> leonardoKubernetesErrors = new ArrayList<>();
  private Map<String, String> proxyUrls = new HashMap<>();
  private Map<String, String> labels = new HashMap<>();

  private static Stream<Map.Entry<AppType, LeonardoAppType>> allAppTypesMap() {
    return Stream.of(
        Map.entry(AppType.CROMWELL, LeonardoAppType.CROMWELL),
        Map.entry(AppType.RSTUDIO, LeonardoAppType.ALLOWED),
        Map.entry(AppType.SAS, LeonardoAppType.ALLOWED));
  }

  @BeforeEach
  public void setUp() {
    kubernetesRuntimeConfig =
        new KubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    leonardoKubernetesRuntimeConfig =
        new LeonardoKubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    persistentDiskRequest = new PersistentDiskRequest().diskType(DiskType.STANDARD).size(10);
    leonardoPersistentDiskRequest =
        new org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest()
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .size(10);
    leonardoAuditInfo =
        new AuditInfo()
            .createdDate("2022-10-10")
            .creator("bob@gmail.com")
            .dateAccessed("2022-10-10");
    legacyLeonardoAuditInfo =
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
  public void testToLeoPersistentDiskRequest() {
    assertThat(mapper.toLeonardoPersistentDiskRequest(persistentDiskRequest))
        .isEqualTo(leonardoPersistentDiskRequest);
  }

  @Test
  public void testToLeonardoAppType() {
    assertThat(mapper.toLeonardoAppType(AppType.RSTUDIO)).isEqualTo(LeonardoAppType.ALLOWED);
    assertThat(mapper.toLeonardoAppType(AppType.SAS)).isEqualTo(LeonardoAppType.ALLOWED);
    assertThat(mapper.toLeonardoAppType(AppType.CROMWELL)).isEqualTo(LeonardoAppType.CROMWELL);
  }

  @Test
  public void testToLeonardoAllowedAppChart() {
    assertThat(mapper.toLeonardoAllowedChartName(AppType.RSTUDIO))
        .isEqualTo(LeonardoAllowedChartName.RSTUDIO);
    assertThat(mapper.toLeonardoAllowedChartName(AppType.SAS))
        .isEqualTo(LeonardoAllowedChartName.SAS);
    assertThat(mapper.toLeonardoAllowedChartName(AppType.CROMWELL)).isNull();
  }

  @ParameterizedTest(name = "appType {0} can be mapped for getApp call")
  @MethodSource("allAppTypesMap")
  public void testToAppFromGetResponse(Map.Entry<AppType, LeonardoAppType> appTypeMapEntry) {
    labels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appTypeMapEntry.getKey()));
    LeonardoGetAppResponse getAppResponse =
        new LeonardoGetAppResponse()
            .appType(appTypeMapEntry.getValue())
            .status(LeonardoAppStatus.RUNNING)
            .auditInfo(legacyLeonardoAuditInfo)
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
    assertThat(mapper.toApiApp(getAppResponse)).isEqualTo(app.appType(appTypeMapEntry.getKey()));
  }

  @ParameterizedTest(name = "appType {0} can be mapped for listApp call")
  @MethodSource("allAppTypesMap")
  public void testToAppFromListResponse(Map.Entry<AppType, LeonardoAppType> appTypeMapEntry) {
    labels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appTypeMapEntry.getKey()));
    LeonardoListAppResponse listAppResponse =
        new LeonardoListAppResponse()
            .appType(appTypeMapEntry.getValue())
            .status(LeonardoAppStatus.RUNNING)
            .auditInfo(legacyLeonardoAuditInfo)
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
    assertThat(mapper.toApiApp(listAppResponse)).isEqualTo(app.appType(appTypeMapEntry.getKey()));
  }

  @Test
  public void testToApiDiskFromListDiskResponse() {
    ListPersistentDiskResponse listPersistentDiskResponse =
        new ListPersistentDiskResponse()
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.SSD)
            .auditInfo(leonardoAuditInfo)
            .status(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus.READY);

    Disk disk =
        new Disk()
            .diskType(DiskType.SSD)
            .gceRuntime(true)
            .creator(legacyLeonardoAuditInfo.getCreator())
            .dateAccessed(legacyLeonardoAuditInfo.getDateAccessed())
            .createdDate(legacyLeonardoAuditInfo.getCreatedDate())
            .status(DiskStatus.READY);
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse)).isEqualTo(disk);

    // RSTUDIO
    Map<String, String> rstudioLabel = new HashMap<>();
    rstudioLabel.put(LEONARDO_LABEL_APP_TYPE, "rstudio");
    assertThat(mapper.toApiListDisksResponse(listPersistentDiskResponse.labels(rstudioLabel)))
        .isEqualTo(disk.appType(AppType.RSTUDIO).gceRuntime(false));
  }
}
