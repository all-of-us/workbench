import * as fp from 'lodash/fp';

export const subscribable = () => {
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

/**
 * A simple state container inspired by clojure atoms. Method names were chosen based on similarity
 * to lodash and Immutable. (deref => get, reset! => set, swap! => update, reset to go back to initial value)
 * Implements the Store interface
 */
export const atom = initialValue => {
  let value = initialValue;
  const { subscribe, next } = subscribable();
  const get = () => value;
  const set = newValue => {
    const oldValue = value;
    value = newValue;
    next(newValue, oldValue);
  };
  return { subscribe, get, set, update: fn => set(fn(get())), reset: () => set(initialValue) };
};
