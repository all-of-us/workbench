package org.pmiops.workbench.workspaces;

import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.utils.mappers.MapStructConfig;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;

@Mapper(config = MapStructConfig.class)
public interface WorkspaceOperationMapper {
  // use toModelWithWorkspace() if you need to populate the workspace
  @Mapping(target = "workspace", ignore = true)
  WorkspaceOperation toModelWithoutWorkspace(DbWorkspaceOperation source);

  default WorkspaceOperation toModelWithWorkspace(
      DbWorkspaceOperation source,
      WorkspaceDao workspaceDao,
      FireCloudService fireCloudService,
      WorkspaceMapper workspaceMapper) {

    WorkspaceOperation modelOperation = toModelWithoutWorkspace(source);
    // set the operation's workspace, if possible
    Optional.ofNullable(source.getWorkspaceId())
        .flatMap(
            workspaceId ->
                getWorkspaceMaybe(workspaceId, workspaceDao, fireCloudService, workspaceMapper))
        .ifPresent(modelOperation::workspace);

    return modelOperation;
  }

  /**
   * We can return a Workspace if:
   *
   * <ul>
   *   <li>The Workspace ID is in the DB
   *   <li>The Workspace is in Terra/FireCloud
   * </ul>
   *
   * TODO: move this to WorkspaceMapper?
   *
   * @return Workspace or EMPTY
   */
  default Optional<Workspace> getWorkspaceMaybe(
      long workspaceId,
      WorkspaceDao workspaceDao,
      FireCloudService fireCloudService,
      WorkspaceMapper workspaceMapper) {
    return workspaceDao
        .findActiveByWorkspaceId(workspaceId)
        .flatMap(
            dbWorkspace ->
                Optional.ofNullable(
                        fireCloudService.getWorkspace(
                            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName()))
                    .map(
                        fcWorkspaceResponse ->
                            workspaceMapper.toApiWorkspace(
                                dbWorkspace, fcWorkspaceResponse.getWorkspace())));
  }
}
