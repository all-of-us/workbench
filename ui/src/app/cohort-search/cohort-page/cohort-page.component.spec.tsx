import {mount} from 'enzyme';
import * as React from 'react';

import {cohortsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {cdrVersionStore} from 'app/utils/navigation';
import {CohortBuilderApi, CohortsApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {CohortPage} from './cohort-page.component';

describe('CohortPage', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.next(cdrVersionListResponse);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
  });

  it('should render', () => {
    const wrapper = mount(<CohortPage setCohortChanged={() => {}} setShowWarningModal={() => {}} setUpdatingCohort={() => {}}/>);
    expect(wrapper).toBeTruthy();
  });

  it('should render one search group for each includes/excludes item', async() => {
    const mockGetCohort = jest.spyOn(cohortsApi(), 'getCohort');
    const {id, namespace} = workspaceDataStub;
    const wrapper = mount(<CohortPage setCohortChanged={() => {}} setShowWarningModal={() => {}} setUpdatingCohort={() => {}}/>);
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
