import { shallow } from 'enzyme';
import * as React from 'react';

import LoginReactComponent from './component';

describe('LoginComponent', () => {
  let props: any;

  const component = () => {
    return shallow<LoginReactComponent>
    (<LoginReactComponent {...props}/>);
  };

  beforeEach(() => {
    props = {};
  });

  it('should render', () => {
   const loginComponent = component();
    expect(loginComponent.children('LoginButton')).toBeTruthy();
    expect(loginComponent.children('GoogleIcon')).toBeTruthy();
    expect(loginComponent.children('SecondaryLoginbutton')).toBeTruthy();
  });
});
