package org.pmiops.workbench.utils.mappers;

import com.google.gson.Gson;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AllowedChartName;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoClusterError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoMachineConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeImage;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
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
import org.pmiops.workbench.model.RuntimeError;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {
  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

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

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  @Mapping(
      target = "googleProject",
      source = "cloudContext",
      qualifiedByName = "cloudContextToGoogleProject")
  @Mapping(target = "persistentDiskId", source = "id")
  // these 2 values are set by listDisksAfterMapper()
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "gceRuntime", ignore = true)
  Disk toApiDisk(ListPersistentDiskResponse disk);

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

  @Nullable
  default String getJupyterImage(@Nullable List<LeonardoRuntimeImage> images) {
    return Optional.ofNullable(images)
        .flatMap(
            i -> i.stream().filter(image -> "Jupyter".equals(image.getImageType())).findFirst())
        .map(LeonardoRuntimeImage::getImageUrl)
        .orElse(null);
  }
}
