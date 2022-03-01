package org.pmiops.workbench.workspaces;

import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.utils.mappers.MapStructConfig;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;

@Mapper(config = MapStructConfig.class)
public interface WorkspaceOperationMapper {
  // use toModelWithWorkspace() if you need to populate the workspace
  @Mapping(target = "workspace", ignore = true)
  WorkspaceOperation toModelWithoutWorkspace(DbWorkspaceOperation source);

  default WorkspaceOperation toModelWithWorkspace(
      DbWorkspaceOperation dbOperation,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    WorkspaceOperation apiOperation = toModelWithoutWorkspace(dbOperation);

    // populate a Workspace API Entity from the DbWorkspace, if it exists
    Optional.ofNullable(dbOperation.getWorkspaceId())
        .flatMap(workspaceDao::findActiveByWorkspaceId)
        .map(workspaceMapper::toApiWorkspace)
        .ifPresent(apiOperation::setWorkspace);

    return apiOperation;
  }
}
