import {mount} from 'enzyme';
import * as React from 'react';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let props: {signIn: Function, onCreateAccount: Function};

  const component = (props) => mount(<LoginReactComponent {...props}/>);

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
