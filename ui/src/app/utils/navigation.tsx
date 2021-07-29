import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, CohortReview, ConceptSet, Criteria, ErrorResponse} from 'generated/fetch';
import * as querystring from 'querystring';
import * as React from 'react';
import {useLocation} from 'react-router';
import {useHistory} from 'react-router-dom';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {URLSearchParams} from 'url';

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

export const useNavigation = () => {
  const history = useHistory();

  const navigate = (commands, extras?: NavigateExtras) => {
    // url should always lead with a slash so that the given url replaces the current one
    const url = '/' + commands.join('/').replace(/^\//, '');
    history.push({
      pathname: url,
      search: extras && extras.queryParams ? querystring.stringify(extras.queryParams) : ''
    });
  };

  // TODO angular2react - refactor this with navigate
  // TODO angular2react - add type to extras
  const navigateByUrl = (url, extras?: NavigateExtras) => {
    url = '/' + url.replace(/^\//, '');

    const preventDefaultIfNoKeysPressed = extras && extras.preventDefaultIfNoKeysPressed && !!extras.event;

    // if modifier keys are pressed (like shift or cmd) use the href
    // if no keys are pressed, prevent default behavior and route using navigateByUrl
    if (preventDefaultIfNoKeysPressed && !(extras.event.shiftKey || extras.event.altKey || extras.event.ctrlKey || extras.event.metaKey)) {
      extras.event.preventDefault();
    }

    history.push({
      pathname: url,
      search: extras && extras.queryParams ? querystring.stringify(extras.queryParams) : ''
    });
  };

  return [navigate, navigateByUrl];
};

interface NavigateExtras {
  queryParams?: object;
  preventDefaultIfNoKeysPressed?: boolean;
  event?: React.MouseEvent;
}

export interface NavigationProps {
  navigate: (commands, extras?: NavigateExtras) => void;
  navigateByUrl: (commands, extras?: NavigateExtras) => void;
}

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
