import {useEffect, useState} from 'react';
import {atom} from './subscribable';

export const routeDataStore = atom({});
export const authStore = atom({});

export const useStore = theStore => {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe(v => setValue(v)).unsubscribe;
  }, [theStore, setValue]);
  return value;
};
