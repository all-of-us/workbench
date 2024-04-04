import * as React from 'react';

import { CohortBuilderApi, Criteria } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';

import { SearchBar } from './search-bar';

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
  const searchTerms = 'selected search term';
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  });
  it('should render', () => {
    render(
      <SearchBar
        node={nodeStub}
        searchTerms={searchTerms}
        selectedSurvey={''}
        setIngredients={() => {}}
        selectOption={() => {}}
        setInput={() => {}}
      />
    );
    expect(screen.getByDisplayValue(searchTerms)).toBeTruthy();
  });
});
