import * as React from 'react';
import * as fp from 'lodash/fp';

import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { useNavigation } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

const { useCallback, useEffect } = React;

export function useExitActionListener(callback: () => void) {
  const listener = useCallback(
    (event: MessageEvent) => {
      const tanagraUrl = serverConfigStore.get().config.tanagraBaseUrl;
      if (
        event.origin !== tanagraUrl ||
        typeof event.data !== 'object' ||
        event.data.message !== 'CLOSE'
      ) {
        return;
      }
      callback();
    },
    [callback]
  );

  useEffect(() => {
    window.addEventListener('message', listener);
    return () => {
      window.removeEventListener('message', listener);
    };
  }, [listener]);
}

export const TanagraDev = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withSpinnerOverlay()
)(
  ({
    cdrVersionTiersResponse,
    hideSpinner,
    workspace: { cdrVersionId, id, namespace },
  }) => {
    const [navigate] = useNavigation();

    useExitActionListener(() => {
      // Navigate to Data tab when exiting Tanagra iframe
      navigate(['workspaces', namespace, id, 'data']);
    });

    const { bigqueryDataset } = findCdrVersion(
      cdrVersionId,
      cdrVersionTiersResponse
    );
    const tanagraUrl = serverConfigStore.get().config.tanagraBaseUrl;
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
          src={`${tanagraUrl}/ui#/tanagra/underlays/aou${bigqueryDataset}/studies/${namespace}/export`}
        />
      </div>
    );
  }
);
