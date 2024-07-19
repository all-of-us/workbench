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
      The <AoU /> Researcher Workbench is currently experiencing an outage. Our
      team continues to work diligently to resolve this issue as quickly as
      possible, and we apologize for any inconveniences this may cause. We will
      provide updates via the
      <a href='https://support.researchallofus.org/hc/en-us' target='blank'>
        {' '}
        User Support Hub homepage
      </a>{' '}
      when available.
    </ErrorMessage>
  </div>
);
