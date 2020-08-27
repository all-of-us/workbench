package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.leonardo.model.GetRuntimeResponse;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {
  @Mapping(target = "internalId", source = "id")
  @Mapping(target = "jupyterUserScriptUri", ignore = true)
  org.pmiops.workbench.leonardo.model.ListClusterResponse toListClusterResponse(
      org.pmiops.workbench.leonardo.model.Cluster cluster);

  @Mapping(target = "patchInProgress", ignore = true)
  org.pmiops.workbench.leonardo.model.ListRuntimeResponse toListRuntimeResponse(
      GetRuntimeResponse runtime);

  @Mapping(target = "clusterName", source = "runtimeName")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  ListClusterResponse toApiListClusterResponse(
      org.pmiops.workbench.leonardo.model.ListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  ListRuntimeResponse toApiListRuntimeResponse(
      org.pmiops.workbench.leonardo.model.ListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "clusterName", source = "runtimeName")
  @Mapping(target = "clusterNamespace", source = "googleProject")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  Cluster toApiCluster(GetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  Runtime toApiRuntime(GetRuntimeResponse runtime);

  RuntimeLocalizeRequest clusterToRuntimeLocalizeRequest(ClusterLocalizeRequest req);

  @Mapping(target = "clusterLocalDirectory", source = "runtimeLocalDirectory")
  ClusterLocalizeResponse runtimeToClusterLocalizeResponse(RuntimeLocalizeResponse resp);

  default ClusterStatus toApiClusterStatus(
      org.pmiops.workbench.leonardo.model.RuntimeStatus leonardoRuntimeStatus) {
    if (leonardoRuntimeStatus == null) {
      return ClusterStatus.UNKNOWN;
    }
    return ClusterStatus.fromValue(leonardoRuntimeStatus.toString());
  }

  default RuntimeStatus toApiRuntimeStatus(
      org.pmiops.workbench.leonardo.model.RuntimeStatus leonardoRuntimeStatus) {
    if (leonardoRuntimeStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(leonardoRuntimeStatus.toString());
  }
}
