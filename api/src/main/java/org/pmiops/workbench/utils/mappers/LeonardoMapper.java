package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AllowedChartName;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoClusterError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoMachineConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeImage;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.model.AdminRuntimeFields;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeError;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.TQSafeDiskStatus;
import org.pmiops.workbench.model.TQSafeDiskType;
import org.pmiops.workbench.model.TaskQueueDisk;
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

  @Mapping(target = "additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
      toLeonardoPersistentDiskRequest(PersistentDiskRequest persistentDiskRequest);

  @Mapping(target = "bootDiskSize", ignore = true)
  @Mapping(target = "cloudService", constant = "GCE")
  LeonardoGceWithPdConfig toLeonardoGceWithPdConfig(GceWithPdConfig gceWithPdConfig);

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  // these 2 values are set by listDisksAfterMapper()
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "gceRuntime", ignore = true)
  Disk toApiListDisksResponse(ListPersistentDiskResponse disk);

  @AfterMapping
  default void listDisksAfterMapper(
      @MappingTarget Disk disk, ListPersistentDiskResponse listDisksResponse) {
    LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(listDisksResponse.getLabels())
        .ifPresentOrElse(disk::setAppType, () -> disk.gceRuntime(true));
  }

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "persistentDiskId", source = "id")
  // these 2 values are set by taskQueueListDisksAfterMapper()
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "gceRuntime", ignore = true)
  TaskQueueDisk toTaskQueueDisk(ListPersistentDiskResponse disk);

  @AfterMapping
  default void taskQueueListDisksAfterMapper(
      @MappingTarget TaskQueueDisk disk, ListPersistentDiskResponse listDisksResponse) {
    LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(listDisksResponse.getLabels())
        .ifPresentOrElse(disk::setAppType, () -> disk.gceRuntime(true));
  }

  @Mapping(target = "patchInProgress", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "googleProject", ignore = true)
  LeonardoListRuntimeResponse toListRuntimeResponse(LeonardoGetRuntimeResponse runtime);

  @Nullable
  @Named("legacy_cloudContextToGoogleProject")
  default String toGoogleProject(@Nullable LeonardoCloudContext lcc) {
    return Optional.ofNullable(lcc)
        // we don't support LeonardoCloudProvider.AZURE so don't attempt to map it
        .filter(c -> c.getCloudProvider() == LeonardoCloudProvider.GCP)
        .map(LeonardoCloudContext::getCloudResource)
        .orElse(null);
  }

  @Nullable
  @Named("cloudContextToGoogleProject")
  default String toGoogleProject(@Nullable CloudContext lcc) {
    return Optional.ofNullable(lcc)
        // we don't support LeonardoCloudProvider.AZURE so don't attempt to map it
        .filter(c -> c.getCloudProvider() == CloudProvider.GCP)
        .map(CloudContext::getCloudResource)
        .orElse(null);
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  AdminRuntimeFields toAdminRuntimeFields(LeonardoGetRuntimeResponse leonardoGetRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  AdminRuntimeFields toAdminRuntimeFields(LeonardoListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "legacy_cloudContextToGoogleProject")
  @Mapping(
      target = "configurationType",
      source = "labels",
      qualifiedByName = "mapRuntimeConfigurationLabels")
  // these 3 set by getRuntimeAfterMapper()
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "legacy_cloudContextToGoogleProject")
  @Mapping(
      target = "configurationType",
      source = "labels",
      qualifiedByName = "mapRuntimeConfigurationLabels")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "errors", ignore = true)
  // these 3 set by listRuntimeAfterMapper()
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntimeWithoutDisk(LeonardoListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(LeonardoClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    mapRuntimeConfig(
        runtime,
        leonardoGetRuntimeResponse.getRuntimeConfig(),
        leonardoGetRuntimeResponse.getDiskConfig());
  }

  @AfterMapping
  default void listRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoListRuntimeResponse leonardoListRuntimeResponse) {
    mapRuntimeConfig(
        runtime,
        leonardoListRuntimeResponse.getRuntimeConfig(),
        // listRuntime does not actually have a diskConfig field.  This is OK because we only
        // call this from a context where we're not expecting one.
        null);
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "appType", source = "app", qualifiedByName = "mapAppType")
  @Mapping(target = "autopauseThreshold", ignore = true)
  UserAppEnvironment toApiApp(ListAppResponse app);

  KubernetesRuntimeConfig toKubernetesRuntimeConfig(
      org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
          leonardoKubernetesRuntimeConfig);

  @Mapping(target = "additionalProperties", ignore = true)
  org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
      toLeonardoKubernetesRuntimeConfig(KubernetesRuntimeConfig kubernetesRuntimeConfig);

  // SAS and RStudio apps are implemented as ALLOWED Helm Charts
  @ValueMapping(source = "RSTUDIO", target = "ALLOWED")
  @ValueMapping(source = "SAS", target = "ALLOWED")
  org.broadinstitute.dsde.workbench.client.leonardo.model.AppType toLeonardoAppType(
      AppType appType);

  // Cromwell is not an ALLOWED Helm Chart in Leonardo
  @ValueMapping(source = "CROMWELL", target = MappingConstants.NULL)
  AllowedChartName toLeonardoAllowedChartName(AppType appType);

  @ValueMapping(source = "BALANCED", target = MappingConstants.NULL)
  DiskType toDiskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType diskType);

  @ValueMapping(source = "BALANCED", target = MappingConstants.NULL)
  TQSafeDiskType toTaskQueueDiskType(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType diskType);

  @Named("mapAppType")
  default AppType mapAppType(ListAppResponse app) {
    final Map<String, String> appLabels = LeonardoLabelHelper.toLabelMap(app.getLabels());
    if (appLabels == null || appLabels.isEmpty()) {
      throw new ServerErrorException(
          String.format(
              "App %s in Google Project %s has no labels",
              app.getAppName(), toGoogleProject(app.getCloudContext())));
    }
    return LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(appLabels)
        .orElseThrow(
            () ->
                new ServerErrorException(
                    String.format(
                        "Missing app type labels for app %s in Google Project %s with labels %s",
                        app.getAppName(), toGoogleProject(app.getCloudContext()), appLabels)));
  }

  @Named("mapRuntimeConfigurationLabels")
  default RuntimeConfigurationType mapRuntimeConfigurationLabels(Object runtimeLabelsObj) {
    final Map<String, String> runtimeLabels = LeonardoLabelHelper.toLabelMap(runtimeLabelsObj);
    if (runtimeLabels == null
        || runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG) == null) {
      // If there's no label, fall back onto the old behavior where every Runtime was created with a
      // default Dataproc config
      return RuntimeConfigurationType.HAILGENOMICANALYSIS;
    } else {
      return RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
          .inverse()
          .get(runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG));
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

  @ValueMapping(source = MappingConstants.NULL, target = "UNKNOWN")
  RuntimeStatus toApiRuntimeStatus(LeonardoRuntimeStatus leonardoRuntimeStatus);

  @ValueMapping(source = MappingConstants.NULL, target = "UNKNOWN")
  DiskStatus toApiDiskStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus leonardoDiskStatus);

  @ValueMapping(source = MappingConstants.NULL, target = "UNKNOWN")
  TQSafeDiskStatus toTaskQueueDiskStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus leonardoDiskStatus);

  @Nullable
  default String getJupyterImage(@Nullable List<LeonardoRuntimeImage> images) {
    return Optional.ofNullable(images)
        .flatMap(
            i -> i.stream().filter(image -> "Jupyter".equals(image.getImageType())).findFirst())
        .map(LeonardoRuntimeImage::getImageUrl)
        .orElse(null);
  }
}
