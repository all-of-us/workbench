import * as React from 'react';
import * as fp from 'lodash/fp';

import { Criteria, CriteriaSubType, Domain } from 'generated/fetch';

import { colorWithWhiteness } from 'app/styles/colors';
import {
  currentCohortCriteriaStore,
  currentCohortReviewStore,
  currentCohortSearchContextStore,
  currentCohortStore,
  currentConceptSetStore,
  currentConceptStore,
  currentWorkspaceStore,
  systemErrorStore,
} from 'app/utils/navigation';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { ReplaySubject } from 'rxjs/ReplaySubject';

import {
  CdrVersionStore,
  cdrVersionStore,
  profileStore,
  withStore,
} from './stores';
import { WorkspaceData } from './workspace-data';

const { useEffect, useState } = React;

export function isBlank(toTest: String): boolean {
  if (toTest === null || toTest === undefined) {
    return true;
  } else {
    toTest = toTest.trim();
    return toTest === '';
  }
}

/**
 * Random String Generator (simplified version)
 *
 * Info:      http://stackoverflow.com/a/27872144/383904
 * Use:       randomString(length);
 * Default:   return a random alpha-numeric string
 */
export function randomString(len): string {
  let str = '',
    i = 0;
  for (; i++ < len; ) {
    let rand = Math.floor(Math.random() * 62);
    const charCode = (rand += rand > 9 ? (rand < 36 ? 55 : 61) : 48);
    str += String.fromCharCode(charCode);
  }
  return str;
}

