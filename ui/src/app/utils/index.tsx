import {ElementRef, OnChanges, OnDestroy, OnInit, ViewChild} from '@angular/core';

import {DataAccessLevel, Domain} from 'generated';
import {Domain as FetchDomain} from 'generated/fetch';
import {DataAccessLevel as FetchDataAccessLevel} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

export const WINDOW_REF = 'window-ref';
import {colorWithWhiteness} from 'app/styles/colors';
import {
  cdrVersionStore,
  currentCohortStore,
  currentConceptSetStore,
  currentWorkspaceStore,
  routeConfigDataStore,
  urlParamsStore,
  userProfileStore
} from 'app/utils/navigation';

export function isBlank(toTest: String): boolean {
  if (toTest === null) {
    return true;
  } else {
    toTest = toTest.trim();
    return toTest === '';
  }
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

// TODO: consolidate this with above when UI is fully converted
export function hasRegisteredAccessFetch(access: FetchDataAccessLevel): boolean {
  return [
    FetchDataAccessLevel.Registered,
    FetchDataAccessLevel.Protected
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
        // We allow overriding of the currentValue, for reuse of the same
        // logic outside of the scope of a current workspace.
        return <WrappedComponent {...{[name]: value}} {...this.props}/>;
      }
    }

    return Wrapper;
  };
};

export const connectReplaySubject = <T extends {}>(subject: ReplaySubject<T>, name: string) => {
  return (WrappedComponent) => {
    class Wrapper extends React.Component<any, {value: T}> {
      static displayName = 'connectReplaySubject()';
      private subscription;

      constructor(props) {
        super(props);
        this.state = {value: null};
      }

      componentDidMount() {
        this.subscription = subject.subscribe(v => this.setState({value: v}));
      }

      componentWillUnmount() {
        this.subscription.unsubscribe();
      }

      render() {
        const {value} = this.state;
        // Since ReplaySubject may not have an initial value, only render the
        // connected value once the value is available.
        return value && <WrappedComponent {...{[name]: value}} {...this.props}/>;
      }
    }

    return Wrapper;
  };
};

// HOC that provides a 'workspace' prop with current WorkspaceData
export const withCurrentWorkspace = () => {
  return connectBehaviorSubject(currentWorkspaceStore, 'workspace');
};

// HOC that provides a 'cohort' prop with current Cohort
export const withCurrentCohort = () => {
  return connectBehaviorSubject(currentCohortStore, 'cohort');
};

// HOC that provides a 'conceptSet' prop with current ConceptSet
export const withCurrentConceptSet = () => {
  return connectBehaviorSubject(currentConceptSetStore, 'conceptSet');
};

// HOC that provides a 'profileState' prop with current profile and a reload function
export const withUserProfile = () => {
  return connectBehaviorSubject(userProfileStore, 'profileState');
};

// HOC that provides a 'urlParams' prop with the current url params object
export const withUrlParams = () => {
  return connectBehaviorSubject(urlParamsStore, 'urlParams');
};

// HOC that provides a 'routeConfigData' prop with current route's data object
export const withRouteConfigData = () => {
  return connectBehaviorSubject(routeConfigDataStore, 'routeConfigData');
};

// HOC that provides a 'cdrVersionListResponse' prop with the CDR version
// information. Rendering of the connected component is blocked on initial load
// of the CDR versions. This should only affect initial page loads, this HOC can
// be included last (if multiple HOCs are in use) to minimize this impact.
export const withCdrVersions = () => {
  return connectReplaySubject(cdrVersionStore, 'cdrVersionListResponse');
};

// Temporary method for converting generated/models/Domain to generated/models/fetch/Domain
export function generateDomain(domain: FetchDomain): Domain {
  const d = fp.capitalize(FetchDomain[domain]);
  return Domain[d];
}

