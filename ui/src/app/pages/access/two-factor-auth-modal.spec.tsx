import * as React from 'react';

import { render, RenderResult } from '@testing-library/react';

import { TwoFactorAuthModal } from './two-factor-auth-modal';

describe('TwoFactorAuthModal', () => {
  const component = (): RenderResult =>
    render(
      <div id='popup-root'>
        <TwoFactorAuthModal onCancel={() => {}} onClick={() => {}} />
      </div>
    );

  it('should render', () => {
    const { container } = component();
    expect(container).toBeTruthy();
  });
});
