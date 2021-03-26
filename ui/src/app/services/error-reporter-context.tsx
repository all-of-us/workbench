import * as React from "react";
import {createContext, useEffect, useState} from "react";

import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {atom} from "app/utils/subscribable";
import {serverConfigStore, useStore} from "app/utils/stores";
import {configApi} from "app/services/swagger-fetch-clients";
import {serverConfigStore as angularServerConfigStore} from "app/utils/navigation";
import {environment} from "environments/environment";

interface StackdriverReporterStore {
  reporter?: StackdriverErrorReporter;
}

const stackdriverReporterStore = atom<StackdriverReporterStore>({});

const StackdriverErrorReporterContext = createContext({
  stackdriverErrorReporter: null
});

export const StackdriverReporterProvider = ({children}) => {
  useStore(stackdriverReporterStore);
  const [config, setConfig] = useState();

  useEffect(() => {
    if (environment.debug) {
      // This is a local dev server, we want to disable Stackdriver reporting as
      // it's not useful and likely won't work due to the origin.
      console.log("debug");
      return;
    }

    async function getServerConfig () {
      if (config) {
        return;
      } else if (angularServerConfigStore.getValue()) {
        setConfig(angularServerConfigStore.getValue())
      } else if (serverConfigStore.get()) {
        setConfig(serverConfigStore.get());
      } else {
        // This probably won't ever be reached, as both components are set in app/pages/app.component.ts
        const config = await configApi().getConfig();
        angularServerConfigStore.next(config);
        serverConfigStore.set({config: config});
        setConfig(config);
      }
    }

    getServerConfig();

    if (config) {
      const reporter = new StackdriverErrorReporter();
      reporter.start({
        key: config.publicApiKeyForErrorReports,
        projectId: config.projectId,
      });
      stackdriverReporterStore.set(reporter);
    }
    else {
      console.log('wtf but in context');
    }
  });

  return <StackdriverErrorReporterContext.Provider value={{
    stackdriverErrorReporter: stackdriverReporterStore.get().reporter
  }}>
    {children}
  </StackdriverErrorReporterContext.Provider>
}

// This HOC can be used to wrap class components that need StackdriverErrorReporterContext injected.
// For function components, using useContext(StackdriverErrorReporterContext) is preferred.
export const withStackdriverErrorReporterContext = Component =>
    props => (
        <StackdriverErrorReporterContext.Consumer>
          {context => <Component stackdriverErrorReporterContext={context} {...props}/>}
        </StackdriverErrorReporterContext.Consumer>
    );
