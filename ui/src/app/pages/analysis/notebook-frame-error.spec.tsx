import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';

import { SecuritySuspendedMessage } from './notebook-frame-error';

describe('SecuritySuspendedMessage', () => {
  const now = new Date('2000-01-01 03:00:00');
  const nowMinus10Minutes = new Date('2000-01-01 02:55:00');
  const nowPlusFiveMinutes = new Date('2000-01-01 03:05:00');

  const component = (suspendedUntil: Date) => {
    return render(
      <SecuritySuspendedMessage
        error={
          new ComputeSecuritySuspendedError({
            suspendedUntil: suspendedUntil.toISOString(),
          })
        }
      />
    );
  };

  beforeEach(() => {
    jest.useFakeTimers().setSystemTime(now.getTime());
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should show pending suspension', async () => {
    component(nowPlusFiveMinutes);
    expect(
      screen.getByText(
        /Your analysis environment is suspended due to security egress concerns/i
      )
    ).toBeInTheDocument();
    expect(screen.getByText(/in 5 minutes/i)).toBeInTheDocument();
  });

  it('should indicate reload for past suspension', async () => {
    component(nowMinus10Minutes);
    expect(
      screen.getByText(
        /Your analysis environment was temporarily suspended but is now available for use. Reload the page to continue./i
      )
    ).toBeInTheDocument();
  });

  it('should update estimate over time', async () => {
    component(nowPlusFiveMinutes);
    expect(screen.getByText(/in 5 minutes/i)).toBeInTheDocument();
    jest.advanceTimersByTime(5 * 60 * 1000); // advance time by 5 minutes
    await screen.findByText(/Reload the page to continue./i);
  });
});
