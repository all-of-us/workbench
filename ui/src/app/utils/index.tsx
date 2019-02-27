import {ElementRef, OnChanges, OnDestroy, OnInit, ViewChild} from '@angular/core';

import {DataAccessLevel, Domain} from 'generated';
import {Domain as FetchDomain} from 'generated/fetch';
import {fromJS} from 'immutable';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export const WINDOW_REF = 'window-ref';
import {currentCohortStore, currentConceptSetStore, currentWorkspaceStore, routeConfigDataStore, urlParamsStore, userProfileStore} from 'app/utils/navigation';

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
  const match = fp.find(([v]) => v === value || v === DEFAULT, pairs);
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
  return {height: window.innerHeight, width: window.innerWidth};
};

export const withWindowSize = () => WrappedComponent => {
  class Wrapper extends React.Component<any,
    { windowSize: { width: number, height: number } }> {
    constructor(props) {
      super(props);
      this.state = {windowSize: getWindowSize()};
    }

    static displayName = 'withWindowSize()';

    resize = () => {
      const {windowSize} = this.state;
      const newSize = getWindowSize();
      if (!fp.isEqual(windowSize, newSize)) {
        this.setState({windowSize: newSize});
      }
    }

    componentDidMount() {
      window.addEventListener('resizeAnimation', this.resize);
    }

    componentWillUnmount() {
      window.removeEventListener('resizeAnimation', this.resize);
    }

    render() {
      const {windowSize} = this.state;
      return <WrappedComponent windowSize={windowSize} {...this.props} />;
    }
  }

  return Wrapper as any;
};

export const nextSort = ({field, direction}, newField) => {
  return newField === field ?
    {field, direction: direction === 'asc' ? 'desc' : 'asc'} :
    {field: newField, direction: 'asc'};
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

type ReactStyles<T> = {
  readonly [P in keyof T]: React.CSSProperties;
};

/**
 * Helper to assert the React.CSSProperties type for all properties in a tuple,
 * while maintaining property names. This will fail compilation if input CSS
 * properties are invalid and will avoid the need for a type assertion on the
 * output. Also makes the properties readonly.
 *
 * This is a workaround to an issue in the Typescript compiler which should
 * eventually be fixed: https://github.com/Microsoft/TypeScript/issues/11152.
 *
 * This approach works only with a single-level nested tuples currently:
 * const styles = reactStyles({
 *   style1: {color: 'red'},
 *   style2: {color: 'blue', position: 'relative'}
 * });
 *
 * Alternatively, style tuples can be cast individually (with arbitrary nesting):
 * const styles = {
 *   style1: {color: 'red'} as React.CssProperties,
 *   style2: {color: 'blue', position: 'relative'} as React.CssProperties
 * };
 */
export function reactStyles<T extends {[key: string]: React.CSSProperties }>(t: T): ReactStyles<T> {
  return t;
}

/**
 * Helper base class for defining an Angular-wrapped React component. This is a
 * stop-gap for React migration.
 *
 * Requirements:
 *  - Component template must contain a div labeled "#root".
 *  - React propNames must exactly match instance property names on the subclass
 *    (usually these are also annotated as Angular @Inputs)
 */
export class ReactWrapperBase implements OnChanges, OnInit, OnDestroy {
  @ViewChild('root') rootElement: ElementRef;

  constructor(private WrappedComponent: React.ComponentType, private propNames: string[]) {}

  ngOnInit(): void {
    this.renderComponent();
  }

  ngOnChanges(): void {
    this.renderComponent();
  }

  ngOnDestroy(): void {
    ReactDOM.unmountComponentAtNode(this.rootElement.nativeElement);
  }

  renderComponent(): void {
    const {WrappedComponent, propNames} = this;
    ReactDOM.render(
      <WrappedComponent {...fp.fromPairs(propNames.map(name => [name, this[name]]))} />,
      this.rootElement.nativeElement
    );
  }
}

export function decamelize(str: string, separator: string) {
  separator = typeof separator === 'undefined' ? '_' : separator;

  return str
    .replace(/([a-z\d])([A-Z])/g, '$1' + separator + '$2')
    .replace(/([A-Z]+)([A-Z][a-z\d]+)/g, '$1' + separator + '$2')
    .toLowerCase();
}

export const withStyle = styleObj => WrappedComponent => {
  const Wrapper = React.forwardRef(({ style = {}, ...props }: any, ref) => {
    return <WrappedComponent ref={ref} style={{...styleObj, ...style}} {...props} />;
  });
  Wrapper.displayName = 'withStyle';
  return Wrapper;
};

export const summarizeErrors = errors => {
  const errorList = fp.cond([
    [fp.isPlainObject, fp.flatMap(fp.values)],
    [fp.isArray, fp.identity],
    [() => true, () => []]
  ])(errors);
  if (errorList.length) {
    return errorList.map((v, i) => {
      return <div key={i} style={{marginTop: i !== 0 ? '0.25rem' : undefined}}>{v}</div>;
    });
  }
};

export const connectBehaviorSubject = <T extends {}>(subject: BehaviorSubject<T>, name: string) => {
  return (WrappedComponent) => {
    class Wrapper extends React.Component<any, {value: T}> {
      static displayName = 'connectBehaviorSubject()';
      private subscription;

      constructor(props) {
        super(props);
        this.state = {value: subject.getValue()};
      }

      componentDidMount() {
        this.subscription = subject.subscribe(v => this.setState({value: v}));
      }

      componentWillUnmount() {
        this.subscription.unsubscribe();
      }

      render() {
        const {value} = this.state;
        return <WrappedComponent {...this.props} {...{[name]: value}} />;
      }
    }

    return Wrapper;
  };
};

// HOC that provides a 'workspace' prop with current WorkspaceData
export const withCurrentWorkspace = () => {
  return connectBehaviorSubject(currentWorkspaceStore, 'workspace');
};

// HOC that provides a 'workspace' prop with current WorkspaceData
export const withCurrentCohort = () => {
  return connectBehaviorSubject(currentCohortStore, 'cohort');
};

// HOC that provides a 'workspace' prop with current WorkspaceData
export const withCurrentConceptSet = () => {
  return connectBehaviorSubject(currentConceptSetStore, 'conceptSet');
};

// HOC that provides a 'profileState' prop with current profile and a reload function
export const withUserProfile = () => {
  return connectBehaviorSubject(userProfileStore, 'profileState');
};

// HOC that provides a 'profileState' prop with current profile and a reload function
export const withUrlParams = () => {
  return connectBehaviorSubject(urlParamsStore, 'urlParams');
};

// HOC that provides a 'profileState' prop with current profile and a reload function
export const withRouteConfigData = () => {
  return connectBehaviorSubject(routeConfigDataStore, 'routeConfigData');
};

// Temporary method for converting generated/models/Domain to generated/models/fetch/Domain
export function generateDomain(domain: FetchDomain): Domain {
  const d = fp.capitalize(FetchDomain[domain]);
  return Domain[d];
}
