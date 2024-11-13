import '@testing-library/jest-dom';

import * as React from 'react';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InitialCreditsPanel } from './initial-credits-panel';

const setup = (
  freeTierUsage: number,
  freeTierDollarQuota: number,
  expirationDate: number
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
  setup(freeTierUsage, freeTierDollarQuota, expirationDate);
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  // This should reflect the quota minus the usage
  expect(screen.getByText(`$134.00`)).toBeInTheDocument();
  expect(
    screen.getByText(`initial credits epiration date:`)
  ).toBeInTheDocument();
  expect(screen.getByText(`Dec 3, 2023`)).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: /request credit extension/i })
  ).toBeInTheDocument();
});

it('should display initial credits for a user without an expiration record', async () => {
  const freeTierUsage = 100.0;
  const freeTierDollarQuota = 234;
  const expirationDate = null;
  setup(freeTierUsage, freeTierDollarQuota, expirationDate);
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  expect(
    screen.queryByText(`initial credits epiration date:`)
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /request credit extension/i })
  ).not.toBeInTheDocument();
});
