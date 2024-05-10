import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { findByText, render, screen, waitFor } from '@testing-library/react';
import {
  DataAccessPanel,
  DataAccessPanelProps,
} from 'app/pages/profile/data-access-panel';
import { cdrVersionStore } from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';

const queryRTGranted = (wrapper) =>
  screen.queryByTestId('registered-tier-access-granted');
const queryRTDenied = (wrapper) =>
  screen.queryByTestId('registered-tier-access-denied');
const queryCTGranted = (wrapper) =>
  screen.queryByTestId('controlled-tier-access-granted');
const queryCTDenied = (wrapper) =>
  screen.queryByTestId('controlled-tier-access-denied');

const expectAccessStatus = (wrapper, rtStatus: boolean, ctStatus: boolean) => {
  expect(!!queryRTGranted(wrapper)).toEqual(rtStatus);
  expect(!!queryRTDenied(wrapper)).not.toEqual(rtStatus);
  expect(!!queryCTGranted(wrapper)).toEqual(ctStatus);
  expect(!!queryCTDenied(wrapper)).not.toEqual(ctStatus);
};

describe('Data Access Panel', () => {
  const component = (props: DataAccessPanelProps) => {
    return renderWithRouter(<DataAccessPanel {...props} />);
  };

  beforeEach(() => {
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('Should show success status for registered tier when the user has access', async () => {
    const wrapper = component({ userAccessTiers: ['registered'] });
    expectAccessStatus(wrapper, true, false);
  });

  it('Should show success status for controlled tier when the user has access', async () => {
    const wrapper = component({ userAccessTiers: ['controlled'] });
    expectAccessStatus(wrapper, false, true);
  });

  it('Should show success status when the user is in the registered tier and controlled tier', async () => {
    const wrapper = component({
      userAccessTiers: ['registered', 'controlled'],
    });
    expectAccessStatus(wrapper, true, true);
  });

  it('Should not show success status when the user is not in the registered tier or controlled tier', async () => {
    const wrapper = component({ userAccessTiers: [] });
    expectAccessStatus(wrapper, false, false);
  });
});
