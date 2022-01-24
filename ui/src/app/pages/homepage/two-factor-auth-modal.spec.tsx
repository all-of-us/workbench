import * as React from 'react';
import { mount } from 'enzyme';

import { TwoFactorAuthModal } from './two-factor-auth-modal';

describe('TwoFactorAuthModal', () => {
  const component = () =>
    mount(<TwoFactorAuthModal onCancel={() => {}} onClick={() => {}} />);

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
