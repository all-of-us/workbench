import 'rxjs/Rx';

import {Injectable} from '@angular/core';

import {Workspace} from 'generated';
import {WorkspaceResponse} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceStorageService {
  // Cache of loading or completed Promises for workspace data. Key is of the
  // form "ns/id".
  private cache = new Map<string, Promise<WorkspaceData>>();

  constructor(private workspacesService: WorkspacesService) {}

  private wsKey(wsNs, wsId: string): string {
    return `${wsNs}/${wsId}`;
  }

  reloadWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    const load = this.workspacesService.getWorkspace(wsNs, wsId)
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
    this.cache.set(key, load);
    return load;
  }

  getWorkspace(wsNs: string, wsId: string): Promise<WorkspaceData> {
    const key = this.wsKey(wsNs, wsId);
    if (!this.cache.has(key)) {
      return this.reloadWorkspace(wsNs, wsId);
    }
    return this.cache.get(key);
  }
}
