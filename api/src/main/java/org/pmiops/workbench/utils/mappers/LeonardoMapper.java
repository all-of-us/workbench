package org.pmiops.workbench.utils.mappers;

import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.labelValueToAppType;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.UpdateDataprocConfig;
import org.broadinstitute.dsde.workbench.client.leonardo.model.UpdateGceConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.DiskType;
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

  DataprocConfig toDataprocConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig leonardoMachineConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "componentGatewayEnabled", ignore = true)
  @Mapping(target = "workerPrivateAccess", ignore = true)
  @Mapping(target = "region", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig toLeonardoDataProcConfig(
      DataprocConfig dataprocConfig);

  @Mapping(target = "cloudService", ignore = true)
  UpdateDataprocConfig toLeonardoUpdateDataProcConfig(DataprocConfig dataprocConfig);

  @AfterMapping
  default void addMachineConfigDefaults(
      @MappingTarget
          org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig
              leonardoMachineConfig) {
    leonardoMachineConfig
        .cloudService(
            org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig.CloudServiceEnum
                .DATAPROC)
        .componentGatewayEnabled(true);
  }

  GceConfig toGceConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig leonardoGceConfig);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "zone", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig toLeonardoGceConfig(
      GceConfig gceConfig);

  @Mapping(target = "persistentDisk", source = "leonardoDiskConfig")
  @Mapping(target = "machineType", source = "leonardoGceConfig.machineType")
  @Mapping(target = "gpuConfig", source = "leonardoGceConfig.gpuConfig")
  GceWithPdConfig toGceWithPdConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig leonardoGceConfig,
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskConfig leonardoDiskConfig);

  @Mapping(target = "cloudService", ignore = true)
  UpdateGceConfig toLeonardoUpdateGceConfig(GceConfig gceConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "diskSize", ignore = true)
  UpdateGceConfig toLeonardoUpdateGceConfig(GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "labels", ignore = true)
  PersistentDiskRequest diskConfigToPersistentDiskRequest(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskConfig leonardoDiskConfig);

  PersistentDiskRequest toPersistentDiskRequest(
      org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
          leonardoPersistentDiskRequest);

  org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
      toLeonardoPersistentDiskRequest(PersistentDiskRequest persistentDiskRequest);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "zone", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfig toLeonardoGceWithPdConfig(
      GceWithPdConfig gceWithPdConfig);

  @AfterMapping
  default void addCloudServiceEnum(
      @MappingTarget
          org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig leonardoGceConfig) {
    leonardoGceConfig.setCloudService(
        org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig.CloudServiceEnum.GCE);
  }

  @AfterMapping
  default void addPdCloudServiceEnum(
      @MappingTarget
          org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfig
              leonardoGceWithPdConfig) {
    leonardoGceWithPdConfig.setCloudService(
        org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfig.CloudServiceEnum
            .GCE);
  }

  @ValueMapping(source = "BALANCED", target = MappingConstants.NULL) // we don't support Balanced
  DiskType toApiDiskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType diskType);

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiGetDiskResponse(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse disk);

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiListDisksResponse(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse disk);

  @SuppressWarnings("unchecked")
  @AfterMapping
  default void getDiskAfterMapper(
      @MappingTarget Disk disk,
      org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse
          leoGetDiskResponse) {
    mapDiskLabelsToDiskAppType(disk, (Map<String, String>) leoGetDiskResponse.getLabels());
  }

  @SuppressWarnings("unchecked")
  @AfterMapping
  default void listDisksAfterMapper(
      @MappingTarget Disk disk,
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse
          leoListDisksResponse) {
    mapDiskLabelsToDiskAppType(disk, (Map<String, String>) leoListDisksResponse.getLabels());
  }

  default void mapDiskLabelsToDiskAppType(Disk disk, Map<String, String> diskLabels) {
    if (diskLabels != null && diskLabels.containsKey(LEONARDO_LABEL_APP_TYPE)) {
      disk.appType(labelValueToAppType(diskLabels.get(LEONARDO_LABEL_APP_TYPE)));
    } else {
      disk.isGceRuntime(true);
    }
  }

  @Mapping(target = "patchInProgress", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse toListRuntimeResponse(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  ListRuntimeResponse toApiListRuntimeResponse(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse
          leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntime(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(target = "errors", ignore = true)
  Runtime toApiRuntime(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime,
      org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse
          leonardoGetRuntimeResponse) {
    mapLabels(runtime, leonardoGetRuntimeResponse.getLabels());
    mapRuntimeConfig(
        runtime,
        leonardoGetRuntimeResponse.getRuntimeConfig(),
        leonardoGetRuntimeResponse.getDiskConfig());
  }

  //  @AfterMapping
  //  default void listRuntimeAfterMapper(
  //      @MappingTarget Runtime runtime,
  // org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse
  // leonardoListRuntimeResponse) {
  //    mapLabels(runtime, leonardoListRuntimeResponse.getLabels());
  //    mapRuntimeConfig(
  //        runtime,
  //        leonardoListRuntimeResponse.getRuntimeConfig(),
  //        leonardoListRuntimeResponse.getDiskConfig()); // TODO JOEL this doesn't exist on
  // RuntimeResponse ?
  //  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "appName", source = "appName")
  @Mapping(target = "googleProject", source = "cloudContext.cloudResource")
  @Mapping(target = "autopauseThreshold", ignore = true)
  UserAppEnvironment toApiApp(GetAppResponse app);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "googleProject", source = "cloudContext.cloudResource")
  UserAppEnvironment toApiApp(ListAppResponse app);

  KubernetesRuntimeConfig toKubernetesRuntimeConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
          leonardoKubernetesRuntimeConfig);

  org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
      toLeonardoKubernetesRuntimeConfig(KubernetesRuntimeConfig kubernetesRuntimeConfig);

  @ValueMapping(source = "RSTUDIO", target = "CUSTOM")
  org.broadinstitute.dsde.workbench.client.leonardo.model.AppType toLeonardoAppType(
      AppType appType);

  @ValueMapping(source = "CUSTOM", target = "RSTUDIO")
  @ValueMapping(source = "GALAXY", target = MappingConstants.NULL) // we don't support Galaxy
  @ValueMapping(source = "WDS", target = MappingConstants.NULL) // we don't support WDS
  AppType toApiAppType(org.broadinstitute.dsde.workbench.client.leonardo.model.AppType appType);

  default void mapLabels(Runtime runtime, Object runtimeLabelsObj) {
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
      Runtime runtime,
      Object runtimeConfigObj,
      @Nullable org.broadinstitute.dsde.workbench.client.leonardo.model.DiskConfig diskConfig) {
    if (runtimeConfigObj == null) {
      return;
    }

    Gson gson = new Gson();
    String runtimeConfigJson = gson.toJson(runtimeConfigObj);
    org.broadinstitute.dsde.workbench.client.leonardo.model.RuntimeConfig runtimeConfig =
        gson.fromJson(
            runtimeConfigJson,
            org.broadinstitute.dsde.workbench.client.leonardo.model.RuntimeConfig.class);

    if (org.broadinstitute.dsde.workbench.client.leonardo.model.RuntimeConfig.CloudServiceEnum
        .DATAPROC
        .equals(runtimeConfig.getCloudService())) {
      runtime.dataprocConfig(
          toDataprocConfig(
              gson.fromJson(
                  runtimeConfigJson,
                  org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig.class)));
    } else if (org.broadinstitute.dsde.workbench.client.leonardo.model.RuntimeConfig
        .CloudServiceEnum.GCE
        .equals(runtimeConfig.getCloudService())) {
      // Unfortunately the discriminator does not allow us to distinguish plain GCE config
      // from GceWithPd; use the diskConfig to help differentiate.
      org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig leonardoGceConfig =
          gson.fromJson(
              runtimeConfigJson,
              org.broadinstitute.dsde.workbench.client.leonardo.model.GceConfig.class);
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

  default RuntimeStatus toApiRuntimeStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ClusterStatus leonardoRuntimeStatus) {
    if (leonardoRuntimeStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(leonardoRuntimeStatus.toString());
  }

  default DiskStatus toApiDiskStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus leonardoDiskStatus) {
    if (leonardoDiskStatus == null) {
      return DiskStatus.UNKNOWN;
    }
    return DiskStatus.fromValue(leonardoDiskStatus.toString());
  }

  default String getJupyterImage(
      List<org.broadinstitute.dsde.workbench.client.leonardo.model.RuntimeImage> images) {
    return images.stream()
        .filter(image -> "Jupyter".equals(image.getImageType()))
        .findFirst()
        .get()
        .getImageUrl();
  }
}
