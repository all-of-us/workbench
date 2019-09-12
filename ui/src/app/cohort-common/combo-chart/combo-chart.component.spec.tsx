import {shallow} from 'enzyme';

import * as React from 'react';
import {ComboChart} from './combo-chart.component';


describe('GenderChart', () => {
  it('should render', () => {
    const wrapper = shallow(<ComboChart data={[]} mode='percent'/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
