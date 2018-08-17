import {Workspace, WorkspaceAccessLevel, WorkspaceResponse} from '../../generated';

/**
 * Composite class with helper functions useful in the UI.
 */
export class WorkspacePermissions {

  static workspace(workspace: Workspace) {
    return workspace;
  }

  static accessLevel(accessLevel: WorkspaceAccessLevel) {
    return accessLevel;
  }

  static isOwner(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER;
  }

  static canWrite(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER ||
      accessLevel === WorkspaceAccessLevel.WRITER;
  }

  static canRead(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.OWNER ||
      accessLevel === WorkspaceAccessLevel.WRITER ||
      accessLevel === WorkspaceAccessLevel.READER;
  }

  static isReadOnly(accessLevel: WorkspaceAccessLevel) {
    return accessLevel === WorkspaceAccessLevel.READER;
  }

  static isPending(workspace: Workspace) {
    return workspace.researchPurpose.reviewRequested === true &&
      workspace.researchPurpose.approved === null;
  }

  static isRejected(workspace: Workspace) {
    return workspace.researchPurpose.reviewRequested === true &&
      workspace.researchPurpose.approved === false;
  }

  static create(wsResponse: WorkspaceResponse) {
    return {
      workspace: this.workspace(wsResponse.workspace),
      accessLevel: this.accessLevel(wsResponse.accessLevel),
      isOwner: this.isOwner(wsResponse.accessLevel),
      canWrite: this.canWrite(wsResponse.accessLevel),
      canRead: this.canRead(wsResponse.accessLevel),
      isReadOnly: this.isReadOnly(wsResponse.accessLevel),
      isPending: this.isPending(wsResponse.workspace),
      isRejected: this.isRejected(wsResponse.workspace)
    };
  }

}
