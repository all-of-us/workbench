import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { CohortResourceCard } from 'app/pages/data/cohort/cohort-resource-card';

describe('CohortResourceCard', () => {
  const component = () => {
    const props = {
      cohort: {},
    };

    return mount(
      <MemoryRouter>
        <CohortResourceCard resource={props} onUpdate={() => {}} />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
