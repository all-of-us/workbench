import {mount} from 'enzyme';
import * as React from 'react';

import {currentCohortSearchContextStore} from 'app/utils/navigation';
import {Domain} from 'generated/fetch';
import {CohortSearch} from './cohort-search.component';

const searchContextStubs = [
  {
    domain: Domain.CONDITION,
    item: {
      searchParameters: []
    }
  },
  {
    domain: Domain.PERSON,
    item: {
      searchParameters: []
    }
  }
];

describe('CohortSearch', () => {
  it('should render', () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    const wrapper = mount(<CohortSearch/>);
    expect(wrapper).toBeTruthy();
  });

  it('should render CriteriaSearch component for any domain except Person', () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    const wrapper = mount(<CohortSearch/>);
    expect(wrapper.find('[id="criteria-search-container"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="demographics"]').length).toBe(0);
  });

  it('should render Demographics component for Person domain', () => {
    currentCohortSearchContextStore.next(searchContextStubs[1]);
    const wrapper = mount(<CohortSearch/>);
    expect(wrapper.find('[id="criteria-search-container"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="demographics"]').length).toBe(1);
  });
});
