
import {WorkspaceComponent} from 'app/views/workspace/component';
import {Cohort, CohortListResponse, Workspace} from 'generated';

import {Observable} from 'rxjs/Observable';
import {Observer} from 'rxjs/Observer';




export class CohortsServiceStub {
  constructor() {
    const stubWorkspace: Workspace = {
      name: WorkspaceComponent.DEFAULT_WORKSPACE_NAME,
      cohorts: [],
      id: WorkspaceComponent.DEFAULT_WORKSPACE_ID,
      namespace: WorkspaceComponent.DEFAULT_WORKSPACE_NS
    };

    const exampleCohort: Cohort = {id: '',
      name: '', description: '', criteria: '', type: ''};
    exampleCohort.id = '1';
    exampleCohort.name = 'sample name';
    exampleCohort.description = 'sample description';

    stubWorkspace.cohorts = [exampleCohort];
    this.workspaces = [stubWorkspace];
  }
  public workspaces: Workspace[];

  private getMatchingWorkspaceOrSendError(
      wsNamespace: string,
      wsId: string,
      observer: Observer<{}>): Workspace {
    const workspaceFound = this.workspaces.find(function(workspace: Workspace) {
      if (workspace.namespace === wsNamespace && workspace.id === wsId) {
        return true;
      } else {
        return false;
      }
    });
    if (workspaceFound === undefined) {
      observer.error(`Error Searching. No workspace ${wsNamespace}, ${wsId} found `
                    + 'in cohort service stub.');
    }
    return workspaceFound;
  }

  public getCohort(
      wsNamespace: string,
      wsId: string,
      cId: string): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
          }));
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }

  public updateCohort(
      wsNamespace: string,
      wsId: string,
      cId: string,
      newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          const index = workspaceMatch.cohorts.findIndex(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
            return false;
          });
          if (index !== -1) {
            workspaceMatch.cohorts[index] = newCohort;
            observer.complete();
          } else {
            observer.error(new Error(`Error updating. No cohort with id: ${cId} `
                                    + `exists in workspace ${wsNamespace}, ${wsId} `
                                    + `in cohort service stub`));
          }
        }
      }, 0);
    });
    return observable;
  }

  public createCohort(
      wsNamespace: string,
      wsId: string,
      newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === newCohort.id) {
              observer.error(new Error(`Error creating. Cohort with `
                                      + `id: ${cohort.id} already exists.`));
              return true;
            }
          }));
          workspaceMatch.cohorts.push(newCohort);
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }

  public getCohortsInWorkspace(
      wsNamespace: string,
      wsId: string): Observable<CohortListResponse> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next({items: workspaceMatch.cohorts});
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }
}
