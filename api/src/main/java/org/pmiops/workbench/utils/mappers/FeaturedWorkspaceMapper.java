package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.PublishWorkspaceRequest;

@Mapper(config = MapStructConfig.class)
public interface FeaturedWorkspaceMapper {

  @Mapping(target = "workspace", source = "dbWorkspace")
  @Mapping(target = "id", ignore = true)
  DbFeaturedWorkspace toDbFeaturedWorkspace(
      PublishWorkspaceRequest featuredWorkspace, DbWorkspace dbWorkspace);

  @Mapping(target = "description", source = "featuredWorkspace.description")
  @Mapping(target = "category", source = "featuredWorkspace.category")
  DbFeaturedWorkspace toDbFeaturedWorkspace(
      DbFeaturedWorkspace dbFeaturedWorkspace, PublishWorkspaceRequest featuredWorkspace);

  FeaturedWorkspaceCategory toFeaturedWorkspaceCategory(DbFeaturedCategory dbFeaturedCategory);

  DbFeaturedCategory toDbFeaturedCategory(FeaturedWorkspaceCategory dbFeaturedCategory);
}
