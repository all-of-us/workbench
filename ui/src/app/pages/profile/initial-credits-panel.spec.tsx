import '@testing-library/jest-dom';

import * as React from 'react';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InitialCreditsPanel } from './initial-credits-panel';

const setup = (
  initialCreditsUsage: number,
  initialCreditsLimit: number,
  expirationDate: number
) => {
  return {
    container: render(
      <InitialCreditsPanel
        {...{
          initialCreditsUsage,
          initialCreditsLimit,
          expirationDate,
        }}
      />
    ).container,
    user: userEvent.setup(),
  };
};

it('should display initial credits for a user with an expiration record', async () => {
  const initialCreditsUsage = 100.0;
  const initialCreditsLimit = 234;
  const expirationDate = new Date('2023-12-03T20:00:00Z').getTime();
  setup(initialCreditsUsage, initialCreditsLimit, expirationDate);
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  // This should reflect the quota minus the usage
  expect(screen.getByText(`$134.00`)).toBeInTheDocument();
  expect(
    screen.getByText(`initial credits expiration date:`)
  ).toBeInTheDocument();
  expect(screen.getByText(`Dec 3, 2023`)).toBeInTheDocument();
});

it('should display initial credits for a user without an expiration record', async () => {
  const initialCreditsUsage = 100.0;
  const initialCreditsLimit = 234;
  const expirationDate = null;
  setup(initialCreditsUsage, initialCreditsLimit, expirationDate);
  expect(screen.getByText(`$100.00`)).toBeInTheDocument();
  expect(
    screen.queryByText(`initial credits expiration date:`)
  ).not.toBeInTheDocument();
});
