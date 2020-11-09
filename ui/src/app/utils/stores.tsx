import { BreadcrumbType } from 'app/utils/navigation';
import {atom, Atom} from 'app/utils/subscribable';
import {Profile} from 'generated';
import {Runtime} from 'generated/fetch';
import * as React from 'react';

const {useEffect, useState} = React;

export interface RouteDataStore {
  title?: string;
  minimizeChrome?: boolean;
  helpContentKey?: string;
  breadcrumb?: BreadcrumbType;
  pathElementForTitle?: string;
  notebookHelpSidebarStyles?: boolean;
  contentFullHeightOverride?: boolean;
}

export const routeDataStore = atom<RouteDataStore>({});

interface AuthStore {
  authLoaded: boolean;
  isSignedIn: boolean;
}

export const authStore = atom<AuthStore>({authLoaded: false, isSignedIn: false});

interface ProfileStore {
  profile?: Profile;
}

export const profileStore = atom<ProfileStore>({});

export interface RuntimeOperation {
  pendingRuntime?: Runtime;
  aborter: AbortController;
}

interface WorkspaceRuntimeOperationMap {
  [workspaceNamespace: string]: RuntimeOperation;
}

export const runtimeOpsStore = atom<WorkspaceRuntimeOperationMap>({});

export const updateRuntimeOpsStoreForWorkspaceNamespace = (workspaceNamespace: string, runtimeOperation: RuntimeOperation) => {
  runtimeOpsStore.set({
    ...runtimeOpsStore.get(),
    [workspaceNamespace]: runtimeOperation
  });
};

export const markRuntimeOperationCompleteForWorkspace = (workspaceNamespace: string) => {
  const ops = runtimeOpsStore.get();
  if (ops[workspaceNamespace]) {
    delete ops[workspaceNamespace];
    runtimeOpsStore.set(ops);
  }
};

export const abortRuntimeOperationForWorkspace = (workspaceNamespace: string) => {
  const ops = runtimeOpsStore.get();
  if (ops[workspaceNamespace]) {
    ops[workspaceNamespace].aborter.abort();
    delete ops[workspaceNamespace];
    runtimeOpsStore.set(ops);
  }
};

export const clearRuntimeOperationStore = () => {

};

// runtime store states: undefined(initial state) -> Runtime (user selected) <--> null (delete only - no recreate)
export interface RuntimeStore {
  workspaceNamespace: string | null | undefined;
  runtime: Runtime | null | undefined;
}

export const runtimeStore = atom<RuntimeStore>({workspaceNamespace: undefined, runtime: undefined});

/**
 * @name useStore
 * @description React hook that will trigger a render when the corresponding store's value changes
 *              this should only be used in function components
 * @param {Atom<T>} theStore A container for the value to be updated
 */
export function useStore<T>(theStore: Atom<T>) {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe(v => setValue(v)).unsubscribe;
  }, [theStore]);
  return value;
}

/**
 * HOC that injects the value of the given store as a prop. When the store changes, the wrapped
 * component will re-render
 */
export const withStore = (theStore, name) => WrappedComponent => {
  return (props) => {
    const value = useStore(theStore);
    const storeProp = {[name]: value};
    return <WrappedComponent {...props} {...storeProp}/> ;
  };
};
