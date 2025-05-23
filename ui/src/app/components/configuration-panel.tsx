import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { cond } from '@terra-ui-packages/core-utils';
import { toAppType, UIAppType } from 'app/components/apps-panel/utils';
import {
  GKEAppConfigurationPanel,
  GkeAppConfigurationPanelProps,
} from 'app/components/gke-app-configuration-panel';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { ProfileStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  RuntimeConfigurationPanel,
  RuntimeConfigurationPanelProps,
} from './runtime-configuration-panel';

export interface ConfigurationPanelProps {
  onClose: () => void;
  appType: UIAppType;
  runtimeConfPanelInitialState?: RuntimeConfigurationPanelProps['initialPanelContent'];
  gkeAppConfPanelInitialState?: GkeAppConfigurationPanelProps['initialPanelContent'];
}

export const ConfigurationPanel = fp.flow(
  withCurrentWorkspace(),
  withUserProfile()
)(
  ({
    onClose,
    workspace,
    appType,
    runtimeConfPanelInitialState = null,
    gkeAppConfPanelInitialState = null,
    profileState,
  }: ConfigurationPanelProps & {
    workspace: WorkspaceData;
    profileState: ProfileStore;
  }) => {
    const { namespace, terraName } = workspace;
    const [creatorInitialCreditsRemaining, setCreatorInitialCreditsRemaining] =
      useState(null);

    useEffect(() => {
      const aborter = new AbortController();
      const fetchFreeCredits = async () => {
        const { freeCreditsRemaining } =
          await workspacesApi().getWorkspaceCreatorFreeCreditsRemaining(
            namespace,
            terraName,
            { signal: aborter.signal }
          );
        setCreatorInitialCreditsRemaining(freeCreditsRemaining);
      };

      fetchFreeCredits();

      return function cleanup() {
        aborter.abort();
      };
    }, []);

    return (
      <div id='configuration-panel-container'>
        {cond(
          [
            appType === UIAppType.JUPYTER,
            () => (
              <div>
                <RuntimeConfigurationPanel
                  {...{ onClose, profileState, creatorInitialCreditsRemaining }}
                  initialPanelContent={runtimeConfPanelInitialState}
                />
              </div>
            ),
          ],
          () => (
            <GKEAppConfigurationPanel
              {...{
                onClose,
                creatorInitialCreditsRemaining,
                workspace,
                profileState,
              }}
              appType={toAppType[appType]}
              workspaceNamespace={workspace.namespace}
              initialPanelContent={gkeAppConfPanelInitialState}
            />
          )
        )}
      </div>
    );
  }
);
