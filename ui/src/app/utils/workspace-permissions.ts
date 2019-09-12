import {Workspace, WorkspaceAccessLevel, WorkspaceResponse} from 'generated/fetch';

export namespace WorkspacePermissionsUtil {
  export function canWrite(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER ||
      accessLevel === WorkspaceAccessLevel.WRITER;
  }
}

export class WorkspacePermissions {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;

  constructor(workspaceResponse: WorkspaceResponse) {
    this.workspace = workspaceResponse.workspace;
    this.accessLevel = workspaceResponse.accessLevel;
  }

  get isOwner() {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
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

  get isPending() {
    return this.workspace.researchPurpose.reviewRequested === true &&
      this.workspace.researchPurpose.approved === null;
  }

  get isRejected() {
    return this.workspace.researchPurpose.reviewRequested === true &&
      this.workspace.researchPurpose.approved === false;
  }
}
