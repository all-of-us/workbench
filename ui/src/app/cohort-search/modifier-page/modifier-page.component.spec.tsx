import {shallow} from 'enzyme';

import * as React from 'react';
import {ListModifierPage} from './modifier-page.component';


describe('ListModifierPage', () => {
  it('should render', () => {
    const wrapper = shallow(<ListModifierPage disabled={() => {}} wizard={{}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
