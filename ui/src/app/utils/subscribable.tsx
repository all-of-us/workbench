import * as fp from 'lodash/fp';

export interface Subscribable {
  subscribe: (Function) => { unsubscribe: () => void};
  next: (newValue: any, oldValue: any) => void;
}

export interface Atom {
  get: () => any;
  set: (any) => void;
  subscribe: Subscribable['subscribe'];
  reset: () => void;
}

export const subscribable = (): Subscribable => {
  let subscribers = [];
  return {
    subscribe: fn => {
      subscribers = fp.concat(subscribers, [fn]);
      return {
        unsubscribe: () => {
          subscribers = fp.without([fn], subscribers);
        }
      };
    },
    next: (...args) => {
      fp.forEach(fn => fn(...args), subscribers);
    }
  };
};

// A simple state container inspired by clojure atoms.
export const atom = (initialValue): Atom => {
  let value = initialValue;
  const { subscribe, next } = subscribable();
  const get = () => value;
  const set = newValue => {
    const oldValue = value;
    value = newValue;
    next(newValue, oldValue);
  };
  return { subscribe, get, set, reset: () => set(initialValue) };
};
