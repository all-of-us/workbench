import {mount} from 'enzyme';
import * as React from 'react';

import {CohortResourceCard} from 'app/pages/data/cohort/cohort-resource-card';


describe('CohortResourceCard', () => {
  const component = () => {
    const props = {
      cohort: {}
    };

    return mount(<CohortResourceCard resource={props} onUpdate={() => {}}/>);
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
