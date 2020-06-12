import {useEffect, useState} from 'react';
import {atom, Atom} from './subscribable';

export const routeDataStore = atom({});
export const authStore = atom({});

export const useStore = (theStore: Atom) => {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe(v => setValue(v)).unsubscribe;
  }, [theStore, setValue]);
  return value;
};
