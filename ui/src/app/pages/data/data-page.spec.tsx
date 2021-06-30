import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspaceStubVariables} from 'testing/stubs/workspaces';

import {ResourceCardBase} from 'app/components/card';
import {DataComponent} from 'app/pages/data/data-component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortReviewApi, CohortsApi, ConceptSetsApi, DataSetApi, WorkspacesApi} from 'generated/fetch';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {serverConfigStore} from 'app/utils/stores';
import {urlParamsStore} from '../../utils/url-params-store';


describe('DataPage', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    serverConfigStore.set({config: {enableGenomicExtraction: true, gsuiteDomain: ''}});
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', async() => {
    const wrapper = mount(<DataComponent />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show all datasets, cohorts, and concept sets', async() => {
    const wrapper = mount(<DataComponent />);
    const resourceCardsExpected =
      ConceptSetsApiStub.stubConceptSets().length +
      exampleCohortStubs.length +
      cohortReviewStubs.length +
      DataSetApiStub.stubDataSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCardBase).length).toBe(resourceCardsExpected);
  });

  it('should show only cohorts when selected', async() => {
    const wrapper = mount(<DataComponent />);
    const resourceCardsExpected = exampleCohortStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-cohorts"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCardBase).length).toBe(resourceCardsExpected);
  });

  it('should show only cohort reviews when selected', async() => {
    const wrapper = mount(<DataComponent />);
    const resourceCardsExpected = cohortReviewStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-cohort-reviews"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCardBase).length).toBe(resourceCardsExpected);
  });

  it('should show only conceptSets when selected', async() => {
    const wrapper = mount(<DataComponent />);
    const resourceCardsExpected = ConceptSetsApiStub.stubConceptSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-concept-sets"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCardBase).length).toBe(resourceCardsExpected);
  });

  it('should show only dataSets when selected', async() => {
    const wrapper = mount(<DataComponent />);
    const resourceCardsExpected = DataSetApiStub.stubDataSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-data-sets"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCardBase).length).toBe(resourceCardsExpected);
  });
});
