package org.pmiops.workbench.utils.mappers;

import java.util.List;
import com.google.gson.Gson;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {

  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

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
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  @AfterMapping
  default void mapRuntimeConfig(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    Gson gson = new Gson();
    LeonardoRuntimeConfig runtimeConfig =
        gson.fromJson(
            gson.toJson(leonardoGetRuntimeResponse.getRuntimeConfig()),
            LeonardoRuntimeConfig.class);

    if (CloudServiceEnum.DATAPROC.equals(runtimeConfig.getCloudService())) {
      runtime.dataprocConfig(
          toDataprocConfig(
              gson.fromJson(
                  gson.toJson(leonardoGetRuntimeResponse.getRuntimeConfig()),
                  LeonardoMachineConfig.class)));
    } else if (CloudServiceEnum.GCE.equals(runtimeConfig.getCloudService())) {
      runtime.gceConfig(
          toGceConfig(
              gson.fromJson(
                  gson.toJson(leonardoGetRuntimeResponse.getRuntimeConfig()),
                  LeonardoGceConfig.class)));
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
