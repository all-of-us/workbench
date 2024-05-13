import * as React from 'react';

import { screen } from '@testing-library/react';
import {
  DataAccessPanel,
  DataAccessPanelProps,
} from 'app/pages/profile/data-access-panel';
import { cdrVersionStore } from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';

const queryRTGranted = () =>
  screen.queryByTestId('registered-tier-access-granted');
const queryRTDenied = () =>
  screen.queryByTestId('registered-tier-access-denied');
const queryCTGranted = () =>
  screen.queryByTestId('controlled-tier-access-granted');
const queryCTDenied = () =>
  screen.queryByTestId('controlled-tier-access-denied');

const expectAccessStatus = (rtStatus: boolean, ctStatus: boolean) => {
  expect(!!queryRTGranted()).toEqual(rtStatus);
  expect(!!queryRTDenied()).not.toEqual(rtStatus);
  expect(!!queryCTGranted()).toEqual(ctStatus);
  expect(!!queryCTDenied()).not.toEqual(ctStatus);
};

describe('Data Access Panel', () => {
  const component = (props: DataAccessPanelProps) => {
    return renderWithRouter(<DataAccessPanel {...props} />);
  };

  beforeEach(() => {
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('Should show success status for registered tier when the user has access', async () => {
    component({ userAccessTiers: ['registered'] });
    expectAccessStatus(true, false);
  });

  it('Should show success status for controlled tier when the user has access', async () => {
    component({ userAccessTiers: ['controlled'] });
    expectAccessStatus(false, true);
  });

  it('Should show success status when the user is in the registered tier and controlled tier', async () => {
    component({
      userAccessTiers: ['registered', 'controlled'],
    });
    expectAccessStatus(true, true);
  });

  it('Should not show success status when the user is not in the registered tier or controlled tier', async () => {
    component({ userAccessTiers: [] });
    expectAccessStatus(false, false);
  });
});
