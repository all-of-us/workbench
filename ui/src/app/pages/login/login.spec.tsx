import {mount} from 'enzyme';
import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let props: {signIn: Function, onCreateAccount: Function};

  const component = (props) => mount(<MemoryRouter><LoginReactComponent {...props}/></MemoryRouter>);

  beforeEach(() => {
    props = {
      signIn: () => {},
      onCreateAccount: () => {}
    };
  });

  it('should render', () => {
    const wrapper = component(props);
    expect(wrapper).toBeTruthy();
  });
});