const throttleAnimation = (fn) => {
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

window.addEventListener(
  'resize',
  throttleAnimation(() => {
    window.dispatchEvent(new CustomEvent('resizeAnimation'));
  })
);

export interface WindowSize {
  height: number;
  width: number;
}
// Components using withWindowSize can use this props interface to ensure they are including the
// correctly-named props, e.g. "interface MyClassProps extends WindowSizeProps".
export interface WindowSizeProps {
  windowSize: WindowSize;
}

const getWindowSize = (): WindowSize => {
  return { height: window.innerHeight, width: window.innerWidth };
};

export const withWindowSize = () => (WrappedComponent) => {
  class Wrapper extends React.Component<any, { windowSize: WindowSize }> {
    constructor(props) {
      super(props);
      this.state = { windowSize: getWindowSize() };
    }

    static displayName = 'withWindowSize()';

    resize = () => {
      const { windowSize } = this.state;
      const newSize = getWindowSize();
      if (!fp.isEqual(windowSize, newSize)) {
        this.setState({ windowSize: newSize });
      }
    };

    componentDidMount() {
      window.addEventListener('resizeAnimation', this.resize);
    }

    componentWillUnmount() {
      window.removeEventListener('resizeAnimation', this.resize);
    }

    render() {
      const { windowSize } = this.state;
      return <WrappedComponent windowSize={windowSize} {...this.props} />;
    }
  }

  return Wrapper;
};

export const nextSort = ({ field, direction }, newField) => {
  return newField === field
    ? { field, direction: direction === 'asc' ? 'desc' : 'asc' }
    : { field: newField, direction: 'asc' };
};

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
export function reactStyles<T extends { [key: string]: React.CSSProperties }>(
  t: T
): ReactStyles<T> {
  return t;
}

export function decamelize(str: string, separator: string) {
  separator = typeof separator === 'undefined' ? '_' : separator;

  return str
    .replace(/([a-z\d])([A-Z])/g, '$1' + separator + '$2')
    .replace(/([A-Z]+)([A-Z][a-z\d]+)/g, '$1' + separator + '$2')
    .toLowerCase();
}

export const withStyle = (styleObj) => (WrappedComponent) => {
  const Wrapper = React.forwardRef(({ style = {}, ...props }: any, ref) => {
    return (
      <WrappedComponent
        ref={ref}
        style={{ ...styleObj, ...style }}
        {...props}
      />
    );
  });
  Wrapper.displayName = 'withStyle';
  return Wrapper;
};

export const summarizeErrors = (errors) => {
  const errorList = fp.cond([
    [fp.isPlainObject, fp.flatMap(fp.values)],
    [fp.isArray, fp.identity],
    [() => true, () => []],
  ])(errors);
  if (errorList.length) {
    return errorList.map((v, i) => {
      return (
        <div key={i} style={{ marginTop: i !== 0 ? '0.375rem' : undefined }}>
          {v}
        </div>
      );
    });
  }
};

export const connectBehaviorSubject = <T extends {}>(
  subject: BehaviorSubject<T>,
  name: string,
  preventRenderUntilDataIsPresent: boolean = false
) => {
  return (WrappedComponent) => {
    class Wrapper extends React.Component<any, { value: T }> {
      static displayName = 'connectBehaviorSubject()';
      private subscription;

      constructor(props) {
        super(props);
        this.state = { value: subject.getValue() };
      }

      componentDidMount() {
        this.subscription = subject.subscribe((v) => {
          this.setState({ value: v });
        });
      }

      componentWillUnmount() {
        this.subscription.unsubscribe();
      }

      render() {
        const { value } = this.state;
        // We allow overriding of the currentValue, for reuse of the same
        // logic outside of the scope of a current workspace.
        if (preventRenderUntilDataIsPresent && value == null) {
          return null;
        }
        return <WrappedComponent {...{ [name]: value }} {...this.props} />;
      }
    }

    return Wrapper;
  };
};

export const connectReplaySubject = <T extends {}>(
  subject: ReplaySubject<T>,
  name: string
) => {
  return (WrappedComponent) => {
    class Wrapper extends React.Component<any, { value: T }> {
      static displayName = 'connectReplaySubject()';
      private subscription;

      constructor(props) {
        super(props);
        this.state = { value: null };
      }

      componentDidMount() {
        this.subscription = subject.subscribe((v) =>
          this.setState({ value: v })
        );
      }

      componentWillUnmount() {
        this.subscription.unsubscribe();
      }

      render() {
        const { value } = this.state;
        // Since ReplaySubject may not have an initial value, only render the
        // connected value once the value is available.
        return (
          value && <WrappedComponent {...{ [name]: value }} {...this.props} />
        );
      }
    }

    return Wrapper;
  };
};

// HOC that provides a 'workspace' prop with current WorkspaceData
export const withCurrentWorkspace = () => {
  return connectBehaviorSubject(currentWorkspaceStore, 'workspace');
};
export interface WithCurrentWorkspace {
  workspace: WorkspaceData;
}

// HOC that provides a 'cohort' prop with current Cohort
export const withCurrentCohort = () => {
  return connectBehaviorSubject(currentCohortStore, 'cohort');
};

// HOC that provides a 'cohortReview' prop with current CohortReview
export const withCurrentCohortReview = () => {
  return connectBehaviorSubject(currentCohortReviewStore, 'cohortReview');
};

// HOC that provides a 'criteria' prop with current Cohort
export const withCurrentCohortCriteria = () => {
  return connectBehaviorSubject(currentCohortCriteriaStore, 'criteria');
};

export const withCurrentConcept = () => {
  return connectBehaviorSubject(currentConceptStore, 'concept');
};

export const withCurrentCohortSearchContext = () => {
  return connectBehaviorSubject(
    currentCohortSearchContextStore,
    'cohortContext'
  );
};

// HOC that provides a 'conceptSet' prop with current ConceptSet
export const withCurrentConceptSet = () => {
  return connectBehaviorSubject(currentConceptSetStore, 'conceptSet');
};

export const withSystemError = () => {
  return connectBehaviorSubject(systemErrorStore, 'systemError');
};

// HOC that provides a 'profileState' prop with current profile and a reload function
export const withUserProfile = () => {
  return withStore(profileStore, 'profileState');
};

// HOC that provides a 'cdrVersionTiersResponse' prop with the CDR version information.
export const withCdrVersions = () => {
  return withStore(cdrVersionStore, 'cdrVersionTiersResponse');
};
export interface WithCdrVersions {
  cdrVersionTiersResponse: CdrVersionStore;
}

export function formatDomainString(domainString: string): string {
  return domainString === Domain.PHYSICAL_MEASUREMENT_CSS.toString()
    ? fp.capitalize(Domain.PHYSICAL_MEASUREMENT.toString())
    : domainString === Domain.WHOLE_GENOME_VARIANT.toString()
    ? 'VCF Files'
    : fp.capitalize(domainString);
}

export function formatDomain(domain: Domain): string {
  return formatDomainString(domain.toString());
}

// Given a value and an array, return a new array with the value appended.
export const append = fp.curry((value, arr) => fp.concat(arr, [value]));

// Given a value and an array, return a new array that 'toggles' the presence of the value.
// E.g. remove if it exists, append if it doesn't.
export const toggleIncludes = fp.curry(
  <T extends {}>(value: T, arr: Array<T>) => {
    return fp.includes(value, arr) ? fp.pull(value, arr) : append(value, arr);
  }
);

export function sliceByHalfLength(obj) {
  return Math.ceil(obj.length / 2);
}

export function hasNewValidProps(currProps, prevProps, fieldsToCompare) {
  for (const fieldGetter of fieldsToCompare) {
    if (!fieldGetter(currProps)) {
      return false;
    }

    if (fieldGetter(currProps) !== fieldGetter(prevProps)) {
      return true;
    }
  }

  return false;
}

// Returns a function which will execute `action` at most once every `sensitivityMs` milliseconds
// if the returned function has been invoked within the last `sensitivityMs` milliseconds
// Example : debouncing user activity events to change rate of invocation from 1000/s to 1/s
export function debouncer(action, sensitivityMs) {
  let t = Date.now();

  const timer = global.setInterval(() => {
    if (Date.now() - t < sensitivityMs) {
      action();
    }
  }, sensitivityMs);

  return {
    invoke: () => {
      t = Date.now();
    },
    getTimer: () => timer,
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
    getTimer: () => timeout,
  };
}

export function highlightSearchTerm(
  searchTerm: string,
  stringToHighlight: string,
  highlightColor: string
) {
  if (searchTerm.length < 3) {
    return stringToHighlight;
  }
  try {
    const words: string[] = [];
    const matchString = new RegExp(
      searchTerm.replace(/([^a-zA-z0-9 \-<>=\/]+)/g, () => '').trim(),
      'i'
    );
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
    return words.map((word, w) => (
      <span
        key={w}
        style={
          matchString.test(word.toLowerCase())
            ? {
                color: colorWithWhiteness(highlightColor, -0.4),
                backgroundColor: colorWithWhiteness(highlightColor, 0.7),
                display: 'inline-block',
              }
            : {}
        }
      >
        {word}
      </span>
    ));
  } catch (e) {
    return stringToHighlight;
  }
}

// render a float value as US currency, rounded to cents: 255.372793 -> $255.37
// negative values are rendered as $0
export function formatInitialCreditsUSD(value?: number): string {
  if (!value || value <= 0.0) {
    return '$0';
  } else {
    return '$' + value.toFixed(2).toString();
  }
}

export const useDebounce = (fn: Function, updateList = [], ms = 250) => {
  useEffect(() => {
    const timeoutId = setTimeout(fn, ms);
    return () => clearTimeout(timeoutId);
  }, updateList);
};

// Allows the state to be set to true, triggering a render. This hook will automatically set the state back to false.
// initial use case: rendering the Navigation component
export const useToggle = (): [boolean, Function] => {
  const [state, setState] = useState(false);

  useEffect(() => {
    if (state) {
      setState(false);
    }
  }, [state]);

  return [state, setState];
};

export const withAsyncErrorHandling = fp.curry(
  (
      handler: (error: Error) => void,
      fnToTry: (...args: any[]) => Promise<any>
    ) =>
    async (...args) => {
      try {
        return await fnToTry(...args);
      } catch (error) {
        handler(error);
      }
    }
);

// Takes a search string and validates for the most common MySQL use cases.
// Checks for unclosed "", trailing + or -, and breaking special characters.
export function validateInputForMySQL(
  searchString: string,
  searchTrigger: number
): Array<string> {
  const inputErrors = new Set<string>(); // use Set to prevent duplicate messages
  let unclosedQuotes = false;
  for (let i = 0; i < searchString.length; i++) {
    const character = searchString[i];
    if (character === '"') {
      unclosedQuotes = !unclosedQuotes;
      continue;
    }
    if (unclosedQuotes) {
      // inside a quote, no need to validate further
      continue;
    }
    // Check for characters that break search
    if ('~@[]|<>()'.indexOf(character) > -1) {
      inputErrors.add(
        'The following characters are not allowed in the search string: ~ @ [ ] | < > ( )'
      );
      continue;
    }
    // Check for trailing + or -
    if (
      '+-'.indexOf(character) > -1 &&
      (searchString[i + 1] === ' ' || searchString[i + 1] === undefined)
    ) {
      inputErrors.add(
        `Trailing ${character} characters are not allowed in the search string`
      );
    }
  }
  if (unclosedQuotes) {
    // unclosed quote
    inputErrors.add('There is an unclosed " in the search string');
  }
  const quotesPattern = /([+\-]?"[^"]*")/;
  searchString
    .trim()
    .split(quotesPattern)
    .filter((subString) => subString.length > 0)
    .forEach((subString) => {
      // Only validate strings that aren't inside quotes
      if (!subString.match(quotesPattern)) {
        // check each endsWith term - terms that start with *
        const termsArray = subString.trim().split(' ');
        termsArray.forEach((word) => {
          // consecutive special chars
          if (word.match(/[+*-]{2,}/)) {
            inputErrors.add(
              `Search term [${word}] cannot contain consecutive special characters `
            );
          }
          if (word.match(/^\*.*\*$/)) {
            inputErrors.add(
              `Search term [${word}] cannot start and end in wild character '*'`
            );
          }
          // length of every word without the special chars mut be >= searchTrigger
          // for hyphenated word there will be at least 2 letters, if hyphen is removed
          if (word.replace(/[+*"'-]/g, '').length < searchTrigger) {
            inputErrors.add(
              `Search term [${word}] length must be at least ${searchTrigger} characters without special characters`
            );
          }
        });
        if (termsArray.length === 1 && termsArray[0].match(/^-/)) {
          inputErrors.add(
            `Search term [${termsArray[0]}] with one word cannot start with a minus character '-'`
          );
        }
      } else if (subString.replace(/[+*"'-]/g, '').length < searchTrigger) {
        // Still need to check the quoted string for length
        inputErrors.add(
          `Search term [${subString}] length must be at least ${searchTrigger} characters without special characters`
        );
      }
    });
  return Array.from(inputErrors);
}

// lensOnProps - inspired by lenses in RamdaJS https://ramdajs.com/docs/#lens
// This is a lens implementation that will change the key names of a set of properties
// lensProps(['a', 'b], ['x', 'y'], {x: 1, y: 2}) -> {a: 1, b: 2}
export const lensOnProps = fp.curry(
  (setters: string[], getters: string[], obj: object): object => {
    return fp.flow(
      fp.zip(getters),
      fp.map<[string, string], [string, object]>(([fromProp, toProp]) => [
        toProp,
        obj[fromProp],
      ]),
      fp.fromPairs
    )(setters);
  }
);

// useId: provides a unique ID for an element. Useful for using aria-labelledby
export const useId = () => {
  const [id] = useState(() => fp.uniqueId('element-'));
  return id;
};

export const nothing = {};

// maybe - takes a function and a value. If the value is not defined returns "nothing"
// Example usage: fp.flow(maybe(doSomethingIfIhaveData), maybe(doAnotherThingIfThereIsAResult))(getData())
export const maybe = fp.curry((fn, value) =>
  value !== nothing && value ? fn(value) : nothing
);

// normalize username (email) to remove the domain if it exists
export const usernameWithoutDomain = (username: string) => {
  if (!username) {
    return '';
  }
  const atIdx = username.indexOf('@');
  return atIdx === -1 ? username : username.substring(0, atIdx);
};

export const capStringWithEllipsis = (value: string, maxLength: number) =>
  value?.length > maxLength ? value.slice(0, maxLength) + '...' : value;

export const updateCriteriaSelectionStore = (
  criteriaLookupItems: Criteria[],
  domain: Domain
) => {
  const updatedSelections = currentCohortCriteriaStore.getValue().map((sel) => {
    const criteriaMatch = criteriaLookupItems.find(
      (item) =>
        item.conceptId === sel.conceptId &&
        item.standard === sel.standard &&
        (domain !== Domain.SURVEY || item.subtype === sel.subtype) &&
        (sel.subtype !== CriteriaSubType.ANSWER.toString() ||
          item.value === sel.code)
    );
    if (criteriaMatch) {
      sel.id = criteriaMatch.id;
    }
    return sel;
  });
  currentCohortCriteriaStore.next(updatedSelections);
};
