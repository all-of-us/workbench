import * as React from 'react';
import { shallow } from 'enzyme';

import { CohortBuilderApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';

import { ListSearch } from './list-search.component';

beforeEach(() => {
  registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  serverConfigStore.set({
    config: {
      ...defaultServerConfig,
    },
  });
});

describe('ListSearchComponent', () => {
  it('should render', () => {
    const wrapper = shallow(
      <ListSearch hierarchy={() => {}} selections={[]} wizard={{}} />
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
