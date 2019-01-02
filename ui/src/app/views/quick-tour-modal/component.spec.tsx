import {QuickTourReact} from './component';

import * as React from 'react';
import { shallow } from 'enzyme';


describe('QuickTourModalComponent', () => {

  it('should render', () => {
    const component = shallow(<QuickTourReact learning={true} closeFunction={ () => {} }/>);
    expect(component).toMatchSnapshot();
  });

});

