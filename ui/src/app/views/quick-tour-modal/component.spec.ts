import {QuickTourModalComponent, QuickTourReact} from './component';

import * as React from 'react';
import {mount, shallow} from 'enzyme';

describe('QuickTourModalComponent', () => {

  it('should render', () => {
    const component = shallow(<QuickTourReact />);
    expect(component).toMatchSnapshot();
  });

});
