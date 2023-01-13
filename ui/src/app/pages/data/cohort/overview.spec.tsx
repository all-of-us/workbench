import * as React from 'react';
import { shallow } from 'enzyme';

import { ListOverview } from './overview';

describe('ListOverview', () => {
  it('should render', () => {
    const wrapper = shallow(<ListOverview searchRequest={{}} update={0} />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
