import * as React from 'react';

import { ErrorMessage } from 'app/components/messages';
import { AoU } from 'app/components/text-wrappers';

interface LoginErrorBannerProps {
  details: string;
}
export const LOGIN_ERROR_BANNER = ({ details }: LoginErrorBannerProps) => (
  <div
    style={{
      fontWeight: 800,
      fontSize: 'large',
      padding: '1rem',
      lineHeight: '2rem',
    }}
  >
    <ErrorMessage>
      Scheduled Downtime Notice for the <AoU /> Researcher Workbench: {details}{' '}
      To learn more, please see the announcement on the User Support Hub{' '}
      <a
        href='https://support.researchallofus.org/hc/en-us/articles/35123782567700'
        target='blank'
      >
        {' '}
        here
      </a>
      .
    </ErrorMessage>
  </div>
);
