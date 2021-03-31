import * as fp from 'lodash/fp';
import * as React from 'react';

export const withContextProviders = (...providers) => (WrappedComponent) => (props) => {
  // Using reduceRight so that the first item in the array of providers is the outermost
  // in the DOM
  return fp.reduceRight((provider, acc) => {
    let ContextProvider;
    if (Array.isArray(provider)) {
      // The provider has props in this case.
      ContextProvider = provider[0];
      return <ContextProvider {...provider[1]}>{acc}</ContextProvider>;
    } else {
      ContextProvider = provider;
      return <ContextProvider>{acc}</ContextProvider>;
    }
  }, <WrappedComponent {...props} />)(providers);
};
