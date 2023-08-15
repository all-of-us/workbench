import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import {
  CohortReviewApi,
  CohortsApi,
  ConceptSetsApi,
  DataSetApi,
  WorkspacesApi,
} from 'generated/fetch';

import { resourceTableRows } from 'app/components/resource-list.spec';
import { DataComponent } from 'app/pages/data/data-component';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from 'app/utils/constants';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
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

describe('DataPage', () => {
  beforeEach(() => {
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
    return mount(
      <MemoryRouter>
        <DataComponent hideSpinner={() => {}} showSpinner={() => {}} />
      </MemoryRouter>
    );
  };

  it('should render', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show all datasets, cohorts, and concept sets', async () => {
    const wrapper = component();
    const resourceTableRowsExpected =
      ConceptSetsApiStub.stubConceptSets().length +
      exampleCohortStubs.length +
      cohortReviewStubs.length +
      DataSetApiStub.stubDataSets().length;
    await waitOneTickAndUpdate(wrapper);

    expect(resourceTableRowsExpected).toBeGreaterThan(
      ROWS_PER_PAGE_RESOURCE_TABLE
    );
    // Since we have pagination only 10 rows at a time should be displayed
    expect(resourceTableRows(wrapper).length).toBe(
      ROWS_PER_PAGE_RESOURCE_TABLE
    );

    // Click the next button to confirm the number of rows
    const paginationNextButton = wrapper
      .find('button')
      .findWhere(
        (element) =>
          element.prop('className') ===
          'p-paginator-next p-paginator-element p-link'
      );
    paginationNextButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(resourceTableRows(wrapper).length).toBe(
      resourceTableRowsExpected - ROWS_PER_PAGE_RESOURCE_TABLE
    );
  });

  it('should show only cohorts when selected', async () => {
    const wrapper = component();
    const resourceTableRowsExpected = exampleCohortStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="view-only-cohorts"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(resourceTableRows(wrapper).length).toBe(resourceTableRowsExpected);
  });

  it('should show only cohort reviews when selected', async () => {
    const wrapper = component();
    const resourceTableRowsExpected = cohortReviewStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="view-only-cohort-reviews"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(resourceTableRows(wrapper).length).toBe(resourceTableRowsExpected);
  });

  it('should show only conceptSets when selected', async () => {
    const wrapper = component();
    const resourceTableRowsExpected =
      ConceptSetsApiStub.stubConceptSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="view-only-concept-sets"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(resourceTableRows(wrapper).length).toBe(resourceTableRowsExpected);
  });

  it('should show only dataSets when selected', async () => {
    const wrapper = component();
    const resourceTableRowsExpected = DataSetApiStub.stubDataSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="view-only-data-sets"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(resourceTableRows(wrapper).length).toBe(resourceTableRowsExpected);
  });
});
