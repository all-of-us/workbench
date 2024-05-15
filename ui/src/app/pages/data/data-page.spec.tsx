import * as React from 'react';

import {
  CohortReviewApi,
  CohortsApi,
  ConceptSetsApi,
  DataSetApi,
  WorkspacesApi,
} from 'generated/fetch';

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DataComponent } from 'app/pages/data/data-component';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from 'app/utils/constants';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import {
  expectSpinner,
  renderWithRouter,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import {
  CohortReviewServiceStub,
  cohortReviewStubs,
} from 'testing/stubs/cohort-review-service-stub';
import {
  CohortsApiStub,
  exampleCohortStubs,
} from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

export const resourceTableRows = () =>
  within(screen.getAllByRole('rowgroup')[1]).getAllByRole('row');

describe('DataPage', () => {
  let user;
  beforeEach(() => {
    user = userEvent.setup();
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  const component = () => {
    return renderWithRouter(
      <DataComponent hideSpinner={() => {}} showSpinner={() => {}} />
    );
  };

  it('should render', async () => {
    component();
    expectSpinner();
  });

  it('should show all datasets, cohorts, and concept sets', async () => {
    component();
    const resourceTableRowsExpected =
      ConceptSetsApiStub.stubConceptSets().length +
      exampleCohortStubs.length +
      cohortReviewStubs.length +
      DataSetApiStub.stubDataSets().length;

    expect(resourceTableRowsExpected).toBeGreaterThan(
      ROWS_PER_PAGE_RESOURCE_TABLE
    );

    await waitForNoSpinner();

    // Since we have pagination only 10 rows at a time should be displayed
    expect(resourceTableRows().length).toBe(ROWS_PER_PAGE_RESOURCE_TABLE);

    // Click the next button to confirm the number of rows
    const paginationNextButton = screen.getByRole('button', {
      name: /next page/i,
    });

    await user.click(paginationNextButton);
    expect(resourceTableRows().length).toBe(
      resourceTableRowsExpected - ROWS_PER_PAGE_RESOURCE_TABLE
    );
  });

  it('should show only cohorts when selected', async () => {
    component();
    const resourceTableRowsExpected = exampleCohortStubs.length;

    const cohortsFilterButton = await screen.findByRole('button', {
      name: 'Cohorts',
    });
    await user.click(cohortsFilterButton);
    expect(resourceTableRows().length).toBe(resourceTableRowsExpected);
  });

  it('should show only cohort reviews when selected', async () => {
    component();
    const resourceTableRowsExpected = cohortReviewStubs.length;
    const cohortReviewsFilterButton = await screen.findByRole('button', {
      name: 'Cohort Reviews',
    });
    await user.click(cohortReviewsFilterButton);
    expect(resourceTableRows().length).toBe(resourceTableRowsExpected);
  });

  it('should show only conceptSets when selected', async () => {
    component();
    const resourceTableRowsExpected =
      ConceptSetsApiStub.stubConceptSets().length;
    const conceptSetsFilterButton = await screen.findByRole('button', {
      name: 'Concept Sets',
    });
    await user.click(conceptSetsFilterButton);
    expect(resourceTableRows().length).toBe(resourceTableRowsExpected);
  });

  it('should show only dataSets when selected', async () => {
    component();
    const resourceTableRowsExpected = DataSetApiStub.stubDataSets().length;
    const datatsetsFilterButton = await screen.findByRole('button', {
      name: 'Datasets',
    });
    await user.click(datatsetsFilterButton);
    expect(resourceTableRows().length).toBe(resourceTableRowsExpected);
  });
});
