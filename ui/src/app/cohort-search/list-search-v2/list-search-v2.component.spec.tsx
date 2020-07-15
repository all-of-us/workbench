import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {shallow} from 'enzyme';

import {CohortBuilderApi} from 'generated/fetch';
import * as React from 'react';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ListSearchV2} from './list-search-v2.component';

beforeEach(() => {
  registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
});

describe('ListSearchComponent', () => {
  it('should render', () => {
    const wrapper = shallow(<ListSearchV2
      hierarchy={() => {}}
      selections={[]}
      wizard={{}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
})
