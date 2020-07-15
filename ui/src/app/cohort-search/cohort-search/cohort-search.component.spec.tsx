import {shallow} from 'enzyme';
import * as React from 'react';

import {DomainType} from 'generated/fetch';
import {CohortSearch} from './cohort-search.component';

const searchContextStub = {
  domain: DomainType.CONDITION,
  item: {
    searchParameters: []
  }
};

describe('CohortSearch', () => {
  it('should render', () => {
    const wrapper = shallow(<CohortSearch closeSearch={() => {}} searchContext={searchContextStub}/>);
    expect(wrapper).toBeTruthy();
  });
});