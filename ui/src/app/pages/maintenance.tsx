import * as React from 'react';

import { FlexColumn, FlexSpacer } from 'app/components/flex';
import { PageHeader } from 'app/components/headers';
import logo from 'assets/images/all-of-us-logo.svg';

export const Maintenance = () => {
  return (
    <FlexColumn style={{ height: '100vh' }}>
      <FlexSpacer />
      <FlexColumn style={{ alignItems: 'center' }}>
        <img
          style={{ height: '15vh', marginBottom: '1rem' }}
          src={logo}
          alt='all of us logo'
        />
        <PageHeader>Down for System Maintenance</PageHeader>
      </FlexColumn>
      <FlexSpacer />
    </FlexColumn>
  );
};
