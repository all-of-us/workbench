import '@testing-library/jest-dom';

import * as React from 'react';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { serverConfigStore } from 'app/utils/stores';

import { InitialCreditsPanel } from './initial-credits-panel';

const setup = (
  freeTierUsage: number,
  freeTierDollarQuota: number,
  expirationDate: number,
  eligibleForExtension: boolean
) => {
  const updateInitialCredits = jest.fn();
  return {
    container: render(
      <InitialCreditsPanel
        {...{
          freeTierUsage,
          freeTierDollarQuota,
          expirationDate,
          updateInitialCredits,
          eligibleForExtension,
        }}
      />
    ).container,
    user: userEvent.setup(),
  };
};

it('should display initial credits for a user with an expiration record', async () => {
  const freeTierUsage = 100.0;
  const freeTierDollarQuota = 234;
  const expirationDate = new Date('2023-12-03T20:00:00Z').getTime();
  const eligibleForExtension = true;
  setup(
    freeTierUsage,
    freeTierDollarQuota,
    expirationDate,
    eligibleForExtension
  );
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  // This should reflect the quota minus the usage
  expect(screen.getByText(`$134.00`)).toBeInTheDocument();
  expect(
    screen.getByText(`initial credits expiration date:`)
  ).toBeInTheDocument();
  expect(screen.getByText(`Dec 3, 2023`)).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: /request credit extension/i })
  ).toBeInTheDocument();
});

it('should show extension modal when extension button is clicked', async () => {
  const freeTierUsage = 100.0;
  const freeTierDollarQuota = 234;
  const expirationDate = new Date('2023-12-03T20:00:00Z').getTime();
  const eligibleForExtension = true;
  serverConfigStore.set({
    config: {
      gsuiteDomain: 'fake-research-aou.org',
      initialCreditsValidityPeriodDays: 100,
      initialCreditsExpirationWarningDays: 5,
    },
  });
  const { user } = setup(
    freeTierUsage,
    freeTierDollarQuota,
    expirationDate,
    eligibleForExtension
  );
  await user.click(
    screen.getByRole('button', { name: /request credit extension/i })
  );
  screen.getByText(/request credit expiration date extension/i);
});

it('should not show extension button if the user is not eligible', async () => {
  const freeTierUsage = 100.0;
  const freeTierDollarQuota = 234;
  const expirationDate = new Date('2023-12-03T20:00:00Z').getTime();
  const eligibleForExtension = false;
  setup(
    freeTierUsage,
    freeTierDollarQuota,
    expirationDate,
    eligibleForExtension
  );
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /request credit extension/i })
  ).not.toBeInTheDocument();
});

it('should display initial credits for a user without an expiration record', async () => {
  const freeTierUsage = 100.0;
  const freeTierDollarQuota = 234;
  const expirationDate = null;
  const eligibleForExtension = false;
  setup(
    freeTierUsage,
    freeTierDollarQuota,
    expirationDate,
    eligibleForExtension
  );
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  expect(
    screen.queryByText(`initial credits expiration date:`)
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /request credit extension/i })
  ).not.toBeInTheDocument();
});
