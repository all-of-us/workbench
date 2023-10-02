package org.pmiops.workbench.workspaces;

import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface WorkspaceOperationMapper {
  // use toModelWithWorkspace() if you need to populate the workspace
  @Mapping(target = "workspace", ignore = true)
  WorkspaceOperation toModelWithoutWorkspace(DbWorkspaceOperation source);

  default WorkspaceOperation toModelWithWorkspace(
      WorkspaceOperation modelOperation, Workspace workspace) {

    // set the operation's workspace, if possible
    Optional.ofNullable(workspace).ifPresent(modelOperation::workspace);

    return modelOperation;
  }
}
