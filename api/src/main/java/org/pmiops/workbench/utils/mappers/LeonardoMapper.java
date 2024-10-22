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
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfigInResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoOneOfRuntimeConfigInResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeImage;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig;
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

  DataprocConfig toDataprocConfig(LeonardoDataprocConfig leonardoDataprocConfig);

  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "workerPrivateAccess", ignore = true)
  @Mapping(target = "region", ignore = true)
  @Mapping(target = "cloudService", constant = "DATAPROC")
  @Mapping(target = "componentGatewayEnabled", constant = "true")
  LeonardoDataprocConfig toLeonardoDataprocConfig(DataprocConfig dataprocConfig);

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

  GceWithPdConfig toGceWithPdConfig(
      LeonardoGceWithPdConfigInResponse leonardoConfig, PersistentDiskRequest persistentDisk);

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
    setDiskEnvironmentType(disk, listDisksResponse.getLabels());
  }

  default void setDiskEnvironmentType(Disk disk, @Nullable Object diskLabels) {
    LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(diskLabels)
        .ifPresentOrElse(disk::setAppType, () -> disk.gceRuntime(true));
  }

  @Mapping(target = "workspaceId", ignore = true)
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
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "legacy_cloudContextToGoogleProject")
  ListRuntimeResponse toApiListRuntimeResponse(
      LeonardoListRuntimeResponse leonardoListRuntimeResponse);

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
  Runtime toApiRuntime(LeonardoListRuntimeResponse runtime);

  RuntimeError toApiRuntimeError(LeonardoClusterError err);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    mapRuntimeConfig(runtime, leonardoGetRuntimeResponse.getRuntimeConfig());
  }

  @AfterMapping
  default void listRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoListRuntimeResponse leonardoListRuntimeResponse) {
    mapRuntimeConfig(runtime, leonardoListRuntimeResponse.getRuntimeConfig());
  }

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "appType", source = "labels", qualifiedByName = "mapAppType")
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

  @Named("mapAppType")
  default AppType mapAppType(@Nullable Object appLabels) {
    return LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(appLabels)
        .orElseThrow(
            () ->
                new ServerErrorException(
                    String.format("Missing app type labels for app with labels %s", appLabels)));
  }

  @Named("mapRuntimeConfigurationLabels")
  default RuntimeConfigurationType mapRuntimeConfigurationLabels(Object runtimeLabelsObj) {
    @SuppressWarnings("unchecked")
    final Map<String, String> runtimeLabels = (Map<String, String>) runtimeLabelsObj;
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

  @Nullable
  default CloudServiceEnum getCloudService(LeonardoOneOfRuntimeConfigInResponse runtimeConfigObj) {
    if (runtimeConfigObj == null) {
      return null;
    }

    Gson gson = new Gson();
    LeonardoRuntimeConfig runtimeConfig =
        gson.fromJson(gson.toJson(runtimeConfigObj), LeonardoRuntimeConfig.class);

    return runtimeConfig.getCloudService();
  }

  default void mapRuntimeConfig(
      Runtime runtime, LeonardoOneOfRuntimeConfigInResponse runtimeConfigObj) {
    if (runtimeConfigObj == null) {
      return;
    }

    CloudServiceEnum cloudService = getCloudService(runtimeConfigObj);
    if (cloudService == null) {
      return;
    }

    switch (cloudService) {
      case DATAPROC:
        {
          runtime.dataprocConfig(
              toDataprocConfig(
                  new Gson()
                      .fromJson(
                          new Gson().toJson(runtimeConfigObj), LeonardoDataprocConfig.class)));
          break;
        }
      case GCE:
        LeonardoGceWithPdConfigInResponse leonardoConfig =
            new Gson()
                .fromJson(
                    new Gson().toJson(runtimeConfigObj), LeonardoGceWithPdConfigInResponse.class);

        leonardoConfig.getPersistentDiskId();
        PersistentDiskRequest disk = null;

        runtime.gceWithPdConfig(toGceWithPdConfig(leonardoConfig, disk));
        break;
      default:
        throw new IllegalArgumentException("Invalid RuntimeConfig.cloudService : " + cloudService);
    }
  }

  default RuntimeStatus toApiRuntimeStatus(LeonardoRuntimeStatus clusterStatus) {
    if (clusterStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(clusterStatus.toString());
  }

  default DiskStatus toApiDiskStatus(
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus leonardoDiskStatus) {
    if (leonardoDiskStatus == null) {
      return DiskStatus.UNKNOWN;
    }
    return DiskStatus.fromValue(leonardoDiskStatus.toString());
  }

  @Nullable
  default String getJupyterImage(@Nullable List<LeonardoRuntimeImage> images) {
    return Optional.ofNullable(images)
        .flatMap(
            i -> i.stream().filter(image -> "Jupyter".equals(image.getImageType())).findFirst())
        .map(LeonardoRuntimeImage::getImageUrl)
        .orElse(null);
  }
}
