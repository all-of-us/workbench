import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';


import {Workspace} from 'generated';
import {WorkspaceResponse} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceAccessLevel} from 'generated';

export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceStorageService {
  private activeCall = false;
  private activeCallObservable: Observable<WorkspaceResponse>;
  private loadingWorkspace = new ReplaySubject<WorkspaceData>(1);
  private cachedWorkspace: WorkspaceData = {
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

  public loadingWorkspace$ = this.loadingWorkspace.asObservable();

  constructor(private workspacesService: WorkspacesService) {}

  reloadWorkspace(wsNs: string, wsId: string): Observable<WorkspaceData> {
    if (!this.activeCall) {
      this.activeCallObservable = this.workspacesService.getWorkspace(wsNs, wsId);
      this.activeCall = true;
    }
    this.activeCallObservable.subscribe(
      (workspaceResponse) => {
        const workspaceData: WorkspaceData = {
          ...workspaceResponse.workspace,
          accessLevel: workspaceResponse.accessLevel
        };
        console.log(this);
        this.loadingWorkspace.next(workspaceData);
        this.cachedWorkspace = workspaceData;
        this.activeCall = false;
      },
      (e) => {
        if (e.status === 404) {
          e.title = 'Workspace ' + wsNs + '/' + wsId + ' not found';
        }
        this.activeCall = false;
        throw e;
      }
    );
    return this.loadingWorkspace$.first();
  }

  getWorkspace(wsNs: string, wsId: string): Observable<WorkspaceData> {
    if (!this.activeCall) {
      if (this.cachedWorkspace.namespace !== wsNs || this.cachedWorkspace.id !== wsId) {
        this.reloadWorkspace(wsNs, wsId);
      } else {
        this.loadingWorkspace.next(this.cachedWorkspace);
      }
    } else {
      this.activeCallObservable.subscribe(
        (workspaceResponse) => {
          const workspaceData: WorkspaceData = {
            ...workspaceResponse.workspace,
            accessLevel: workspaceResponse.accessLevel
          };
          console.log(this);
          this.loadingWorkspace.next(workspaceData);
          this.cachedWorkspace = workspaceData;
          this.activeCall = false;
        },
        (e) => {
          if (e.status === 404) {
            e.title = 'Workspace ' + wsNs + '/' + wsId + ' not found';
          }
          this.activeCall = false;
          throw e;
        }
      );
    }
    return this.loadingWorkspace$.first();
  }
}
