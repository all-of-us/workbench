import { MatchParams } from 'app/components/app-router';
import * as React from 'react';
import {useState} from 'react';

export interface ParamsContextProps {
  paramsContext: {
    params: MatchParams;
    setParams: Function;
  };
}

export const ParamsContext = React.createContext<ParamsContextProps>({
  paramsContext: {
    params: {},
    setParams: (newParams: MatchParams) => {}
  }
});

export const ParamsContextProvider = ({children}) => {
  const [params, setParams] = useState<MatchParams>({});

  return <ParamsContext.Provider value={{paramsContext: {params, setParams}}}>
    {children}
  </ParamsContext.Provider>;
};

// A HOC to use with class components to inject ParamsContext into the wrapped component's props
export const withParamsContext = WrappedComponent => {
  return props => (
      <ParamsContext.Consumer>
        {context => <WrappedComponent {...context} {...props}/>}
      </ParamsContext.Consumer>
  );
};
