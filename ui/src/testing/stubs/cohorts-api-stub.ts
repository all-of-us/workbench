import {convertToResources, ResourceType} from 'app/utils/resourceActionsReact';
import {
  Cohort,
  CohortsApi,
  EmptyResponse,
  RecentResource,
  Workspace,
  WorkspaceAccessLevel
} from 'generated/fetch';
import {WorkspaceStubVariables} from './workspace-service-stub';

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

export class CohortsApiStub extends CohortsApi {
  public workspaces: Workspace[];
  public cohorts: CohortStub[];
  public resourceList: RecentResource[];

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });

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

  updateCohort(ns: string, wsid: string, cid: number, newCohort: Cohort): Promise<Cohort> {
    return new Promise<Cohort>(observer => {

    });
  }

  deleteCohort(ns: string, wsid: string, id: number): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      setTimeout(() => {
        const cohortIndex = this.cohorts.findIndex(cohort => cohort.id === id);
        if (cohortIndex === -1) {
          throw new Error('Cohort not found in workspace.');
        }
        this.cohorts.splice(cohortIndex, 1);
        resolve({});
      }, 0);
    });
  }
}
