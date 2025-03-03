import * as React from 'react';

import { ErrorMessage } from 'app/components/messages';
import { AoU } from 'app/components/text-wrappers';

export const LOGIN_ERROR_BANNER = () => (
  <div
    style={{
      fontWeight: 800,
      fontSize: 'large',
      padding: '1rem',
      lineHeight: '2rem',
    }}
  >
    <ErrorMessage>
      Scheduled Downtime Notice for the <AoU /> Researcher Workbench: The
      Researcher Workbench will be unavailable for scheduled maintenance on
      Wednesday, March 5, 2025 and Friday, March 7, 2025 from 8:00 to 9:00 a.m.
      ET. To learn more, please see the announcement on the User Support Hub{' '}
      <a href='https://support.researchallofus.org/hc/en-us' target='blank'>
        {' '}
        here
      </a>{' '}
      .
    </ErrorMessage>
  </div>
);
