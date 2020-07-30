import {shallow} from 'enzyme';

import * as React from 'react';
import {AttributesPageV2} from './attributes-page-v2.component';


describe('AttributesPageV2', () => {
  it('should render', () => {
    const wrapper = shallow(<AttributesPageV2 node={{}} close={() => {}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
