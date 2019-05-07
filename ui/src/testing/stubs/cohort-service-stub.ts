import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortListResponse,
  EmptyResponse,
  Workspace,
} from 'generated';

import {RecentResource, WorkspaceAccessLevel} from 'generated/fetch';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';


export let DEFAULT_COHORT_ID = 1;
export let DEFAULT_COHORT_ID_2 = 2;


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
  public resourceList: RecentResource[];

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
      lastModifiedTime: new Date().getTime() - 1000,
    };

    const exampleCohortTwo: CohortStub = {
      id: DEFAULT_COHORT_ID_2,
      name: 'sample name 2',
      description: 'sample description 2',
      criteria: '',
      type: '',
      workspaceId: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      creationTime: new Date().getTime(),
      lastModifiedTime: new Date().getTime() - 4000,
    };
    this.cohorts = [exampleCohort, exampleCohortTwo];
    this.workspaces = [stubWorkspace];
    this.resourceList = convertToResources(this.cohorts, stubWorkspace.namespace,
      stubWorkspace.id, WorkspaceAccessLevel.OWNER, ResourceType.COHORT);
  }

  getCohort(_: string, wsid: string, cid: number): Observable<Cohort> {
    return new Observable<Cohort>(observer => {
      setTimeout(() => {
        observer.next(this.cohorts.find((cohort: CohortStub) => {
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

        const index = this.cohorts.findIndex((cohort: CohortStub) => {
          if (cohort.id === cid && cohort.workspaceId) {
            return true;
          }
          return false;
        });
        if (index !== -1) {
          const newCohortStub = new CohortStub(newCohort, wsid);
          this.cohorts[index] = newCohortStub;
          observer.next(newCohortStub);
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
        observer.next(this.cohorts.find((cohort: Cohort) => {
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

  deleteCohort(ns: string, wsid: string, id: number): Observable<EmptyResponse> {
    return new Observable<EmptyResponse>(observer => {
      setTimeout(() => {
        const cohortIndex = this.cohorts.findIndex(cohort => cohort.id === id);
        if (cohortIndex === -1) {
          observer.error('Cohort not found in workspace.');
        }
        this.cohorts.splice(cohortIndex, 1);
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}
