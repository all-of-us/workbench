import {Router} from '@angular/router';
import {fromJS} from 'immutable';
import {
  find,
  isEqual
} from 'lodash/fp';
import * as React from 'react';

import {DataAccessLevel} from 'generated';

export const WINDOW_REF = 'window-ref';

export function isBlank(toTest: String): boolean {
  if (toTest === null) {
    return true;
  } else {
    toTest = toTest.trim();
    return toTest === '';
  }
}

export function deepCopy(obj: Object): Object {
  return fromJS(obj).toJS();
}

/**
 * Determine whether the given access level is >= registered. This is the
 * minimum required level to do most things in the Workbench app (outside of
 * local/test development).
 */
export function hasRegisteredAccess(access: DataAccessLevel): boolean {
  return [
    DataAccessLevel.Registered,
    DataAccessLevel.Protected
  ].includes(access);
}


/**
 * Random String Generator (simplified version)
 *
 * Info:      http://stackoverflow.com/a/27872144/383904
 * Use:       randomString(length);
 * Default:   return a random alpha-numeric string
 */
export function randomString(len): string {
  let str = '', i = 0;
  for (; i++ < len;) {
    let rand = Math.floor(Math.random() * 62);
    const charCode = rand += rand > 9 ? (rand < 36 ? 55 : 61) : 48;
    str += String.fromCharCode(charCode);
  }
  return str;
}

export const DEFAULT = Symbol();

export const switchCase = (value, ...pairs) => {
  const match = find(([v]) => v === value || v === DEFAULT, pairs);
  return match && match[1]();
};

const throttleAnimation = fn => {
  let running = false;
  return (...args) => {
    if (!running) {
      running = true;
      window.requestAnimationFrame(() => {
        running = false;
        fn(...args);
      });
    }
  };
};

window.addEventListener('resize', throttleAnimation(() => {
  window.dispatchEvent(new CustomEvent('resizeAnimation'));
}));

const getWindowSize = () => {
  return { height: window.innerHeight, width: window.innerWidth };
};

export const withWindowSize = () => WrappedComponent => {
  class Wrapper extends React.Component<
    any,
    { windowSize: { width: number, height: number } }
  > {
    constructor(props) {
      super(props);
      this.state = { windowSize: getWindowSize() };
    }

    static displayName = 'withWindowSize()';

    resize = () => {
      const { windowSize } = this.state;
      const newSize = getWindowSize();
      if (!isEqual(windowSize, newSize)) {
        this.setState({ windowSize: newSize });
      }
    }

    componentDidMount() {
      window.addEventListener('resizeAnimation', this.resize);
    }

    componentWillUnmount() {
      window.removeEventListener('resizeAnimation', this.resize);
    }

    render() {
      const { windowSize } = this.state;
      return React.createElement(WrappedComponent, { windowSize, ...this.props });
    }
  }
  return Wrapper as any;
};

export const nextSort = ({ field, direction }, newField) => {
  return newField === field ?
    { field, direction: direction === 'asc' ? 'desc' : 'asc' } :
    { field: newField, direction: 'asc' };
};
