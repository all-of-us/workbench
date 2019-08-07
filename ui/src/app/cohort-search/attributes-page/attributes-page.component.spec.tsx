import {shallow} from 'enzyme';

import * as React from 'react';
import {AttributesPage} from './attributes-page.component';


describe('AttributesPage', () => {
  it('should render', () => {
    const wrapper = shallow(<AttributesPage node={{}} close={() => {}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
