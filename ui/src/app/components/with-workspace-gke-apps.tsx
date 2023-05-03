import * as React from 'react';
import { useEffect, useState } from 'react';

import { UserAppEnvironment } from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';

export interface WithWorkspaceGKEAppsProps {
  workspaceNamespace: string;
}

interface WrappedComponentRequiredProps {
  gkeAppsInWorkspace: NonNullable<UserAppEnvironment[]>;
}

// Given a Component, returns a new Component that injects a gkeAppsInWorkspace prop into the original component.
// TODO: Validate that gkeAppsInWorkspace in WrappedComponent is the correct type
export const withWorkspaceGkeApps = <P,>(
  WrappedComponent: React.FC<Omit<P, keyof WrappedComponentRequiredProps>>
): React.FC<
  Omit<P, keyof WrappedComponentRequiredProps> & WithWorkspaceGKEAppsProps
> => {
  return (props) => {
    const [gkeAppsInWorkspace, setGkeAppsInWorkspace] = useState<
      UserAppEnvironment[] | undefined
    >();
    const [error, setError] = useState<Error>();

    useEffect(() => {
      appsApi()
        .listAppsInWorkspace(props.workspaceNamespace)
        .then(setGkeAppsInWorkspace)
        .catch((e) => {
          setError(e);
          notificationStore.set({
            title: 'Unable to load applications',
            message:
              'An error occurred trying to load your applications. Please try again.',
          });
        });
    }, []);

    if (error) {
      return null;
    }

    // loading
    if (gkeAppsInWorkspace === undefined) {
      return <Spinner />;
    }

    return <WrappedComponent {...{ ...props, gkeAppsInWorkspace }} />;
  };
};
