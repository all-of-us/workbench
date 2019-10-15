import {Workspace, WorkspaceAccessLevel, WorkspaceResponse} from 'generated/fetch';

export namespace WorkspacePermissionsUtil {
  export function canWrite(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER ||
      accessLevel === WorkspaceAccessLevel.WRITER;
  }

  export function isOwner(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER;
  }
}

export class WorkspacePermissions {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;

  constructor(workspaceResponse: WorkspaceResponse) {
    this.workspace = workspaceResponse.workspace;
    this.accessLevel = workspaceResponse.accessLevel;
  }

  get canWrite() {
    return WorkspacePermissionsUtil.canWrite(this.accessLevel);
  }

  get canRead() {
    return this.accessLevel === WorkspaceAccessLevel.OWNER ||
      this.accessLevel === WorkspaceAccessLevel.WRITER ||
      this.accessLevel === WorkspaceAccessLevel.READER;
  }

  get isReadOnly() {
    return this.accessLevel === WorkspaceAccessLevel.READER;
  }
}
