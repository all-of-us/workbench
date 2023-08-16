package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.broadinstitute.dsde.workbench.client.leonardo.model.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
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
  @Mapping(target = "configType", ignore = true)
  @Mapping(target = "additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig toLeonardoMachineConfig(
      DataprocConfig dataprocConfig);

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

  @Mapping(target = "persistentDisk", source = "DiskConfig")
  @Mapping(target = "machineType", source = "leonardoGceConfig.machineType")
  @Mapping(target = "gpuConfig", source = "leonardoGceConfig.gpuConfig")
  GceWithPdConfig toGceWithPdConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfigInResponse
          leonardoGceConfig,
      DiskConfig DiskConfig);

  @Mapping(target = "labels", ignore = true)
  @Mapping(target = "diskType", ignore = true)
  PersistentDiskRequest diskConfigToPersistentDiskRequest(DiskConfig DiskConfig);

  @Mapping(target = "diskType", ignore = true)
  PersistentDiskRequest toPersistentDiskRequest(
      org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
          leonardoPersistentDiskRequest);

  @Mapping(target = "additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
      toLeonardoPersistentDiskRequest(PersistentDiskRequest persistentDiskRequest);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "zone", ignore = true)
  @Mapping(target = "configType", ignore = true)
  @Mapping(target = "additionalProperties", ignore = true)
  @Mapping(target = "gpuConfig.additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfig toLeonardoGceWithPdConfig(
      GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "cloudService", constant = "GCE")
  @Mapping(target = "diskSize", source = "persistentDisk.size")
  @Mapping(target = "additionalProperties", ignore = true)
  UpdateGceConfig toLeonardoUpdateGceConfig(GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "cloudService", constant = "DATAPROC")
  @Mapping(target = "additionalProperties", ignore = true)
  UpdateDataprocConfig toLeonardoUpdateDataprocConfig(DataprocConfig dataprocConfig);

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

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "diskType", ignore = true)
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiGetDiskResponse(GetPersistentDiskResponse disk);

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "diskType", ignore = true)
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiListDisksResponse(ListPersistentDiskResponse disk);

  @AfterMapping
  default void getDiskAfterMapper(
      @MappingTarget Disk disk, GetPersistentDiskResponse leoGetDiskResponse) {
    setDiskEnvironmentType(disk, leoGetDiskResponse.getLabels());
  }

  @AfterMapping
  default void listDisksAfterMapper(
      @MappingTarget Disk disk, ListPersistentDiskResponse leoListDisksResponse) {
    setDiskEnvironmentType(disk, leoListDisksResponse.getLabels());
  }

  default void setDiskEnvironmentType(Disk disk, @Nullable Object diskLabels) {
    LeonardoLabelHelper.maybeMapDiskLabelsToGkeApp(diskLabels)
        .ifPresentOrElse(disk::setAppType, () -> disk.isGceRuntime(true));
  }

  @Mapping(target = "patchInProgress", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse toListRuntimeResponse(
      GetRuntimeResponse runtime);

  @Nullable
  @Named("cloudContextToGoogleProject")
  default String toGoogleProject(@Nullable CloudContext lcc) {
    return Optional.ofNullable(lcc)
        // we don't support CloudProvider.AZURE so don't attempt to map it
        .filter(c -> c.getCloudProvider() == CloudProvider.GCP)
        .map(CloudContext::getCloudResource)
        .orElse(null);
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  ListRuntimeResponse toApiListRuntimeResponse(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse
          ListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  Runtime toApiRuntime(GetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(target = "errors", ignore = true)
  Runtime toApiRuntime(
      org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(ClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, GetRuntimeResponse getRuntimeResponse) {
    mapLabels(runtime, getRuntimeResponse.getLabels());

    mapRuntimeConfig(
        runtime, getRuntimeResponse.getRuntimeConfig(), getRuntimeResponse.getDiskConfig());
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
  UserAppEnvironment toApiApp(GetAppResponse app);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  UserAppEnvironment toApiApp(ListAppResponse app);

  @Mapping(target = "autoscalingEnabled", ignore = true)
  KubernetesRuntimeConfig toKubernetesRuntimeConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
          leonardoKubernetesRuntimeConfig);

  @Mapping(target = "additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
      toLeonardoKubernetesRuntimeConfig(KubernetesRuntimeConfig kubernetesRuntimeConfig);

  @ValueMapping(
      source = "RSTUDIO",
      target = "ALLOWED") // TODO: Update this once we use new leo client to support SAS
  org.broadinstitute.dsde.workbench.client.leonardo.model.AppType toLeonardoAppType(
      AppType appType);

  @ValueMapping(source = "GALAXY", target = MappingConstants.NULL) // we don't support Galaxy
  @ValueMapping(source = "CUSTOM", target = MappingConstants.NULL) // we don't support CUSTOM apps
  @ValueMapping(source = "WDS", target = MappingConstants.NULL) // we don't support WDS apps
  @ValueMapping(
      source = "WORKFLOWS_APP",
      target = MappingConstants.NULL) // we don't support WDS apps
  @ValueMapping(
      source = "CROMWELL_RUNNER_APP",
      target = MappingConstants.NULL) // we don't support WDS apps
  @ValueMapping(
      source = "ALLOWED",
      target = "RSTUDIO") // TODO: Update this once we use new leo client to support SAS
  @ValueMapping(
      source = "HAIL_BATCH",
      target = MappingConstants.NULL) // we don't support HAIL_BATCH apps
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
      OneOfRuntimeConfigInResponse runtimeConfigObj,
      @Nullable DiskConfig diskConfig) {
    if (runtimeConfigObj == null) {
      return;
    }

    if (runtimeConfigObj.getActualInstance()
        instanceof org.broadinstitute.dsde.workbench.client.leonardo.model.DataprocConfig) {
      runtime.dataprocConfig(toDataprocConfig(runtimeConfigObj.getDataprocConfig()));
    } else if (runtimeConfigObj.getActualInstance()
        instanceof
        org.broadinstitute.dsde.workbench.client.leonardo.model.GceWithPdConfigInResponse) {
      runtime.gceWithPdConfig(
          toGceWithPdConfig(runtimeConfigObj.getGceWithPdConfigInResponse(), diskConfig));
    } else {
      throw new IllegalArgumentException(
          "Invalid GetRuntimeResponse.RuntimeConfig.cloudService : " + runtimeConfigObj);
    }
  }

  default RuntimeStatus toApiRuntimeStatus(ClusterStatus ClusterStatus) {
    if (ClusterStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(ClusterStatus.toString());
  }

  default DiskStatus toApiDiskStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus leonardoDiskStatus) {
    if (leonardoDiskStatus == null) {
      return DiskStatus.UNKNOWN;
    }
    return DiskStatus.fromValue(leonardoDiskStatus.toString());
  }

  default String getJupyterImage(List<RuntimeImage> images) {
    return images.stream()
        .filter(image -> "Jupyter".equals(image.getImageType()))
        .findFirst()
        .get()
        .getImageUrl();
  }
}
