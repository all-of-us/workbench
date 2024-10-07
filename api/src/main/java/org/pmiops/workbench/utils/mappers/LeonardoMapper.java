package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.legacy_leonardo_client.LeonardoLabelHelper;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAllowedChartName;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAppType;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoClusterError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetAppResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListAppResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoMachineConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeImage;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
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
import org.pmiops.workbench.model.UserAppEnvironment;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {

  BiMap<RuntimeConfigurationType, String> RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP =
      ImmutableBiMap.of(
          RuntimeConfigurationType.USEROVERRIDE, "user-override",
          RuntimeConfigurationType.GENERALANALYSIS, "preset-general-analysis",
          RuntimeConfigurationType.HAILGENOMICANALYSIS, "preset-hail-genomic-analysis");

  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "workerPrivateAccess", ignore = true)
  @Mapping(target = "cloudService", constant = "DATAPROC")
  @Mapping(target = "componentGatewayEnabled", constant = "true")
  LeonardoMachineConfig toLeonardoMachineConfig(DataprocConfig dataprocConfig);

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "zone", ignore = true)
  @Mapping(target = "cloudService", constant = "GCE")
  LeonardoGceConfig toLeonardoGceConfig(GceConfig gceConfig);

  @Mapping(target = "cloudService", constant = "GCE")
  LeonardoUpdateGceConfig toUpdateGceConfig(GceConfig gceConfig);

  @Mapping(target = "cloudService", constant = "GCE")
  @Mapping(target = "diskSize", source = "gceWithPdConfig.persistentDisk.size")
  LeonardoUpdateGceConfig toUpdateGceConfig(GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "cloudService", constant = "DATAPROC")
  LeonardoUpdateDataprocConfig toUpdateDataprocConfig(DataprocConfig dataprocConfig);

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
  @Mapping(target = "zone", ignore = true)
  @Mapping(target = "cloudService", constant = "GCE")
  LeonardoGceWithPdConfig toLeonardoGceWithPdConfig(GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "gceRuntime", ignore = true)
  Disk toApiListDisksResponse(LeonardoListPersistentDiskResponse disk);

  @AfterMapping
  default void listDisksAfterMapper(
      @MappingTarget Disk disk, LeonardoListPersistentDiskResponse leoListDisksResponse) {
    setDiskEnvironmentType(disk, leoListDisksResponse.getLabels());
  }

  default void setDiskEnvironmentType(Disk disk, @Nullable Object diskLabels) {
    LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(diskLabels)
        .ifPresentOrElse(disk::setAppType, () -> disk.gceRuntime(true));
  }

  @Mapping(target = "patchInProgress", ignore = true)
  LeonardoListRuntimeResponse toListRuntimeResponse(LeonardoGetRuntimeResponse runtime);

  @Nullable
  @Named("cloudContextToGoogleProject")
  default String toGoogleProject(@Nullable LeonardoCloudContext lcc) {
    return Optional.ofNullable(lcc)
        // we don't support LeonardoCloudProvider.AZURE so don't attempt to map it
        .filter(c -> c.getCloudProvider() == LeonardoCloudProvider.GCP)
        .map(LeonardoCloudContext::getCloudResource)
        .orElse(null);
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  ListRuntimeResponse toApiListRuntimeResponse(
      LeonardoListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(target = "errors", ignore = true)
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  Runtime toApiRuntime(LeonardoListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(LeonardoClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    mapRuntimeLabels(runtime, leonardoGetRuntimeResponse.getLabels());
    mapRuntimeConfig(
        runtime,
        leonardoGetRuntimeResponse.getRuntimeConfig(),
        leonardoGetRuntimeResponse.getDiskConfig());
  }

  @AfterMapping
  default void listRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoListRuntimeResponse leonardoListRuntimeResponse) {
    mapRuntimeLabels(runtime, leonardoListRuntimeResponse.getLabels());
    mapRuntimeConfig(
        runtime,
        leonardoListRuntimeResponse.getRuntimeConfig(),
        leonardoListRuntimeResponse.getDiskConfig());
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "appName", source = "appName")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "appType", ignore = true)
  UserAppEnvironment toApiApp(LeonardoGetAppResponse app);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "appType", ignore = true)
  UserAppEnvironment toApiApp(LeonardoListAppResponse app);

  KubernetesRuntimeConfig toKubernetesRuntimeConfig(
      LeonardoKubernetesRuntimeConfig leonardoKubernetesRuntimeConfig);

  LeonardoKubernetesRuntimeConfig toLeonardoKubernetesRuntimeConfig(
      KubernetesRuntimeConfig kubernetesRuntimeConfig);

  // SAS and RStudio apps are implemented as ALLOWED Helm Charts
  @ValueMapping(source = "RSTUDIO", target = "ALLOWED")
  @ValueMapping(source = "SAS", target = "ALLOWED")
  LeonardoAppType toLeonardoAppType(AppType appType);

  @ValueMapping(source = "RSTUDIO", target = "RSTUDIO")
  @ValueMapping(source = "SAS", target = "SAS")
  @ValueMapping(source = "CROMWELL", target = MappingConstants.NULL)
  LeonardoAllowedChartName toLeonardoAllowedChartName(AppType appType);

  @AfterMapping
  default void listAppsAfterMapper(
      @MappingTarget UserAppEnvironment appEnvironment, LeonardoListAppResponse listAppResponse) {
    setAppType(appEnvironment, listAppResponse.getLabels());
  }

  @AfterMapping
  default void getAppAfterMapper(
      @MappingTarget UserAppEnvironment appEnvironment, LeonardoGetAppResponse getAppResponse) {
    setAppType(appEnvironment, getAppResponse.getLabels());
  }

  default void setAppType(UserAppEnvironment appEnvironment, @Nullable Object appLabels) {
    appEnvironment.setAppType(
        LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(appLabels)
            .orElseThrow(
                () ->
                    new ServerErrorException(
                        String.format(
                            "Missing app type labels for app with labels %s", appLabels))));
  }

  default void mapRuntimeLabels(Runtime runtime, Object runtimeLabelsObj) {
    @SuppressWarnings("unchecked")
    final Map<String, String> runtimeLabels = (Map<String, String>) runtimeLabelsObj;
    if (runtimeLabels == null
        || runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG) == null) {
      // If there's no label, fall back onto the old behavior where every Runtime was created with a
      // default Dataproc config
      runtime.setConfigurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS);
    } else {
      runtime.setConfigurationType(
          RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
              .inverse()
              .get(runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG)));
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
