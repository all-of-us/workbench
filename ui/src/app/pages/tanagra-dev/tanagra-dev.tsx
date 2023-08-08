import * as React from 'react';

import { Button } from 'app/components/buttons';
import { SpinnerOverlay } from 'app/components/spinners';
import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';
import { serverConfigStore } from 'app/utils/stores';

const { useCallback, useEffect, useState } = React;

const tanagraUrl = serverConfigStore.get().config.tanagraBaseUrl;

export function useExitActionListener(callback: () => void) {
  const listener = useCallback(
    (event: MessageEvent) => {
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

export const TanagraDev = withSpinnerOverlay()(({ hideSpinner }) => {
  const [loadingIframe, setLoadingIframe] = useState(false);
  const [showIframe, setShowIframe] = useState(false);
  useEffect(() => {
    hideSpinner();
  }, []);

  useExitActionListener(() => {
    setShowIframe(false);
  });

  // Temporary variable to hardcode existing study ids until we have a link between workspaces and studies
  const studyId =
    process.env.REACT_APP_ENVIRONMENT === 'local' ? 'tqmXfZ4qzu' : 'UslvvbIYxk';

  return showIframe ? (
    <div
      style={{
        height: '95vh',
      }}
    >
      {loadingIframe && <SpinnerOverlay />}
      <iframe
        onLoad={() => setLoadingIframe(false)}
        style={{
          border: 0,
          height: '100%',
          width: '100%',
        }}
        src={`${tanagraUrl}/#/tanagra/underlays/SR2022Q4R6/studies/${studyId}/export`}
      />
    </div>
  ) : (
    <div style={{ padding: '2rem' }}>
      <div style={{ marginBottom: '1rem' }}>
        This is a temporary interface to demonstrate the transition from the
        Workbench to the Tanagra iframe and back. This transition will eventually take
        place on the Data tab page.
      </div>
      <Button
        onClick={() => {
          setLoadingIframe(true);
          setShowIframe(true);
        }}
      >
        Show Tanagra iframe
      </Button>
    </div>
  );
});
