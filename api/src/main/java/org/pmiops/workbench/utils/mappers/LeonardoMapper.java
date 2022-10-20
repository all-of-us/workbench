package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoClusterError;
import org.pmiops.workbench.leonardo.model.LeonardoDiskConfig;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoListAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.App;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskConfig;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeError;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {

  String LEONARDO_LABEL_AOU = "all-of-us";
  String LEONARDO_LABEL_AOU_CONFIG = "all-of-us-config";
  String LEONARDO_LABEL_CREATED_BY = "created-by";
  String LEONARDO_LABEL_APP_TYPE = "aou-app-type";
  BiMap<RuntimeConfigurationType, String> RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP =
      ImmutableBiMap.of(
          RuntimeConfigurationType.USEROVERRIDE, "user-override",
          RuntimeConfigurationType.GENERALANALYSIS, "preset-general-analysis",
          RuntimeConfigurationType.HAILGENOMICANALYSIS, "preset-hail-genomic-analysis");

  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "componentGatewayEnabled", ignore = true)
  @Mapping(target = "workerPrivateAccess", ignore = true)
  LeonardoMachineConfig toLeonardoMachineConfig(DataprocConfig dataprocConfig);

  @AfterMapping
  default void addMachineConfigDefaults(
      @MappingTarget LeonardoMachineConfig leonardoMachineConfig) {
    leonardoMachineConfig
        .cloudService(LeonardoMachineConfig.CloudServiceEnum.DATAPROC)
        .componentGatewayEnabled(true);
  }

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "zone", ignore = true)
  LeonardoGceConfig toLeonardoGceConfig(GceConfig gceConfig);

  @Mapping(target = "persistentDisk", source = "leonardoDiskConfig")
  @Mapping(target = "machineType", source = "leonardoGceConfig.machineType")
  @Mapping(target = "gpuConfig", source = "leonardoGceConfig.gpuConfig")
  GceWithPdConfig toGceWithPdConfig(
      LeonardoGceConfig leonardoGceConfig, LeonardoDiskConfig leonardoDiskConfig);

  @Mapping(target = "labels", ignore = true)
  PersistentDiskRequest diskConfigToPersistentDiskRequest(LeonardoDiskConfig leonardoDiskConfig);

  PersistentDiskRequest toPersistentDiskRequest(
      LeonardoPersistentDiskRequest leonardoPersistentDiskRequest);

  LeonardoPersistentDiskRequest toLeonardoPersistentDiskRequest(
      PersistentDiskRequest persistentDiskRequest);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "zone", ignore = true)
  LeonardoGceWithPdConfig toLeonardoGceWithPdConfig(GceWithPdConfig gceWithPdConfig);

  @AfterMapping
  default void addCloudServiceEnum(@MappingTarget LeonardoGceConfig leonardoGceConfig) {
    leonardoGceConfig.setCloudService(LeonardoGceConfig.CloudServiceEnum.GCE);
  }

  @AfterMapping
  default void addPdCloudServiceEnum(
      @MappingTarget LeonardoGceWithPdConfig leonardoGceWithPdConfig) {
    leonardoGceWithPdConfig.setCloudService(LeonardoGceWithPdConfig.CloudServiceEnum.GCE);
  }

  DiskConfig toDiskConfig(LeonardoDiskConfig leonardoDiskConfig);

  Disk toApiDisk(LeonardoListPersistentDiskResponse disk);

  @Mapping(target = "patchInProgress", ignore = true)
  LeonardoListRuntimeResponse toListRuntimeResponse(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  ListRuntimeResponse toApiListRuntimeResponse(
      LeonardoListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(target = "errors", ignore = true)
  Runtime toApiRuntime(LeonardoListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(LeonardoClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    mapLabels(runtime, leonardoGetRuntimeResponse.getLabels());
    mapRuntimeConfig(
        runtime,
        leonardoGetRuntimeResponse.getRuntimeConfig(),
        leonardoGetRuntimeResponse.getDiskConfig());
  }

  @AfterMapping
  default void listRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoListRuntimeResponse leonardoListRuntimeResponse) {
    mapLabels(runtime, leonardoListRuntimeResponse.getLabels());
    mapRuntimeConfig(
        runtime,
        leonardoListRuntimeResponse.getRuntimeConfig(),
        leonardoListRuntimeResponse.getDiskConfig());
  }

  @Mapping(target = "createdDate", source = "app.auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "app.auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "appName", source = "appName")
  @Mapping(target = "googleProject", source = "googleProject")
  @Mapping(target = "autopauseThreshold", ignore = true)
  App toApiApp(LeonardoGetAppResponse app);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "autopauseThreshold", ignore = true)
  App toApiApp(LeonardoListAppResponse app);

  @AfterMapping
  default void getAppAfterMapper(
      @MappingTarget App app, LeonardoGetAppResponse leonardoGetAppResponse) {
    app.appName(leonardoGetAppResponse.getAppName())
        .googleProject(leonardoGetAppResponse.getGoogleProject());
    mapAppType(app, leonardoGetAppResponse.getAppName());
  }

  @AfterMapping
  default void listAppAfterMapper(
      @MappingTarget App app, LeonardoListAppResponse leonardoListAppResponse) {
    mapAppType(app, leonardoListAppResponse.getAppName());
  }

  default void mapAppType(App app, String appName) {
    // App name format is all-of-us-{user-id}-{appType}.
    app.appType(AppType.fromValue(appName.substring(appName.lastIndexOf('-') + 1).toUpperCase()));
  }

  KubernetesRuntimeConfig toKubernetesRuntimeConfig(
      LeonardoKubernetesRuntimeConfig leonardoKubernetesRuntimeConfig);

  LeonardoKubernetesRuntimeConfig toLeonardoKubernetesRuntimeConfig(
      KubernetesRuntimeConfig kubernetesRuntimeConfig);

  @ValueMapping(source = "CROMWELL", target = "CROMWELL")
  @ValueMapping(source = "RSTUDIO", target = "CUSTOM")
  LeonardoAppType toLeonardoAppType(AppType appType);

  default void mapLabels(Runtime runtime, Object runtimeLabelsObj) {
    @SuppressWarnings("unchecked")
    final Map<String, String> runtimeLabels = (Map<String, String>) runtimeLabelsObj;
    if (runtimeLabels == null || runtimeLabels.get(LEONARDO_LABEL_AOU_CONFIG) == null) {
      // If there's no label, fall back onto the old behavior where every Runtime was created with a
      // default Dataproc config
      runtime.setConfigurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS);
    } else {
      runtime.setConfigurationType(
          RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
              .inverse()
              .get(runtimeLabels.get(LEONARDO_LABEL_AOU_CONFIG)));
    }
  }

  default void mapRuntimeConfig(
      Runtime runtime, Object runtimeConfigObj, @Nullable LeonardoDiskConfig diskConfig) {
    if (runtimeConfigObj == null) {
      return;
    }

    Gson gson = new Gson();
    String runtimeConfigJson = gson.toJson(runtimeConfigObj);
    LeonardoRuntimeConfig runtimeConfig =
        gson.fromJson(runtimeConfigJson, LeonardoRuntimeConfig.class);

    if (CloudServiceEnum.DATAPROC.equals(runtimeConfig.getCloudService())) {
      runtime.dataprocConfig(
          toDataprocConfig(gson.fromJson(runtimeConfigJson, LeonardoMachineConfig.class)));
    } else if (CloudServiceEnum.GCE.equals(runtimeConfig.getCloudService())) {
      // Unfortunately the discriminator does not allow us to distinguish plain GCE config
      // from GceWithPd; use the diskConfig to help differentiate.
      LeonardoGceConfig leonardoGceConfig =
          gson.fromJson(runtimeConfigJson, LeonardoGceConfig.class);
      if (diskConfig != null) {
        runtime.gceWithPdConfig(toGceWithPdConfig(leonardoGceConfig, diskConfig));
      } else {
        runtime.gceConfig(toGceConfig(leonardoGceConfig));
      }
    } else {
      throw new IllegalArgumentException(
          "Invalid LeonardoGetRuntimeResponse.RuntimeConfig.cloudService : "
              + runtimeConfig.getCloudService());
    }
  }

  default RuntimeStatus toApiRuntimeStatus(LeonardoRuntimeStatus leonardoRuntimeStatus) {
    if (leonardoRuntimeStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(leonardoRuntimeStatus.toString());
  }

  default DiskStatus toApiDiskStatus(LeonardoDiskStatus leonardoDiskStatus) {
    if (leonardoDiskStatus == null) {
      return DiskStatus.UNKNOWN;
    }
    return DiskStatus.fromValue(leonardoDiskStatus.toString());
  }

  default String getJupyterImage(List<LeonardoRuntimeImage> images) {
    return images.stream()
        .filter(image -> "Jupyter".equals(image.getImageType()))
        .findFirst()
        .get()
        .getImageUrl();
  }
}
