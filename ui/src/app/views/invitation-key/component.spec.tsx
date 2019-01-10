import {mount} from 'enzyme';
import * as React from 'react';

import InvitationKeyReact from './component';


describe('InvitationKeyComponent', () => {
  let props: any;

  const component = () => {
    return mount<InvitationKeyReact>
    (<InvitationKeyReact {...props}/>);
  };

  beforeEach(() => {
    props = {};
  });

  it('should render', () => {
    const wrapper = component();
    console.log(wrapper.html());
  });
});
