import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { BillingStatus } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { cond, withCurrentWorkspace } from 'app/utils';
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

export const ConfigurationPanel = fp.flow(withCurrentWorkspace())(
  ({
    onClose,
    workspace,
    type,
    runtimeConfPanelInitialState = null,
  }: ConfigurationPanelProps & { workspace: WorkspaceData }) => {
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
                />
              </div>
            ),
          ],
          [
            type === UIAppType.CROMWELL,
            () => (
              <CromwellConfigurationPanel
                {...{ onClose, creatorFreeCreditsRemaining, workspace }}
              />
            ),
          ],
          // UIAppType.RStudio
          () => null
        )}
      </div>
    );
  }
);
