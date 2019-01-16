import {mount} from 'enzyme';
import * as React from 'react';

import LoginReactComponent from './component';

describe('LoginComponent', () => {
  let props: any;

  const signIn = jest.fn();
  const onCreateAccount = jest.fn();

  const component = () => {
    return mount<LoginReactComponent>
    (<LoginReactComponent {...props}/>);
  };

  beforeEach(() => {
    props = {
      signIn: signIn,
      onCreateAccount: onCreateAccount
    };
    signIn.mockClear();
    onCreateAccount.mockClear();
  });

  it('should signIn on clicking google icon', () => {
    const wrapper = component();
    const signInButton = wrapper.find({type: 'primary'});
    signInButton.simulate('click');
    expect(signIn).toHaveBeenCalled();
    expect(onCreateAccount).not.toHaveBeenCalled();
  });

  it('should call props onCreateAccount function on clicking Create account button', () => {
    const wrapper = component();
    const createAccountButton = wrapper.find({type: 'secondary'});
    createAccountButton.simulate('click');
    expect(signIn).not.toHaveBeenCalled();
    expect(onCreateAccount).toHaveBeenCalled();
  });
});
