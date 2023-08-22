import * as React from 'react';

import { render, RenderResult, screen } from '@testing-library/react';

import { TwoFactorAuthModal } from './two-factor-auth-modal';

describe('TwoFactorAuthModal', () => {
  const component = (): RenderResult =>
    render(
      <div id='popup-root'>
        <TwoFactorAuthModal onCancel={() => {}} onClick={() => {}} />
      </div>
    );

  it('should render', () => {
    component();
    expect(
      screen.queryByText('Redirecting to turn on Google 2-step Verification')
    ).toBeTruthy();
  });
});
