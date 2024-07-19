import * as React from 'react';

import { ErrorMessage } from 'app/components/messages';

export const LOGIN_ERROR_BANNER = () => (
  <div style={{ fontWeight: 800, paddingLeft: '1rem', paddingRight: '1rem' }}>
    <ErrorMessage>
      We are currently experiencing technical difficulties with our login
      system. Our team is actively working to resolve this issue as quickly as
      possible. We apologize for any inconvenience this may cause and appreciate
      your patience.
    </ErrorMessage>
  </div>
);
