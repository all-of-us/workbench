import {ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy} from '@angular/router';

import {WorkspaceData} from 'app/utils/workspace-data';
import {ConfigResponse} from 'generated';
import {CdrVersionListResponse, Cohort, ConceptSet, Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

export const NavStore = {
  navigate: undefined,
  navigateByUrl: undefined
};

// This is an optional warmup store which can be populated to avoid redundant
// requests on navigation, e.g. from workspace creation/clone -> data page. It
// is not guaranteed to be populated for all workspace transitions. The value
// should only be consumed by central infrastructure, but can be updated from
// specific application pages where appropriate.
export const nextWorkspaceWarmupStore = new BehaviorSubject<WorkspaceData>(undefined);

export const currentWorkspaceStore = new BehaviorSubject<WorkspaceData>(undefined);
export const currentCohortStore = new BehaviorSubject<Cohort>(undefined);
export const currentConceptSetStore = new BehaviorSubject<ConceptSet>(undefined);
export const urlParamsStore = new BehaviorSubject<any>({});
export const queryParamsStore = new BehaviorSubject<any>({});
export const routeConfigDataStore = new BehaviorSubject<any>({});
export const serverConfigStore = new BehaviorSubject<ConfigResponse>(undefined);
export const userProfileStore =
  new BehaviorSubject<{ profile: Profile, reload: Function, updateCache: Function }>({
    profile: {} as Profile,
    reload: () => {},
    updateCache: (profile) => {},
  });
export const signInStore =
  new BehaviorSubject<{
    signOut: Function,
    profileImage: string,
  }>({
    signOut: () => {},
    profileImage: {} as string,
  });

// Use ReplaySubject over BehaviorSubject as this store does not have a legal
// initial value and should not be accessed synchronously. The other stores
// which meet this criteria should likely follow this same pattern, though a
// broader redesign of these value stores is also probably in order.
export const cdrVersionStore = new ReplaySubject<CdrVersionListResponse>(1);

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

/**
 * Strict variant of URI encoding to satisfy Angular routing, specifically for
 * handling parens. See https://github.com/angular/angular/issues/10280 for
 * details. This should no longer be necessary with Angular 6.
 */
export const encodeURIComponentStrict = (uri: string): string => {
  return encodeURIComponent(uri).replace(/[!'()*]/g, (c) => {
    return '%' + c.charCodeAt(0).toString(16);
  });
};

// if modifier keys are pressed (like shift or cmd) use the href
// if no keys are pressed, prevent default behavior and route using navigateByUrl
export const navigateAndPreventDefaultIfNoKeysPressed = (e: React.MouseEvent, url: string) => {
  if (!(e.shiftKey || e.altKey || e.ctrlKey || e.metaKey)) {
    e.preventDefault();
    navigateByUrl(url);
  }
};

export const navigateSignOut = () => {
  window.location.assign(`https://www.google.com/accounts/Logout?continue=` +
    `https://appengine.google.com/_ah/logout?continue=${window.location.origin}/login`);
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
  SearchConcepts = 'SearchConcepts',
  Dataset = 'Dataset',
  Data = 'Data',
}
