import * as React from 'react';
import { useCallback, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import * as querystring from 'querystring-es3';

import {
  Cohort,
  CohortReview,
  ConceptSet,
  Criteria,
  ErrorResponse,
} from 'generated/fetch';

import { SidebarIconId } from 'app/components/help-sidebar-icons';
import { Selection } from 'app/pages/data/cohort/selection-list';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

// This is an optional warmup store which can be populated to avoid redundant
// requests on navigation, e.g. from workspace creation/clone -> data page. It
// is not guaranteed to be populated for all workspace transitions. The value
// should only be consumed by central infrastructure, but can be updated from
// specific application pages where appropriate.
export const nextWorkspaceWarmupStore = new BehaviorSubject<WorkspaceData>(
  undefined
);

export const currentWorkspaceStore = new BehaviorSubject<WorkspaceData>(
  undefined
);
export interface GroupCount {
  groupId: string;
  groupName: string;
  groupCount: number;
  role: string;
  status: string;
}
export const currentGroupCountsStore = new BehaviorSubject<GroupCount[]>([]);
export const currentCohortStore = new BehaviorSubject<Cohort>(undefined);
export const currentCohortReviewStore = new BehaviorSubject<CohortReview>(
  undefined
);
export const currentConceptSetStore = new BehaviorSubject<ConceptSet>(
  undefined
);
export const systemErrorStore = new BehaviorSubject<ErrorResponse>(undefined);
export const currentCohortCriteriaStore = new BehaviorSubject<Array<Selection>>(
  undefined
);
export const currentConceptStore = new BehaviorSubject<Array<Criteria>>(
  undefined
);
export const attributesSelectionStore = new BehaviorSubject<Criteria>(
  undefined
);
export const currentCohortSearchContextStore = new BehaviorSubject<any>(
  undefined
);
export const sidebarActiveIconStore = new BehaviorSubject<SidebarIconId>(null);
export const conceptSetUpdating = new BehaviorSubject<boolean>(false);

export interface NavigateExtras {
  queryParams?: object;
  preventDefaultIfNoKeysPressed?: boolean;
  event?: React.MouseEvent;
}

export type NavigateFn = (path: string[], extras?: NavigateExtras) => void;
export type NavigateByUrlFn = (url: string, extras?: NavigateExtras) => void;

export interface NavigationProps {
  navigate: NavigateFn;
  navigateByUrl: NavigateByUrlFn;
}

export const useNavigation = (): [NavigateFn, NavigateByUrlFn] => {
  const history = useHistory();

  const navigateByUrl = (url: string, extras?: NavigateExtras) => {
    url = '/' + url.replace(/^\//, '');

    const preventDefaultIfNoKeysPressed =
      extras?.preventDefaultIfNoKeysPressed && !!extras.event;

    // if modifier keys are pressed (like shift or cmd) use the href
    // if no keys are pressed, prevent default behavior and route using navigateByUrl
    if (
      preventDefaultIfNoKeysPressed &&
      !(
        extras.event.shiftKey ||
        extras.event.altKey ||
        extras.event.ctrlKey ||
        extras.event.metaKey
      )
    ) {
      extras.event.preventDefault();
    }

    history.push({
      pathname: url,
      search: extras?.queryParams
        ? querystring.stringify(extras.queryParams)
        : '',
    });
  };

  const navigate = (path: string[], extras?: NavigateExtras) => {
    navigateByUrl(path.join('/'), extras);
  };

  return [navigate, navigateByUrl];
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

export const navigateSignOut = (continuePath: string = '/login') => {
  window.location.assign(
    'https://www.google.com/accounts/Logout?continue=' +
      `https://appengine.google.com/_ah/logout?continue=${window.location.origin}${continuePath}`
  );
};

export interface UrlObj {
  url: string;
  queryParams?: Map<string, string>;
}

export const stringifyUrl = (url: UrlObj) => {
  return (
    url.url +
    (url.queryParams ? '?' + querystring.stringify(url.queryParams) : '')
  );
};

function useMessageListener<T>(message: string, callback: (event: T) => void) {
  const listener = useCallback(
    (event: MessageEvent) => {
      const tanagraUrl = serverConfigStore.get().config.tanagraBaseUrl;
      if (
        event.origin !== tanagraUrl ||
        typeof event.data !== 'object' ||
        event.data.message !== message
      ) {
        return;
      }
      callback(event.data);
    },
    [message, callback]
  );

  useEffect(() => {
    window.addEventListener('message', listener);
    return () => {
      window.removeEventListener('message', listener);
    };
  }, [listener]);
}

export function useExitActionListener(callback: () => void) {
  useMessageListener('CLOSE', () => callback());
}

export interface ExportResources {
  cohortIds: string[];
  featureSetIds: string[];
}

export function useExportListener(
  callback: (exportIds: ExportResources) => void
) {
  useMessageListener(
    'EXPORT',
    (event: { resources: { cohorts: string[]; featureSets: string[] } }) =>
      callback({
        cohortIds: event.resources.cohorts,
        featureSetIds: event.resources.featureSets,
      })
  );
}
