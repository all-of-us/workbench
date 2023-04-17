package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;

@Mapper(componentModel = "spring")
public interface FirecloudMapper {

  default WorkspaceAccessLevel fcAccessLevelToApiAccessLevel(RawlsWorkspaceAccessEntry acl) {
    return WorkspaceAccessLevel.fromValue(acl.getAccessLevel());
  }

  @Named("fcToApiWorkspaceAccessLevel")
  default WorkspaceAccessLevel fcToApiWorkspaceAccessLevel(String accessLevel) {
    if (WorkspaceAuthService.PROJECT_OWNER_ACCESS_LEVEL.equals(accessLevel)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(accessLevel);
    }
  }
}
