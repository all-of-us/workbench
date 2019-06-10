import {shallow} from 'enzyme';

import {Map} from 'immutable';
import * as React from 'react';
import {GenderChart} from './gender-chart.component';


describe('GenderChart', () => {
  it('should render', () => {
    const wrapper = shallow(<GenderChart data={Map()}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
