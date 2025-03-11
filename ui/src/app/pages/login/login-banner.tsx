import * as React from 'react';

import { ErrorMessage } from 'app/components/messages';

interface LoginErrorBannerProps {
  header: string;
  details: string;
  moreInfoLink?: string;
}
export const LoginBanner = ({
  header,
  details,
  moreInfoLink,
}: LoginErrorBannerProps) => (
  <div
    style={{
      fontWeight: 800,
      fontSize: 'large',
      padding: '1rem',
      lineHeight: '2rem',
    }}
  >
    <ErrorMessage>
      {header}: {details}{' '}
      {moreInfoLink && (
        <>
          To learn more, please see the announcement on the User Support Hub{' '}
          <a href={moreInfoLink} target='_blank'>
            {' '}
            here
          </a>
          .
        </>
      )}
    </ErrorMessage>
  </div>
);
