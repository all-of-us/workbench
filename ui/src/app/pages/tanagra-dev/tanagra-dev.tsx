import * as React from 'react';

import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';

const { useEffect } = React;

export const TanagraDev = withSpinnerOverlay()(({ hideSpinner }) => {
  useEffect(() => {
    hideSpinner();
  }, []);

  return (
    <div
      style={{
        height: '25rem',
        margin: '2rem auto',
        width: '98%',
      }}
    >
      <iframe
        style={{
          border: 0,
          height: '100%',
          width: '100%',
        }}
        src='http://localhost:3000/#/'
      />
    </div>
  );
});
