package org.pmiops.workbench.utils.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.PublishWorkspaceRequest;

@Mapper(config = MapStructConfig.class)
public interface FeaturedWorkspaceMapper {

  @Mapping(target = "workspace", ignore = true)
  @Mapping(target = "id", ignore = true)
  DbFeaturedWorkspace toDbFeaturedWorkspace(
      PublishWorkspaceRequest featuredWorkspace, DbWorkspace dbWorkspace);

  @AfterMapping
  default void setWorkspace(DbWorkspace dbWorkspace, @MappingTarget DbFeaturedWorkspace target) {
    target.setWorkspace(dbWorkspace);
  }
}
