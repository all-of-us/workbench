package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.leonardo.model.LeonardoDiskConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskConfig;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {

  String RUNTIME_LABEL_AOU = "all-of-us";
  String RUNTIME_LABEL_AOU_CONFIG = "all-of-us-config";
  String RUNTIME_LABEL_CREATED_BY = "created-by";
  BiMap<RuntimeConfigurationType, String> RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP =
      ImmutableBiMap.of(
          RuntimeConfigurationType.USEROVERRIDE, "user-override",
          RuntimeConfigurationType.GENERALANALYSIS, "preset-general-analysis",
          RuntimeConfigurationType.HAILGENOMICANALYSIS, "preset-hail-genomic-analysis");

  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "properties", ignore = true)
  LeonardoMachineConfig toLeonardoMachineConfig(DataprocConfig dataprocConfig);

  @AfterMapping
  default void addCloudServiceEnum(@MappingTarget LeonardoMachineConfig leonardoMachineConfig) {
    leonardoMachineConfig.setCloudService(LeonardoMachineConfig.CloudServiceEnum.DATAPROC);
  }

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "bootDiskSize", ignore = true)
  LeonardoGceConfig toLeonardoGceConfig(GceConfig gceConfig);

  @AfterMapping
  default void addCloudServiceEnum(@MappingTarget LeonardoGceConfig leonardoGceConfig) {
    leonardoGceConfig.setCloudService(LeonardoGceConfig.CloudServiceEnum.GCE);
  }

  DiskConfig toDiskConfig(LeonardoDiskConfig leonardoDiskConfig);

  Disk toApiDisk(LeonardoGetPersistentDiskResponse disk);

  @Mapping(target = "cloudService", ignore = true)
  LeonardoGceWithPdConfig toLeonardoGceWithPdConfig(GceWithPdConfig gceWithPdConfig);

  @AfterMapping
  default void addPdCloudServiceEnum(
      @MappingTarget LeonardoGceWithPdConfig leonardoGceWithPdConfig) {
    leonardoGceWithPdConfig.setCloudService(LeonardoGceWithPdConfig.CloudServiceEnum.GCE);
  }

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
  @Mapping(target = "diskConfig", ignore = true)
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "toolDockerImage", ignore = true)
  @Mapping(target = "configurationType", ignore = true)
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "gceWithPdConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  @Mapping(target = "diskConfig", ignore = true)
  Runtime toApiRuntime(LeonardoListRuntimeResponse runtime);

  @AfterMapping
  default void getRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    mapLabels(runtime, leonardoGetRuntimeResponse.getLabels());
    mapRuntimeConfig(runtime, leonardoGetRuntimeResponse.getRuntimeConfig());
    mapDiskConfig(runtime, leonardoGetRuntimeResponse);
  }

  @AfterMapping
  default void listRuntimeAfterMapper(
      @MappingTarget Runtime runtime, LeonardoListRuntimeResponse leonardoListRuntimeResponse) {
    mapLabels(runtime, leonardoListRuntimeResponse.getLabels());
    mapRuntimeConfig(runtime, leonardoListRuntimeResponse.getRuntimeConfig());
  }

  default void mapLabels(Runtime runtime, Object runtimeLabelsObj) {
    @SuppressWarnings("unchecked")
    final Map<String, String> runtimeLabels = (Map<String, String>) runtimeLabelsObj;
    if (runtimeLabels == null || runtimeLabels.get(RUNTIME_LABEL_AOU_CONFIG) == null) {
      // If there's no label, fall back onto the old behavior where every Runtime was created with a
      // default Dataproc config
      runtime.setConfigurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS);
    } else {
      runtime.setConfigurationType(
          RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
              .inverse()
              .get(runtimeLabels.get(RUNTIME_LABEL_AOU_CONFIG)));
    }
  }

  default void mapDiskConfig(
      Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    Gson gson = new Gson();
    runtime.diskConfig(
        toDiskConfig(
            gson.fromJson(
                gson.toJson(leonardoGetRuntimeResponse.getDiskConfig()),
                LeonardoDiskConfig.class)));
  }

  default void mapRuntimeConfig(Runtime runtime, Object runtimeConfigObj) {
    if (runtimeConfigObj == null) {
      return;
    }

    Gson gson = new Gson();
    LeonardoRuntimeConfig runtimeConfig =
        gson.fromJson(gson.toJson(runtimeConfigObj), LeonardoRuntimeConfig.class);

    if (CloudServiceEnum.DATAPROC.equals(runtimeConfig.getCloudService())) {
      runtime.dataprocConfig(
          toDataprocConfig(
              gson.fromJson(gson.toJson(runtimeConfigObj), LeonardoMachineConfig.class)));
    } else if (CloudServiceEnum.GCE.equals(runtimeConfig.getCloudService())) {
      runtime.gceConfig(
          toGceConfig(gson.fromJson(gson.toJson(runtimeConfigObj), LeonardoGceConfig.class)));
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

  default String getJupyterImage(List<LeonardoRuntimeImage> images) {
    return images.stream()
        .filter(image -> "Jupyter".equals(image.getImageType()))
        .findFirst()
        .get()
        .getImageUrl();
  }
}
