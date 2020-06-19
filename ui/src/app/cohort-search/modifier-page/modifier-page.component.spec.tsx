import {shallow} from 'enzyme';

import * as React from 'react';
import {ModifierPage} from './modifier-page.component';


describe('ListModifierPage', () => {
  it('should render', () => {
    const wrapper = shallow(<ModifierPage disabled={() => {}} wizard={{}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
