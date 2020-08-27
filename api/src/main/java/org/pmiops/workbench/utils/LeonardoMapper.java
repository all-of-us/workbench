package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.leonardo.model.GetRuntimeResponse;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {
  @Mapping(source = "id", target = "internalId")
  @Mapping(target = "jupyterUserScriptUri", ignore = true)
  org.pmiops.workbench.leonardo.model.ListClusterResponse toListClusterResponse(
      org.pmiops.workbench.leonardo.model.Cluster cluster);

  @Mapping(target = "patchInProgress", ignore = true)
  org.pmiops.workbench.leonardo.model.ListRuntimeResponse toListRuntimeResponse(
      GetRuntimeResponse runtime);

  @Mapping(source = "runtimeName", target = "clusterName")
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "dateAccessed", ignore = true)
  ListClusterResponse toApiListClusterResponse(
      org.pmiops.workbench.leonardo.model.ListRuntimeResponse leonardoListRuntimeResponse);

  ListRuntimeResponse toApiListRuntimeResponse(
      org.pmiops.workbench.leonardo.model.ListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(source = "runtimeName", target = "clusterName")
  @Mapping(source = "googleProject", target = "clusterNamespace")
  @Mapping(source = "auditInfo.createdDate", target = "createdDate")
  Cluster toApiCluster(GetRuntimeResponse runtime);

  @Mapping(source = "auditInfo.createdDate", target = "createdDate")
  Runtime toApiRuntime(GetRuntimeResponse runtime);

  default ClusterStatus toApiClusterStatus(
      org.pmiops.workbench.leonardo.model.ClusterStatus leonardoClusterStatus) {
    return ClusterStatus.fromValue(leonardoClusterStatus.toString());
  }

  default RuntimeStatus toApiRuntimeStatus(
      org.pmiops.workbench.leonardo.model.RuntimeStatus leonardoRuntimeStatus) {
    return RuntimeStatus.fromValue(leonardoRuntimeStatus.toString());
  }
}
