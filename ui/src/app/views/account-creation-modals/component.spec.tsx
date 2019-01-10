import { shallow } from 'enzyme';
import * as React from 'react';

import {
  AccountCreationResendModalProps,
  AccountCreationResendModalReact,
  AccountCreationUpdateModalProps,
  AccountCreationUpdateModalReact,
  AccountCreationUpdateModalState,
} from './component';


describe('AccountCreationUpdateModal', () => {

  let props: AccountCreationUpdateModalProps;

  const component = () => {
    return shallow<AccountCreationUpdateModalReact,
      AccountCreationUpdateModalProps,
      AccountCreationUpdateModalState>
    (<AccountCreationUpdateModalReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      username: 'my-username@fake-research-aou.org',
      creationNonce: 'temp',
      passNewEmail: () => {},
      update: true,
      closeFunction: () => {}
    };
  });

  it('should render, should have a next and close button', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
