import {mount} from 'enzyme';
import * as React from 'react';

import {TwoFactorAuthModal} from "./two-factor-auth-modal";

describe('TwoFactorAuthModal', () => {

  const component = () => mount(<TwoFactorAuthModal/>);

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
