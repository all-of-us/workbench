import 'rxjs/Rx';

import {Injectable} from '@angular/core';

import {currentWorkspaceStore} from 'app/utils/navigation';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceStorageService {
  // Cache of loading or completed Promises for workspace data. Key is of the
  // form "ns/id".
  private cache = new Map<string, WorkspaceData>();

  constructor(private workspacesService: WorkspacesService) {}

  private wsKey(wsNs, wsId: string): string {
    return `${wsNs}/${wsId}`;
  }

  async reloadWorkspace(wsNs: string, wsId: string): Promise<void> {
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
    currentWorkspaceStore.next(workspace);
    this.cache.set(key, workspace);
    return;
  }

  async getWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    if (!this.cache.has(key)) {
      await this.reloadWorkspace(wsNs, wsId);
    }
    currentWorkspaceStore.next(this.cache.get(key));
    return Promise.resolve(this.cache.get(key));
  }
}
