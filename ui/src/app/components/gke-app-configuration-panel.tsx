import * as React from 'react';
import { useEffect, useState } from 'react';

import { AppType, UserAppEnvironment } from 'generated/fetch';

import {
  CromwellConfigurationPanel,
  CromwellConfigurationPanelProps,
} from 'app/components/cromwell-configuration-panel';
import {
  RStudioConfigurationPanel,
  RStudioConfigurationPanelProps,
} from 'app/components/rstudio-configuration-panel';
import { Spinner } from 'app/components/spinners';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { cond } from 'app/utils';
import { notificationStore } from 'app/utils/stores';

type InjectedProps = 'gkeAppsInWorkspace';

export type GkeAppConfigurationPanelProps = {
  type: AppType;
  workspaceNamespace: string;
} & Omit<CromwellConfigurationPanelProps, InjectedProps> &
  Omit<RStudioConfigurationPanelProps, InjectedProps>;

export const GKEAppConfigurationPanel = ({
  type,
  workspaceNamespace,
  ...props
}: GkeAppConfigurationPanelProps) => {
  const [gkeAppsInWorkspace, setGkeAppsInWorkspace] = useState<
    UserAppEnvironment[] | undefined
  >();
  const [error, setError] = useState<Error>();

  useEffect(() => {
    appsApi()
      .listAppsInWorkspace(workspaceNamespace)
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

  return cond(
    [
      type === AppType.CROMWELL,
      () => (
        <CromwellConfigurationPanel
          {...{
            ...props,
            gkeAppsInWorkspace,
          }}
        />
      ),
    ],
    // AppType.RStudio
    () => (
      <RStudioConfigurationPanel
        {...{
          ...props,
          gkeAppsInWorkspace,
        }}
      />
    )
  );
};
