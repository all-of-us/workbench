import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import { VariantSearch } from 'app/pages/data/cohort/variant-search';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortSearchContextStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';

import { renderWithRouter } from 'testing/react-test-helpers';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('VariantSearch', () => {
  beforeEach(() => {
    currentCohortSearchContextStore.next({});
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  });
  const component = () => {
    return renderWithRouter(
      <VariantSearch select={() => {}} selectedIds={[]} />
    );
  };

  it('should render', () => {
    component();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });
});
