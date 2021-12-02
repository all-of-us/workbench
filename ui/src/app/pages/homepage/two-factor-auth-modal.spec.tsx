import {mount} from 'enzyme';
import * as React from 'react';

import {TwoFactorAuthModal} from './two-factor-auth-modal';

describe('TwoFactorAuthModal', () => {

  const component = () => mount(<TwoFactorAuthModal onCancel={() => {}} onClick={() => {}}/>);

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
