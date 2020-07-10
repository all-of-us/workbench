import {shallow} from 'enzyme';
import * as React from 'react';

import {CohortSearch} from './cohort-search.component';

describe('CBModal', () => {
  it('should render', () => {
    const wrapper = shallow(<CohortSearch/>);
    expect(wrapper).toBeTruthy();
  });
});
