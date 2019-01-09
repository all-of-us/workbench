import {Component, DoCheck, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {fromJS} from 'immutable';
import {
  find,
  fromPairs,
  isEqual
} from 'lodash/fp';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

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

/**
 * See feature-detects/cookies.js in https://github.com/Modernizr
 *
 * navigator.cookieEnabled cannot detect custom or nuanced cookie blocking
 * configurations. For example, when blocking cookies via the Advanced
 * Privacy Settings in IE9, it always returns true. And there have been
 * issues in the past with site-specific exceptions.
 * Don't rely on it.
 *
 * try..catch because some in situations `document.cookie` is exposed but throws a
 * SecurityError if you try to access it; e.g. documents created from data URIs
 * or in sandboxed iframes (depending on flags/context)
 */
export function cookiesEnabled(): boolean {
    try {
        // Create cookie
        document.cookie = 'cookietest=1';
        const ret = document.cookie.indexOf('cookietest=') !== -1;
        // Delete cookie
        document.cookie = 'cookietest=1; expires=Thu, 01-Jan-1970 00:00:01 GMT';
        return ret;
    } catch (e) {
        return false;
    }
}

export const ReactComponent = ({ propNames = [], ...options }) => WrappedComponent => {
  class WrapperComponent implements DoCheck, OnInit, OnDestroy {
    @ViewChild('root')
    rootElement: ElementRef;

    ngOnInit(): void {
      this.renderComponent();
    }

    ngDoCheck(): void {
      this.renderComponent();
    }

    ngOnDestroy(): void {
      ReactDOM.unmountComponentAtNode(this.rootElement.nativeElement);
    }

    renderComponent(): void {
      ReactDOM.render(
        React.createElement(
          WrappedComponent,
          fromPairs(propNames.map(name => [name, this[name]]))
        ),
        this.rootElement.nativeElement
      );
    }
  }
  propNames.forEach(name => {
    Input()(WrapperComponent.prototype, name, undefined);
  });

  return Component({
    ...options,
    template: '<div #root></div>'
  })(WrapperComponent) as any;
};
