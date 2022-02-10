import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let loginProps: { signIn: Function; onCreateAccount: Function };

  const component = (props) =>
    mount(
      <MemoryRouter>
        <LoginReactComponent {...props} />
      </MemoryRouter>
    );

  beforeEach(() => {
    loginProps = {
      signIn: () => {},
      onCreateAccount: () => {},
    };
  });

  it('should render', () => {
    const wrapper = component(loginProps);
    expect(wrapper).toBeTruthy();
  });
});
