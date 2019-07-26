import {shallow} from 'enzyme';

import * as React from 'react';
import {ListOverview} from './list-overview.component';


describe('ListOverview', () => {
  it('should render', () => {
    const wrapper = shallow(<ListOverview searchRequest={{}} update={0}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
