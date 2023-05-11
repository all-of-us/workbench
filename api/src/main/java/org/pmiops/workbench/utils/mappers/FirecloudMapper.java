package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;

@Mapper(componentModel = "spring", suppressTimestampInGenerated = true)
public interface FirecloudMapper {

  default WorkspaceAccessLevel fcAccessLevelToApiAccessLevel(RawlsWorkspaceAccessEntry acl) {
    return WorkspaceAccessLevel.fromValue(acl.getAccessLevel());
  }

  @Named("fcToApiWorkspaceAccessLevel")
  @ValueMapping(source = "PROJECT_OWNER", target = "OWNER")
  WorkspaceAccessLevel fcToApiWorkspaceAccessLevel(RawlsWorkspaceAccessLevel accessLevel);

  @Named("apiToFcWorkspaceAccessLevel")
  RawlsWorkspaceAccessLevel apiToFcWorkspaceAccessLevel(WorkspaceAccessLevel accessLevel);
}