// To convert datetime strings into human-readable dates
export function displayDate(time: Number): string {
  const date = new Date(Number(time));
  // datetime formatting to slice off weekday from readable date string
  return date.toLocaleString('en-US',
    {year: '2-digit', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', hour12: true});
}

// Remove this when we complete RW-3065
export function formatRecentResourceDisplayDate(time: string): string {
  if (!time) {
    return '';
  }

  const date = new Date(time);
  // datetime formatting to slice off weekday from readable date string
  return date.toDateString().split(' ').slice(1).join(' ');
}

export function formatDomainString(domainString: string): string {
  return fp.capitalize(domainString);
}

export function formatDomain(domain: FetchDomain): string {
  return formatDomainString(domain.toString());
}

// Given a value and an array, return a new array with the value appended.
export const append = fp.curry((value, arr) => fp.concat(arr, [value]));

// Given a value and an array, return a new array that 'toggles' the presence of the value.
// E.g. remove if it exists, append if it doesn't.
export const toggleIncludes = fp.curry((value, arr) => {
  return fp.includes(value, arr) ? fp.pull(value, arr) : append(value, arr);
});

export function sliceByHalfLength(obj) {
  return Math.ceil(obj.length / 2);
}

// Returns a function which will execute `action` at most once every `sensitivityMs` milliseconds
// if the returned function has been invoked within the last `sensitivityMs` milliseconds
// Example : debouncing user activity events to change rate of invocation from 1000/s to 1/s
export function debouncer(action, sensitivityMs) {
  let t = Date.now();

  const timer = setInterval(() => {
    if (Date.now() - t < sensitivityMs) {
      action();
    }
  }, sensitivityMs);

  return {
    invoke: () => {
      t = Date.now();
    },
    getTimer: () => timer
  };
}

// Starts a timer which will invoke `f` after `timeoutInSeconds` has passed
// Calling the returned function will reset the timer to zero
// Example : Call a logout function after 30 seconds of the returned function not being invoked
export function resettableTimeout(f, timeoutInSeconds) {
  let timeout;
  return {
    reset: () => {
      clearTimeout(timeout);
      timeout = setTimeout(f, timeoutInSeconds);
    },
    getTimer: () => timeout
  };
}

export function highlightSearchTerm(searchTerm: string, stringToHighlight: string, highlightColor: string) {
  if (searchTerm.length < 3) {
    return stringToHighlight;
  }
  const words: string[] = [];
  const matchString = new RegExp(searchTerm.trim(), 'i');
  const matches = stringToHighlight.match(new RegExp(matchString, 'gi'));
  const splits = stringToHighlight.split(new RegExp(matchString, 'gi'));
  if (matches) {
    for (let i = 0; i < matches.length; i++) {
      words.push(splits[i], matches[i]);
    }
    words.push(splits[splits.length - 1]);
  } else {
    words.push(splits[0]);
  }
  return words.map((word, w) => <span key={w}
    style={matchString.test(word.toLowerCase()) ? {
      color: colorWithWhiteness(highlightColor, -0.4),
      backgroundColor: colorWithWhiteness(highlightColor, 0.7),
      display: 'inline-block'
    } : {}}>
      {word}
    </span>);
}

/*
 * A method to run an api call with a specified number of retries and exponential backoff.
 * This method will only error
 * Parameters:
 *    apiCall: Lambda that will run an API call, in the form of () => apiClient.apiCall(args)
 *    maxRetries: The amount of retries the system will take before erroring
 *    defaultWaitTime: How long the base exponential backoff is, in milliseconds
 *      For example, if 1000 is passed in, it will wait 1s for the first retry,
 *      2s for the second, etc.
 */
export async function apiCallWithGatewayTimeoutRetries<T>(
  apiCall: () => Promise<T>, maxRetries = 3, initialWaitTime = 1000): Promise<T> {
  return apiCallWithGatewayTimeoutRetriesAndRetryCount(apiCall, maxRetries, 1, initialWaitTime);
}

async function apiCallWithGatewayTimeoutRetriesAndRetryCount<T>(
  apiCall: () => Promise<T>, maxRetries = 3, retryCount = 1, initialWaitTime = 1000): Promise<T> {
  try {
    return await apiCall();
  } catch (ex) {
    if (ex.status !== 504 || retryCount > maxRetries) {
      throw ex;
    }
    await new Promise(resolve => setTimeout(resolve, initialWaitTime * Math.pow(2, retryCount)));
    return await apiCallWithGatewayTimeoutRetriesAndRetryCount(
      apiCall, maxRetries, retryCount + 1, initialWaitTime);
  }
}
