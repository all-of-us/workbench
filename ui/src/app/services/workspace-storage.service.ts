import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';


import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

export const BLANK_WORKSPACE: WorkspaceData = {
  accessLevel: WorkspaceAccessLevel.NOACCESS,
  name: '',
  researchPurpose: {
    diseaseFocusedResearch: false,
    methodsDevelopment: false,
    controlSet: false,
    aggregateAnalysis: false,
    ancestry: false,
    commercialPurpose: false,
    population: false,
    reviewRequested: false,
  }
};

@Injectable()
export class WorkspaceStorageService {
  private activeCall = false;
  private activeWorkspace = new BehaviorSubject<WorkspaceData>(BLANK_WORKSPACE);
  public activeWorkspace$ = this.activeWorkspace.asObservable();
  wsNs: string;
  wsId: string;

  constructor(private workspacesService: WorkspacesService) {}

  reload(wsNs: string, wsId: string): Observable<boolean> {
    const finished = new ReplaySubject<boolean>(1);
    const finished$ = finished.asObservable();
    if (!this.activeCall) {
      this.activeCall = true;
      this.workspacesService.getWorkspace(wsNs, wsId).subscribe((workspaceResponse) => {
        const workspaceData: WorkspaceData = {
          ...workspaceResponse.workspace,
          accessLevel: workspaceResponse.accessLevel
        };
        this.activeWorkspace.next(workspaceData);
        this.wsNs = wsNs;
        this.wsId = wsId;
        this.activeCall = false;
        finished.next(true);
      });
    } else {
      finished.next(true);
    }
    return finished$;
  }

  reloadIfNew(wsNs: string, wsId: string): Observable<boolean> {
    const finished = new ReplaySubject<boolean>(1);
    const finished$ = finished.asObservable();
    if (this.wsNs !== wsNs || this.wsId !== wsId) {
      this.reload(wsNs, wsId).subscribe(() => {
        finished.next(true);
      });
    } else {
      this.activeWorkspace.next(this.activeWorkspace.value);
      finished.next(false);
    }
    return finished$;
  }
}
