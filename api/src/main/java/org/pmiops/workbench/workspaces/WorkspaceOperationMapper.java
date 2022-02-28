package org.pmiops.workbench.workspaces;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.db.model.DbWorkspaceOperation.DbWorkspaceOperationStatus;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.model.WorkspaceOperationStatus;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface WorkspaceOperationMapper {
  WorkspaceOperationStatus toModelStatus(DbWorkspaceOperationStatus source);

  // TODO
  @Mapping(target = "workspace", ignore = true)
  WorkspaceOperation toModel(DbWorkspaceOperation source);
}
