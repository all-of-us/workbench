package org.pmiops.workbench.workspaces;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface WorkspaceResourceMapper {
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohort", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "cohortReview", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "conceptSet", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "dataSet", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "notebook", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "modifiedTime", ignore = true) // This should be set when the resource is set
  WorkspaceResource workspaceResourceFromDbWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);
}
