import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {DataPage} from 'app/views/data-page/component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {ResourceCard} from 'app/views/resource-card/component';
import {CohortsApi, ConceptsApi, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspace-storage-service-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

describe('DataPage', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', async() => {
    const wrapper = mount(<DataPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show all datasets, cohorts, and concept sets', async() => {
    const wrapper = mount(<DataPage />);
    const resourceCardsExpected =
      ConceptSetsApiStub.stubConceptSets().length +
      exampleCohortStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCard).length).toBe(resourceCardsExpected);
  });

  it('should show only cohorts when selected', async() => {
    const wrapper = mount(<DataPage />);
    const resourceCardsExpected = exampleCohortStubs.length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-cohorts"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCard).length).toBe(resourceCardsExpected);
  });

  it('should show only conceptSets when selected', async() => {
    const wrapper = mount(<DataPage />);
    const resourceCardsExpected = ConceptSetsApiStub.stubConceptSets().length;
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="view-only-concept-sets"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(ResourceCard).length).toBe(resourceCardsExpected);
  });
});
