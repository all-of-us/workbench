package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface ClusterMapper {
  @Mapping(source = "id", target = "internalId")
  ListClusterResponse toListClusterResponse(Cluster cluster);
}
