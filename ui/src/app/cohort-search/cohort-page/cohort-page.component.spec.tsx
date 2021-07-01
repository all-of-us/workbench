import {mount} from 'enzyme';
import * as React from 'react';

import {cohortsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {CohortBuilderApi, CohortsApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {CohortPage} from './cohort-page.component';
import {cdrVersionStore} from "app/utils/stores";

describe('CohortPage', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
  });

  const component = () => {
    return mount(<CohortPage
        setCohortChanged={() => {}}
        setShowWarningModal={() => {}}
        setUpdatingCohort={() => {}}
        hideSpinner={() => {}}
        showSpinner={() => {}}
        spinnerVisible={false}
    />);
  }

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should render one search group for each includes/excludes item', async() => {
    const mockGetCohort = jest.spyOn(cohortsApi(), 'getCohort');
    const {id, namespace} = workspaceDataStub;
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledTimes(0);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(0);

    // Call cohort with 2 includes groups
    queryParamsStore.next({cohortId: 1});
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 1);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(2);
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(0);

    // Call cohort with 2 includes groups and one excludes group
    queryParamsStore.next({cohortId: 2});
    await waitOneTickAndUpdate(wrapper);
    expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 2);
    expect(wrapper.find('[data-test-id="includes-search-group"]').length).toBe(2);
    expect(wrapper.find('[data-test-id="excludes-search-group"]').length).toBe(1);
  });
});
