import {shallow} from 'enzyme';
import * as React from 'react';

import {AccountCreationProps, AccountCreationReact, AccountCreationState} from './component';

let props: AccountCreationProps;
const component = () => {
  return shallow<AccountCreationReact,
    AccountCreationProps,
    AccountCreationState>(<AccountCreationReact {...props}/>);
};

beforeEach(() => {
  props = {
    invitationKey: 'd4cd81',
    setProfile: () => {},
    updateNext: () => {},
  };
});

it('should handle username invalidity', () => {
  const wrapper = component();
  wrapper.find('#username').simulate('focus');
});
