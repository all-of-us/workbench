import * as fp from 'lodash/fp';

export const delay = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * @name Subscribable
 * @description A container allowing components and functions to subscribe to changes in the container's contents
 * @function subscribe Add a subscription by passing a function
 * @function next Send the new value to all subscribers
 **/
export interface Subscribable<T> {
  subscribe: (fn: (newValue?: T, oldValue?: T) => void) => { unsubscribe: () => void};
  next: (newValue: T, oldValue: T) => void;
}

/** @name Atom
 * @description A container that can store a value to be accessed and subscribed to globally
 * @function get Returns the current value
 * @function set Sets the value and triggers all subscribed functions with the old/new values
 * @function subscribe Add a function to the subscribe list
 * @function reset Resets the value to the initial value
 */
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
    subscribe: fn => {
      subscribers = fp.concat(subscribers, [fn]);
      console.log("Added sub ", subscribers);
      return {
        unsubscribe: () => {
          console.log(subscribers);
          subscribers = fp.without([fn], subscribers);
          console.log(subscribers);
        }
      };
    },
    next: (newValue?: T, oldValue?: T) => {
      fp.forEach((fn: subscriber) => setTimeout(
        () => {
//          console.log(fn);
          console.log("Calling sub ", subscribers);
          try {
            fn(newValue, oldValue);
          } catch (e) {
            // Ignore the error - ideally the supplied fn should
            // supply its error handling
          }
        }
        , 0), subscribers);
    }
  };
}

// A simple state container inspired by clojure atoms.
export function atom<T>(initialValue: T): Atom<T> {
  let value = initialValue;
  const { subscribe, next } = subscribable<T>();
  const get = () => {
    if (value && value.pageKey) {
      console.log(value);
    }
    return value;
  }
  const set = newValue => {
    const oldValue = value;
    value = newValue;
    if (newValue && newValue.pageKey) {
      console.log(newValue, oldValue);
    }
    next(newValue, oldValue);
  };
  return { subscribe, get, set, reset: () => set(initialValue) };
}
