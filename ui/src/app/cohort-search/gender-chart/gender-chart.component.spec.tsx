import {shallow} from 'enzyme';

import * as React from 'react';
import {GenderChart} from './gender-chart.component';


describe('GenderChart', () => {
  it('should render', () => {
    const wrapper = shallow(<GenderChart data={{}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
