import * as fp from 'lodash/fp';

export interface Subscribable<T> {
  subscribe: (Function) => { unsubscribe: () => void};
  next: (newValue: T, oldValue: T) => void;
}

export interface Atom<T> {
  get: () => T;
  set: (value: T) => void;
  subscribe: Subscribable<T>['subscribe'];
  reset: () => void;
}

export function subscribable<T>(): Subscribable<T> {
  let subscribers = [];
  type subscriber = ((newValue?: T, oldValue?: T) => void);

  return {
    subscribe: (fn: subscriber) => {
      subscribers = fp.concat(subscribers, [fn]);
      return {
        unsubscribe: () => {
          subscribers = fp.without([fn], subscribers);
        }
      };
    },
    next: (newValue?: T, oldValue?: T) => {
      fp.forEach(fn => setTimeout(() => fn(newValue, oldValue), 0), subscribers);
    }
  };
}

// A simple state container inspired by clojure atoms.
export function atom<T>(initialValue: T): Atom<T> {
  let value = initialValue;
  const { subscribe, next } = subscribable<T>();
  const get = () => value;
  const set = newValue => {
    const oldValue = value;
    value = newValue;
    next(newValue, oldValue);
  };
  return { subscribe, get, set, reset: () => set(initialValue) };
}
