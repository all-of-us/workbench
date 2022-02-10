import * as React from 'react';
import { useEffect } from 'react';

import { BoldHeader } from 'app/components/headers';
import { PublicLayout } from 'app/components/public-layout';
import { SupportMailto } from 'app/components/support';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';

export const UserDisabled = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return (
    <PublicLayout>
      <BoldHeader>Your account has been disabled</BoldHeader>
      <section
        style={{ color: colors.primary, fontSize: '18px', marginTop: '.5rem' }}
      >
        Contact <SupportMailto /> for more information.
      </section>
    </PublicLayout>
  );
};
