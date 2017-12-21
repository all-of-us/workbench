import {Observable} from 'rxjs/Observable';

import {Cohort, CohortListResponse, Workspace} from 'generated';

import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

export let DEFAULT_COHORT_ID = 1;


class CohortStub implements Cohort {
  id?: number;

  name: string;

  criteria: string;

  type: string;

  description?: string;

  creator?: string;

  creationTime?: number;

  lastModifiedTime?: number;

  workspaceId: string;

  constructor(cohort: Cohort, wsid: string) {
    this.creationTime = cohort.creationTime;
    this.creator = cohort.creator;
    this.criteria = cohort.criteria;
    this.description = cohort.description;
    this.id = cohort.id;
    this.lastModifiedTime = cohort.lastModifiedTime;
    this.name = cohort.name;
    this.type = cohort.type;
    this.workspaceId = wsid;
  }
}


export class CohortsServiceStub {
  public workspaces: Workspace[];
  public cohorts: CohortStub[];

  constructor() {
    const stubWorkspace: Workspace = {
      name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
      id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS
    };

    const exampleCohort: CohortStub = {
      id: DEFAULT_COHORT_ID,
      name: 'sample name',
      description: 'sample description',
      criteria: '',
      type: '',
      workspaceId: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      creationTime: new Date().getTime(),
      lastModifiedTime: new Date().getTime(),
    };
    this.cohorts = [exampleCohort];
    this.workspaces = [stubWorkspace];
  }

  getCohort(_: string, wsid: string, cid: number): Observable<Cohort> {
    return new Observable<Cohort>(observer => {
      setTimeout(() => {
        observer.next(this.cohorts.find(function(cohort: CohortStub) {
          if (cohort.id === cid && cohort.workspaceId === wsid) {
            return true;
          }
        }));
        observer.complete();
      }, 0);
    });
  }

  updateCohort(ns: string, wsid: string, cid: number, newCohort: Cohort): Observable<Cohort> {
    return new Observable<Cohort>(observer => {
      setTimeout(() => {

        const index = this.cohorts.findIndex(function(cohort: CohortStub) {
          if (cohort.id === cid && cohort.workspaceId) {
            return true;
          }
          return false;
        });
        if (index !== -1) {
          this.cohorts[index] = new CohortStub(newCohort, wsid);
          observer.complete();
        } else {
          observer.error(new Error(`Error updating. No cohort with id: ${cid} `
                                  + `exists in workspace ${ns}, ${wsid} `
                                  + `in cohort service stub`));
        }
      }, 0);
    });
  }

  createCohort(_: string, wsid: string, newCohort: Cohort): Observable<Cohort> {
    return new Observable<Cohort>(observer => {
      setTimeout(() => {
        observer.next(this.cohorts.find(function(cohort: Cohort) {
          if (cohort.id === newCohort.id) {
            observer.error(new Error(`Error creating. Cohort with `
                                    + `id: ${cohort.id} already exists.`));
            return true;
          }
        }));
        this.cohorts.push(new CohortStub(newCohort, wsid));
        observer.complete();
      }, 0);
    });
  }

  getCohortsInWorkspace(ns: string, wsid: string): Observable<CohortListResponse> {
    return new Observable<CohortListResponse>(observer => {
      setTimeout(() => {
        const cohortsInWorkspace: Cohort[] = [];
        this.cohorts.forEach(cohort => {
          if (cohort.workspaceId === wsid) {
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
  }
}
