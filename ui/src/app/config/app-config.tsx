import * as React from 'react';
import { useEffect } from 'react';
import { AuthProvider } from 'react-oidc-context';

import { Maintenance } from 'app/pages/maintenance';
import { AppRoutingComponent } from 'app/routing/app-routing';
import { configApi } from 'app/services/swagger-fetch-clients';
import { makeOIDC } from 'app/utils/authentication';
import { serverConfigStore, useStore } from 'app/utils/stores';

export const AppConfigComponent = () => {
  const { config, isDown } = useStore(serverConfigStore);

  useEffect(() => {
    const load = async () => {
      try {
        const serverConfig = await configApi().getConfig();
        serverConfigStore.set({ config: serverConfig });
      } catch (error) {
        serverConfigStore.set({ isDown: true });
      }
    };

    load();
  }, []);

  return (
    <>
      {/* for outdated-browser-rework https://www.npmjs.com/package/outdated-browser-rework*/}
      {/* Check checkBrowserSupport() function called in index.ts and implemented in setup.ts*/}
      <div id='outdated' />
      {/* TODO: Change config in the serverConfigStore to be non-undefined to simplify downstream components.*/}
      {isDown && <Maintenance />}
      {config && (
        <AuthProvider {...makeOIDC(config)}>
          <AppRoutingComponent />
        </AuthProvider>
      )}
    </>
  );
};
