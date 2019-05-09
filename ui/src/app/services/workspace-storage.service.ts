import 'rxjs/Rx';

import {Injectable} from '@angular/core';

import {currentWorkspaceStore} from 'app/utils/navigation';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

const nextCurrentWorkspace = (workspace: WorkspaceData) => {
  currentWorkspaceStore.next(workspace);
};

@Injectable()
export class WorkspaceStorageService {
  // Cache of loading or completed Promises for workspace data. Key is of the
  // form "ns/id".
  private cache = new Map<string, WorkspaceData>();

  constructor(private workspacesService: WorkspacesService) {}

  private wsKey(wsNs, wsId: string): string {
    return `${wsNs}/${wsId}`;
  }

  async reloadWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    const workspace = await this.workspacesService.getWorkspace(wsNs, wsId)
      .toPromise()
      .then((resp) => {
        const workspaceResp = {
          ...resp.workspace,
          accessLevel: resp.accessLevel
        };
        return workspaceResp;
      })
      .catch((e) => {
        // Purge the cache on error to allow for retries.
        this.cache.delete(key);
        if (e.status === 404) {
          e.title = `Workspace ${key} not found`;
        }
        throw e;
      });
    nextCurrentWorkspace(workspace);
    this.cache.set(key, workspace);
    return workspace;
  }

  async getWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    if (!this.cache.has(key)) {
      return await this.reloadWorkspace(wsNs, wsId);
    }
    nextCurrentWorkspace(this.cache.get(key));
    return Promise.resolve(this.cache.get(key));
  }
}
