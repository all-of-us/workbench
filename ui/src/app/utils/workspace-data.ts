import {Workspace, WorkspaceAccessLevel} from 'generated/fetch';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}
