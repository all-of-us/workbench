package org.pmiops.workbench.utils.mappers;

import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceList;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.UserAppEnvironment;

@Mapper(config = MapStructConfig.class)
public interface WsmMapper {

  default String map(OffsetDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
    return dateTime.format(formatter);
  }

  @Mapping(target = "createdDate", source = "metadata.createdDate")
  @Mapping(target = "dateAccessed", source = "metadata.lastUpdatedDate")
  @Mapping(target = "creator", source = "metadata.createdBy")
  @Mapping(target = "appName", source = "metadata.name")
  @Mapping(target = "googleProject", ignore = true)
  @Mapping(target = "autopauseThreshold", ignore = true)
  @Mapping(target = "appType", ignore = true)
  @Mapping(target = "status", source = "metadata.state")
  @Mapping(target = "kubernetesRuntimeConfig", ignore = true)
  @Mapping(target = "proxyUrls", ignore = true)
  @Mapping(target = "diskName", ignore = true)
  @Mapping(target = "errors", ignore = true)
  @Mapping(target = "labels", ignore = true)
  UserAppEnvironment toApiApp(ResourceDescription resourceDescription);

  default UserAppEnvironment toApiApps(ResourceList resourceList) {
    UserAppEnvironment userAppEnvironment = toApiApp(resourceList.getResources().get(0));
    userAppEnvironment.appType(AppType.SAGEMAKER);
    return userAppEnvironment;
  }
}
