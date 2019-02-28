import {
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse
} from 'generated';

export class WorkspacePermissions {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;

  constructor(workspaceResponse: WorkspaceResponse) {
    this.workspace = workspaceResponse.workspace;
    this.accessLevel = workspaceResponse.accessLevel;
  }

  get isOwner(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  get canWrite(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER ||
      this.accessLevel === WorkspaceAccessLevel.WRITER;
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
