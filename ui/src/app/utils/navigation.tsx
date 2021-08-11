import {ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy} from '@angular/router';

import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, CohortReview, ConceptSet, Criteria, ErrorResponse} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {useLocation} from 'react-router';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

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
export const currentCohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const currentConceptSetStore = new BehaviorSubject<ConceptSet>(undefined);
export const globalErrorStore = new BehaviorSubject<ErrorResponse>(undefined);
export const urlParamsStore = new BehaviorSubject<any>({});
export const queryParamsStore = new BehaviorSubject<any>({});
export const routeConfigDataStore = new BehaviorSubject<any>({});
export const currentCohortCriteriaStore = new BehaviorSubject<Array<Selection>>(undefined);
export const currentConceptStore = new BehaviorSubject<Array<Criteria>>(undefined);
export const attributesSelectionStore = new BehaviorSubject<Criteria>(undefined);
export const currentCohortSearchContextStore = new BehaviorSubject<any>(undefined);
export const setSidebarActiveIconStore = new BehaviorSubject<string>(null);
export const conceptSetUpdating = new BehaviorSubject<boolean>(false);

export const signInStore =
  new BehaviorSubject<{
    signOut: Function,
    profileImage: string,
  }>({
    signOut: () => {},
    profileImage: {} as string,
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

export const navigateSignOut = (continuePath: string = '/login') => {
  window.location.assign(`https://www.google.com/accounts/Logout?continue=` +
    `https://appengine.google.com/_ah/logout?continue=${window.location.origin}${continuePath}`);
};

/**
 * Retrieve query parameters from the React Router.
 *
 * Example:
 *  my/query/page?user=alice123
 *  reactRouterUrlSearchParams.get('user') -> value is 'alice123'
 */
export const reactRouterUrlSearchParams = (): URLSearchParams => {
  return new URLSearchParams(useLocation().search);
};

export enum BreadcrumbType {
  Workspaces = 'Workspaces',
  Workspace = 'Workspace',
  WorkspaceEdit = 'WorkspaceEdit',
  WorkspaceDuplicate = 'WorkspaceDuplicate',
  Notebook = 'Notebook',
  ConceptSet = 'ConceptSet',
  Cohort = 'Cohort',
  CohortReview = 'CohortReview',
  Participant = 'Participant',
  CohortAdd = 'CohortAdd',
  SearchConcepts = 'SearchConcepts',
  Dataset = 'Dataset',
  Data = 'Data',
}
