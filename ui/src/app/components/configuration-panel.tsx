import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';

import { CromwellConfigurationPanel } from './cromwell-configuration-panel';
import { RuntimeConfigurationPanel } from './runtime-configuration-panel';

export const ConfigurationPanel = fp.flow(withCurrentWorkspace())(
  ({ onClose, workspace, type = 'runtime', runtimeConfPanelInitialState }) => {
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
        {type === 'runtime' ? (
          <div>
            <RuntimeConfigurationPanel
              {...{ onClose, creatorFreeCreditsRemaining }}
              initialPanelContent={runtimeConfPanelInitialState}
            />
          </div>
        ) : (
          <CromwellConfigurationPanel
            {...{ onClose, creatorFreeCreditsRemaining, workspace }}
          />
        )}
      </div>
    );
  }
);
