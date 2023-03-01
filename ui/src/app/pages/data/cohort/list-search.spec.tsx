import * as React from 'react';
import { shallow } from 'enzyme';

import { CohortBuilderApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';

import { ListSearch } from './list-search';

beforeEach(() => {
  registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
});

describe('ListSearchComponent', () => {
  it('should render', () => {
    const wrapper = shallow(
      <ListSearch hierarchy={() => {}} selections={[]} wizard={{}} />
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
