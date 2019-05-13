import {ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy} from '@angular/router';

import {WorkspaceData} from 'app/services/workspace-storage.service';
import {Profile} from 'generated';
import {Cohort, ConceptSet} from 'generated/fetch';
import * as fp from 'lodash/fp';
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
export const userProfileStore =
  new BehaviorSubject<{ profile: Profile, reload: Function, updateCache: Function }>({
    profile: {} as Profile,
    reload: () => {},
    updateCache: (profile) => {},
  });


/**
 * Slightly stricter variant of Angular's DefaultRouteReuseStrategy. This
 * strategy requires identical params for route reuse, in addition to route
 * config matching.
 *
 * This avoid bugs from invalid parameter assumptions in views and for the most
 * part should not affect performance, based on parameter usage in the
 * application.
 */
export class WorkbenchRouteReuseStrategy extends RouteReuseStrategy {
  // The following methods are copied from Angular's DefaultRouteReuseStrategy.
  shouldDetach(route: ActivatedRouteSnapshot): boolean { return false; }
  store(route: ActivatedRouteSnapshot, detachedTree: DetachedRouteHandle): void {}
  shouldAttach(route: ActivatedRouteSnapshot): boolean { return false; }
  retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle|null { return null; }

  shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
    return future.routeConfig === curr.routeConfig
      && (fp.isEqual(future.params, curr.params) || curr.data.shouldReuse);
  }
}

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
  WorkspaceDuplicate = 'WorkspaceDuplicate',
  Notebook = 'Notebook',
  ConceptSet = 'ConceptSet',
  Cohort = 'Cohort',
  Participant = 'Participant',
  CohortAdd = 'CohortAdd',
  Dataset = 'Dataset',
}
