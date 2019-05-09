import 'rxjs/Rx';

import {Injectable} from '@angular/core';


import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceStorageService {
  // Cache of loaded workspace data. Key is of the form "ns/id".
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
        return {
          ...resp.workspace,
          accessLevel: resp.accessLevel
        };
      })
      .catch((e) => {
        // Purge the cache on error to allow for retries.
        this.cache.delete(key);
        if (e.status === 404) {
          e.title = `Workspace ${key} not found`;
        }
        throw e;
      });
    this.cache.set(key, workspace);
    return workspace;
  }

  async getWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    if (!this.cache.has(key)) {
      await this.reloadWorkspace(wsNs, wsId);
    }
    return Promise.resolve(this.cache.get(key));
  }
}
