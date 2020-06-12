import {useEffect, useState} from 'react';
import {atom, Atom} from './subscribable';

interface RouteDataStore {
  title?: string;
}

export const routeDataStore = atom <RouteDataStore>({});

export interface AuthStore {
  authLoaded: boolean;
  isSignedIn: boolean;
}

export const authStore = atom<AuthStore>({authLoaded: false, isSignedIn: false});

export function useStore<T>(theStore: Atom<T>) {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe(v => setValue(v)).unsubscribe;
  }, [theStore, setValue]);
  return value;
}
