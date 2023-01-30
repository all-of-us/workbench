import * as React from 'react';
import { Router } from 'react-router';
import { mount } from 'enzyme';
import { createMemoryHistory } from 'history';

import { CohortBuilderApi, CohortsApi } from 'generated/fetch';

import {
  cohortsApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { CohortsApiStub } from 'testing/stubs/cohorts-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortPage } from './cohort-page';

describe('CohortPage', () => {
  let history;

  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    history = createMemoryHistory();
  });

  const component = () => {
    return mount(
      <Router history={history}>
        <CohortPage
          setCohortChanged={() => {}}
          setShowWarningModal={() => {}}
          setUpdatingCohort={() => {}}
          hideSpinner={() => {}}
          showSpinner={() => {}}
        />
      </Router>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should render one search group for each includes/excludes item', async () => {
    const mockGetCohort = jest.spyOn(cohortsApi(), 'getCohort');
    const { id, namespace } = workspaceDataStub;
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledTimes(0);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(
      0
    );
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(
      0
    );

    // Call cohort with 2 includes groups
    history.push('?cohortId=1');
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 1);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(
      2
    );
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(
      0
    );

    // Call cohort with 2 includes groups and one excludes group
    history.push('?cohortId=2');
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 2);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(
      2
    );
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(
      1
    );
  });
});
