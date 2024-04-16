import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';

import { ListSearch } from './list-search';

beforeEach(() => {
  registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
});

describe('ListSearchComponent', () => {
  const searchTerms = 'selected search term';
  it('should render', () => {
    render(
      <ListSearch
        hierarchy={() => {}}
        searchContext={{ domain: '' }}
        searchTerms={searchTerms}
        selections={[]}
        wizard={{}}
        workspace={{}}
      />
    );
    expect(screen.getByDisplayValue(searchTerms)).toBeTruthy();
  });
});
