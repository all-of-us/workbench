import {WorkspaceData} from 'app/resolvers/workspace';
import {Profile} from 'generated';
import {Cohort, ConceptSet} from 'generated/fetch';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export const NavStore = {
  navigate: undefined,
  navigateByUrl: undefined
};

export const currentWorkspaceStore = new BehaviorSubject<WorkspaceData>(undefined);
export const currentCohortStore = new BehaviorSubject<Cohort>(undefined);
export const currentConceptSetStore = new BehaviorSubject<ConceptSet>(undefined);
export const urlParamsStore = new BehaviorSubject<any>({});
export const queryParamsStore = new BehaviorSubject<any>({});
export const routeConfigDataStore = new BehaviorSubject<any>({});
export const userProfileStore = new BehaviorSubject<{ profile: Profile, reload: Function }>({
  profile: {} as Profile,
  reload: () => {}
});

// NOTE: Because these are wired up directly to the router component,
// all navigation done from here will effectively use absolute paths.
export const navigate = (...args) => {
  return NavStore.navigate(...args);
};

export const navigateByUrl = (...args) => {
  return NavStore.navigateByUrl(...args);
};

export enum BreadcrumbType {
  Workspaces = 'Workspaces',
  Workspace = 'Workspace',
  WorkspaceEdit = 'WorkspaceEdit',
  WorkspaceClone = 'WorkspaceClone',
  Notebook = 'Notebook',
  ConceptSet = 'ConceptSet',
  Cohort = 'Cohort',
  Participant = 'Participant',
  CohortAdd = 'CohortAdd',
  Dataset = 'Dataset',
}
