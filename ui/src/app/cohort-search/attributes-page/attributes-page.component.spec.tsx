import {shallow} from 'enzyme';

import * as React from 'react';
import {AttributesPage} from './attributes-page.component';


describe('AttributesPageV2', () => {
  it('should render', () => {
    const wrapper = shallow(<AttributesPage node={{}} close={() => {}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
