import * as React from 'react';
import { AuthContextProps } from 'react-oidc-context';
import { useParams } from 'react-router';

import {
  CdrVersionTier,
  ConfigResponse,
  CreateNewUserSatisfactionSurvey,
  Disk,
  ListAppsResponse,
  Profile,
  Runtime,
  TerraJobStatus,
} from 'generated/fetch';

import { BreadcrumbType } from 'app/components/breadcrumb-type';
import { dataSetApi, profileApi } from 'app/services/swagger-fetch-clients';
import { Atom, atom } from 'app/utils/subscribable';
import { StackdriverErrorReporter } from 'stackdriver-errors-js';
import useSWR from 'swr';

const { useEffect, useState } = React;

export interface RouteDataStore {
  title?: string;
  minimizeChrome?: boolean;
  pageKey?: string;
  breadcrumb?: BreadcrumbType;
  pathElementForTitle?: string;
  contentFullHeightOverride?: boolean;
  workspaceNavBarTab?: string;
}

export const routeDataStore = atom<RouteDataStore>({});

export interface AuthStore {
  authLoaded: boolean;
  isSignedIn: boolean;
  auth?: AuthContextProps;
}

export const authStore = atom<AuthStore>({
  authLoaded: false,
  isSignedIn: false,
});

interface CdrVersionStore {
  tiers?: Array<CdrVersionTier>;
}

export const cdrVersionStore = atom<CdrVersionStore>({});

export const useGenomicExtractionJobs = (
  workspaceNamespace: string,
  workspaceId: string,
  pollWhileNonTerminal = true
) =>
  useSWR(
    `/api/workspaces/${workspaceNamespace}/${workspaceId}/genomicExtractionJobs`,
    () =>
      dataSetApi()
        .getGenomicExtractionJobs(workspaceNamespace, workspaceId)
        .then(({ jobs }) => jobs),
    {
      // Genomic jobs will rarely change without user interaction. Avoid some extraneous revalidation.
      revalidateOnFocus: false,
      refreshInterval: (data) => {
        if (
          pollWhileNonTerminal &&
          data?.some(({ status }) =>
            [TerraJobStatus.RUNNING, TerraJobStatus.ABORTING].includes(status)
          )
        ) {
          return 10 * 1000;
        }
        return 0;
      },
    }
  );

// HOC for genomic extraction jobs compatibility with class-based components.
// New components should use the useGenomicExtractionJobs() hook.
export const withGenomicExtractionJobs = (WrappedComponent) => (props) => {
  const { data } = useGenomicExtractionJobs(
    props.workspace.namespace,
    props.workspace.id
  );
  return <WrappedComponent genomicExtractionJobs={data} {...props} />;
};

export interface ProfileStore {
  profile?: Profile;
  load: Function;
  reload: Function;
  updateCache: Function;
}

export const profileStore = atom<ProfileStore>({
  profile: null,
  load: async () => {
    if (!profileStore.get().profile) {
      await profileStore.get().reload();
    }
    return profileStore.get().profile;
  },
  reload: async () => {
    const newProfile = await profileApi().getMe();
    profileStore.get().updateCache(newProfile);
    return profileStore.get().profile;
  },
  updateCache: (p: Profile): void =>
    profileStore.set({
      ...profileStore.get(),
      profile: p,
    }),
});

export interface CreateNewUserSatisfactionSurveyStore {
  newUserSatisfactionSurveyData: CreateNewUserSatisfactionSurvey;
}

export const createNewUserSatisfactionSurveyStore =
  atom<CreateNewUserSatisfactionSurveyStore>({
    newUserSatisfactionSurveyData: {
      satisfaction: undefined,
      additionalInfo: '',
    },
  });

export interface NotificationStore {
  title: string;
  message: string;
  showBugReportLink?: boolean;
  onDismiss?: () => void;
}

export const notificationStore = atom<NotificationStore | null>(null);

export interface CompoundRuntimeOperation {
  pendingRuntime?: Runtime;
  aborter: AbortController;
}

export interface CompoundRuntimeOpStore {
  [workspaceNamespace: string]: CompoundRuntimeOperation;
}

