import * as React from 'react';

import { RenderResult, screen } from '@testing-library/react';

import { renderModal } from 'testing/react-test-helpers';

import { TwoFactorAuthModal } from './two-factor-auth-modal';

describe('TwoFactorAuthModal', () => {
  const component = (): RenderResult =>
    renderModal(<TwoFactorAuthModal onCancel={() => {}} onClick={() => {}} />);

  it('should render', () => {
    component();
    expect(
      screen.queryByText('Redirecting to turn on Google 2-step Verification')
    ).toBeTruthy();
  });
});
