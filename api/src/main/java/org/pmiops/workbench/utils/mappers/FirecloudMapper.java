package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;

@Mapper(componentModel = "spring")
public interface FirecloudMapper {

  default WorkspaceAccessLevel fcAccessLevelToApiAccessLevel(FirecloudWorkspaceAccessEntry acl) {
    return WorkspaceAccessLevel.fromValue(acl.getAccessLevel());
  }

  default WorkspaceAccessLevel fcToApiWorkspaceAccessLevel(String accessLevel) {
    if (WorkspaceAuthService.PROJECT_OWNER_ACCESS_LEVEL.equals(accessLevel)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(accessLevel);
    }
  }
}