// Store tracking any compound Runtime operations per workspace. Currently, this
// only pertains to applying a runtime configuration update via full recreate
// (compound operation of delete -> create).
export const compoundRuntimeOpStore = atom<CompoundRuntimeOpStore>({});

export const registerCompoundRuntimeOperation = (
  workspaceNamespace: string,
  runtimeOperation: CompoundRuntimeOperation
) => {
  compoundRuntimeOpStore.set({
    ...compoundRuntimeOpStore.get(),
    [workspaceNamespace]: runtimeOperation,
  });
};

export const markCompoundRuntimeOperationCompleted = (
  workspaceNamespace: string
) => {
  const { [workspaceNamespace]: op, ...otherOps } =
    compoundRuntimeOpStore.get();
  if (op) {
    op.aborter.abort();
    compoundRuntimeOpStore.set(otherOps);
  }
};

export const clearCompoundRuntimeOperations = () => {
  const ops = compoundRuntimeOpStore.get();
  Object.keys(ops).forEach((k) => ops[k].aborter.abort());
  compoundRuntimeOpStore.set({});
};

// runtime store states: undefined(initial state) -> Runtime (user selected) <--> null (delete only - no recreate)
// error should be set if there is a failure in initializing the store value. If
// error is set, runtimeLoaded should be false.
export interface RuntimeStore {
  workspaceNamespace: string | null | undefined;
  runtime: Runtime | null | undefined;
  runtimeLoaded: boolean;
  loadingError?: Error;
}

export const runtimeStore = atom<RuntimeStore>({
  workspaceNamespace: undefined,
  runtime: undefined,
  runtimeLoaded: false,
  loadingError: undefined,
});

// runtime store states: undefined(initial state) -> Runtime (user selected) <--> null (delete only - no recreate)
export interface DiskStore {
  workspaceNamespace: string | null | undefined;
  gcePersistentDisk: Disk | null | undefined;
}

export const diskStore = atom<DiskStore>({
  workspaceNamespace: undefined,
  gcePersistentDisk: undefined,
});

export interface StackdriverErrorReporterStore {
  reporter?: StackdriverErrorReporter;
}

export const stackdriverErrorReporterStore =
  atom<StackdriverErrorReporterStore>({});

export interface ServerConfigStore {
  config?: ConfigResponse;
}

export const serverConfigStore = atom<ServerConfigStore>({});

// These fields come from the colon-prefixed params declared on router paths.
// See app-routing, signed-in-app-routing, and workspace-app-routing for examples.
// If you want to add a new route param, you will need to define it here as well.
export interface MatchParams {
  cid?: string;
  crid?: string;
  csid?: string;
  dataSetId?: string;
  domain?: string;
  eventId?: string;
  institutionId?: string;
  nbName?: string;
  ns?: string;
  pid?: string;
  sparkConsolePath?: string;
  username?: string;
  usernameWithoutGsuiteDomain?: string;
  wsid?: string;
}

/**
 * HOC which invalidates a component when the specified params change, by way
 * of changing the React key value. This can be used to force a remount / render
 * when a route param changes.
 */
export function withParamsKey(...paramNames: (keyof MatchParams)[]) {
  return (WrappedComponent) =>
    ({ ...props }) => {
      const params = useParams<MatchParams>();
      return (
        <WrappedComponent
          key={paramNames.map((k) => params[k] || '').join('/')}
          {...props}
        />
      );
    };
}

export interface UserAppsStore {
  updating?: boolean;
  userApps?: ListAppsResponse;
  timeoutID?: any;
}

export const userAppsStore = atom<UserAppsStore>({});

/**
 * @name useStore
 * @description React hook that will trigger a render when the corresponding store's value changes
 *              this should only be used in function components
 * @param {Atom<T>} theStore A container for the value to be updated
 */
export function useStore<T>(theStore: Atom<T>) {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe((v) => setValue(v)).unsubscribe;
  }, [theStore]);
  return value;
}

/**
 * HOC that injects the value of the given store as a prop. When the store changes, the wrapped
 * component will re-render
 */
export const withStore = (theStore, name) => (WrappedComponent) => {
  return (props) => {
    const value = useStore(theStore);
    const storeProp = { [name]: value };
    return <WrappedComponent {...props} {...storeProp} />;
  };
};
