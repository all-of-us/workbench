import {Observable} from 'rxjs/Observable';

import {Cohort, CohortListResponse, Workspace} from 'generated';

import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

export let DEFAULT_COHORT_ID = '1';


class CohortStub implements Cohort {
  id?: string;

  name: string;

  criteria: string;

  type: string;

  description?: string;

  creator?: string;

  creationTime?: number;

  lastModifiedTime?: number;

  workspaceId: string;

  constructor(cohort: Cohort, wsId: string) {
    this.creationTime = cohort.creationTime;
    this.creator = cohort.creator;
    this.criteria = cohort.criteria;
    this.description = cohort.description;
    this.id = cohort.id;
    this.lastModifiedTime = cohort.lastModifiedTime;
    this.name = cohort.name;
    this.type = cohort.type;
    this.workspaceId = wsId;
  }
}


export class CohortsServiceStub {
  constructor() {
    const stubWorkspace: Workspace = {
      name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
      id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS
    };

    const exampleCohort: CohortStub = {id: '',
      name: '', description: '', criteria: '', type: '',
      workspaceId: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID};
    exampleCohort.id = DEFAULT_COHORT_ID;
    exampleCohort.name = 'sample name';
    exampleCohort.description = 'sample description';
    exampleCohort.creationTime = new Date().getTime();
    exampleCohort.lastModifiedTime = new Date().getTime();
    this.cohorts = [exampleCohort];
    this.workspaces = [stubWorkspace];
  }
  public workspaces: Workspace[];
  public cohorts: CohortStub[];
  public getCohort(
      wsNamespace: string,
      wsId: string,
      cId: string): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        observer.next(this.cohorts.find(function(cohort: CohortStub) {
          if (cohort.id === cId && cohort.workspaceId === wsId) {
            return true;
          }
        }));
        observer.complete();
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

        const index = this.cohorts.findIndex(function(cohort: CohortStub) {
          if (cohort.id === cId && cohort.workspaceId) {
            return true;
          }
          return false;
        });
        if (index !== -1) {
          this.cohorts[index] = new CohortStub(newCohort, wsId);
          observer.complete();
        } else {
          observer.error(new Error(`Error updating. No cohort with id: ${cId} `
                                  + `exists in workspace ${wsNamespace}, ${wsId} `
                                  + `in cohort service stub`));
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
        observer.next(this.cohorts.find(function(cohort: Cohort) {
          if (cohort.id === newCohort.id) {
            observer.error(new Error(`Error creating. Cohort with `
                                    + `id: ${cohort.id} already exists.`));
            return true;
          }
        }));
        this.cohorts.push(new CohortStub(newCohort, wsId));
        observer.complete();
      }, 0);
    });
    return observable;
  }

  public getCohortsInWorkspace(
      wsNamespace: string,
      wsId: string): Observable<CohortListResponse> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const cohortsInWorkspace: Cohort[] = [];
        this.cohorts.forEach(cohort => {
          if (cohort.workspaceId === wsId) {
            cohortsInWorkspace.push(cohort);
          }
        });
        if (cohortsInWorkspace.length === 0) {
          observer.error('No cohorts in workspace.');
        } else {
          observer.next({items: cohortsInWorkspace});
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }
}
