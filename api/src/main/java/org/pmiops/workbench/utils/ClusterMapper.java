package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.leonardo.model.Cluster;
import org.pmiops.workbench.leonardo.model.ListClusterResponse;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface ClusterMapper {
  @Mapping(source = "id", target = "internalId")
  @Mapping(target = "jupyterUserScriptUri", ignore = true)
  ListClusterResponse toListClusterResponse(Cluster cluster);
}
