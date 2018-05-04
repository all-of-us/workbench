import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {Workspace, WorkspaceAccessLevel, WorkspacesService} from 'generated';

/**
 * Flatten a layer of nesting
 */
export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceResolver implements Resolve<WorkspaceData> {
  constructor(
    private api: WorkspacesService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Promise<WorkspaceData> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;

    // console.log(`Resolving Workspace ${ns}/${wsid}:`);
    // console.dir(route);

    return this.workspaceStorageService.getWorkspace(ns, wsid);
  }
}
