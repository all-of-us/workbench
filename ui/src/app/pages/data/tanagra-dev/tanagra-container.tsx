import * as React from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { getAccessToken } from 'app/utils/authentication';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { useExitActionListener, useNavigation } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

export const TanagraContainer = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(
  ({
    cdrVersionTiersResponse,
    hideSpinner,
    workspace: { cdrVersionId, id, namespace },
  }) => {
    const [navigate] = useNavigation();
    const { 0: splat } = useParams<{ 0: string }>();
    const tanagraUrl = serverConfigStore.get().config.tanagraBaseUrl;
    const { bigqueryDataset } = findCdrVersion(
      cdrVersionId,
      cdrVersionTiersResponse
    );

    useExitActionListener(() => {
      // Navigate to Data tab when exiting Tanagra iframe
      navigate(['workspaces', namespace, id, 'data']);
    });

    return (
      <div
        style={{
          height: '95vh',
        }}
      >
        <iframe
          onLoad={() => hideSpinner()}
          style={{
            border: 0,
            height: '100%',
            width: '100%',
          }}
          src={`${tanagraUrl}/ui#/tanagra/underlays/aou${bigqueryDataset}/studies/${namespace}/${splat}/?token=${getAccessToken()}`}
        />
      </div>
    );
  }
);
