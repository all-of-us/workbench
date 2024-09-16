import {
  Cohort,
  CohortsApi,
  EmptyResponse,
  ResourceType,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResource,
} from 'generated/fetch';
import { CohortListResponse } from 'generated/fetch';

import { convertToResources } from './resources-stub';
import { WorkspaceStubVariables } from './workspaces';

export const DEFAULT_COHORT_ID = 1;
export const DEFAULT_COHORT_ID_2 = 2;

export const exampleCohortStubs: CohortStub[] = [
  {
    id: DEFAULT_COHORT_ID,
    name: 'sample name',
    description: 'sample description',
    criteria:
      '{"includes":[{"temporal": false,"items":[]},{"temporal": false,"items":[]}],"excludes":[],"dataFilters":[]}',
    type: '',
    terraName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
    creationTime: new Date().getTime(),
    lastModifiedTime: new Date().getTime() - 1000,
  },
  {
    id: DEFAULT_COHORT_ID_2,
    name: 'sample name 2',
    description: 'sample description 2',
    criteria:
      '{"includes":[{"temporal": false,"items":[]},{"temporal": false,"items":[]}],' +
      '"excludes":[{"temporal": false,"items":[]}],"dataFilters":[]}',
    type: '',
    terraName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
    creationTime: new Date().getTime(),
    lastModifiedTime: new Date().getTime() - 4000,
  },
];

class CohortStub implements Cohort {
  id?: number;

  name: string;

  criteria: string;

  type: string;

  description?: string;

  creator?: string;

  creationTime?: number;

  lastModifiedTime?: number;

  terraName: string;

  constructor(cohort: Cohort, terraName: string) {
    this.creationTime = cohort.creationTime;
    this.creator = cohort.creator;
    this.criteria = cohort.criteria;
    this.description = cohort.description;
    this.id = cohort.id;
    this.lastModifiedTime = cohort.lastModifiedTime;
    this.name = cohort.name;
    this.type = cohort.type;
    this.terraName = terraName;
  }
}

export class CohortsApiStub extends CohortsApi {
  public workspaces: Workspace[];
  public cohorts: CohortStub[];
  public resourceList: WorkspaceResource[];

  constructor() {
    super(undefined);

    const stubWorkspace: Workspace = {
      displayName: WorkspaceStubVariables.DEFAULT_WORKSPACE_DISPLAY_NAME,
      terraName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
      namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
    };

    this.cohorts = exampleCohortStubs;
    this.workspaces = [stubWorkspace];
    this.resourceList = convertToResources(this.cohorts, ResourceType.COHORT, {
      ...stubWorkspace,
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  }

  updateCohort(
    ns: string,
    terraName: string,
    cid: number,
    newCohort: Cohort
  ): Promise<Cohort> {
    return new Promise<Cohort>((resolve, reject) => {
      const index = this.cohorts.findIndex((cohort: CohortStub) => {
        if (cohort.id === cid && cohort.terraName) {
          return true;
        }
        return false;
      });
      if (index !== -1) {
        const newCohortStub = new CohortStub(newCohort, terraName);
        this.cohorts[index] = newCohortStub;
        resolve(newCohortStub);
      } else {
        reject(
          new Error(
            `Error updating. No cohort with id: ${cid} ` +
              `exists in workspace ${ns}, ${terraName} ` +
              'in cohort service stub'
          )
        );
      }
    });
  }

  deleteCohort(
    ns: string,
    terraName: string,
    id: number
  ): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      const cohortIndex = this.cohorts.findIndex((cohort) => cohort.id === id);
      if (cohortIndex === -1) {
        throw new Error('Cohort not found in workspace.');
      }
      this.cohorts.splice(cohortIndex, 1);
      resolve({});
    });
  }

  getCohortsInWorkspace(
    ns: string,
    terraName: string
  ): Promise<CohortListResponse> {
    return new Promise<CohortListResponse>((resolve, reject) => {
      const cohortsInWorkspace: Cohort[] = [];
      this.cohorts.forEach((cohort) => {
        if (cohort.terraName === terraName) {
          cohortsInWorkspace.push(cohort);
        }
      });
      if (cohortsInWorkspace.length === 0) {
        reject('No cohorts in workspace.');
      } else {
        resolve({ items: cohortsInWorkspace });
      }
    });
  }

  getCohort(
    _ns: string,
    _terraName: string,
    cohortId: number
  ): Promise<Cohort> {
    const cohort =
      this.cohorts.find((c) => c.id === cohortId) || this.cohorts[0];
    return new Promise<Cohort>((resolve) => resolve(cohort));
  }
}
