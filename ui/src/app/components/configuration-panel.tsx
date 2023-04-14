import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { BillingStatus } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { RStudioConfigurationPanel } from 'app/components/rstudio-configuration-panel';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { cond, withCurrentWorkspace, withUserProfile } from 'app/utils';
import { ProfileStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CromwellConfigurationPanel } from './cromwell-configuration-panel';
import {
  RuntimeConfigurationPanel,
  RuntimeConfigurationPanelProps,
} from './runtime-configuration-panel';
import { DisabledPanel } from './runtime-configuration-panel/disabled-panel';

export interface ConfigurationPanelProps {
  onClose: () => void;
  type: UIAppType;
  runtimeConfPanelInitialState?: RuntimeConfigurationPanelProps['initialPanelContent'];
}

export const ConfigurationPanel = fp.flow(
  withCurrentWorkspace(),
  withUserProfile()
)(
  ({
    onClose,
    workspace,
    type,
    runtimeConfPanelInitialState = null,
    profileState,
  }: ConfigurationPanelProps & {
    workspace: WorkspaceData;
    profileState: ProfileStore;
  }) => {
    const { namespace, id } = workspace;
    const [creatorFreeCreditsRemaining, setCreatorFreeCreditsRemaining] =
      useState(null);

    useEffect(() => {
      const aborter = new AbortController();
      const fetchFreeCredits = async () => {
        const { freeCreditsRemaining } =
          await workspacesApi().getWorkspaceCreatorFreeCreditsRemaining(
            namespace,
            id,
            { signal: aborter.signal }
          );
        setCreatorFreeCreditsRemaining(freeCreditsRemaining);
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
            workspace.billingStatus === BillingStatus.INACTIVE,
            () => <DisabledPanel />,
          ],
          [
            type === UIAppType.JUPYTER,
            () => (
              <div>
                <RuntimeConfigurationPanel
                  {...{ onClose, creatorFreeCreditsRemaining }}
                  initialPanelContent={runtimeConfPanelInitialState}
                  profileState={profileState}
                />
              </div>
            ),
          ],
          [
            type === UIAppType.CROMWELL,
            () => (
              <CromwellConfigurationPanel
                {...{
                  onClose,
                  creatorFreeCreditsRemaining,
                  workspace,
                  profileState,
                }}
              />
            ),
          ],
          // UIAppType.RStudio
          () => (
            <RStudioConfigurationPanel
              {...{
                onClose,
                creatorFreeCreditsRemaining,
                workspace,
                profileState,
              }}
            />
          )
        )}
      </div>
    );
  }
);
