import * as React from 'react';
import { shallow } from 'enzyme';

import { CohortBuilderApi, Criteria } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';

import { SearchBar } from './search-bar.component';

const nodeStub = {
  code: '',
  conceptId: 903133,
  count: 0,
  domainId: 'Measurement',
  group: false,
  hasAttributes: true,
  id: 316305,
  name: 'Height Detail',
  parentId: 0,
  predefinedAttributes: null,
  selectable: true,
  subtype: 'HEIGHT',
  type: 'PM',
} as Criteria;
describe('SearchBar', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
      },
    });
  });
  it('should render', () => {
    const wrapper = shallow(
      <SearchBar
        node={nodeStub}
        searchTerms={''}
        selectedSurvey={''}
        setIngredients={() => {}}
        selectOption={() => {}}
        setInput={() => {}}
      />
    );
    expect(wrapper).toBeTruthy();
  });
});
