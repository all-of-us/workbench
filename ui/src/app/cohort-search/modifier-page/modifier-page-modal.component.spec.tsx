import {mount, shallow} from 'enzyme';

import {cohortBuilderApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, serverConfigStore, urlParamsStore} from 'app/utils/navigation';
import {
  CohortBuilderApi, CohortsApi, Domain, ModifierType,
  WorkspacesApi
} from 'generated/fetch';
import * as React from 'react';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';
import {workspaceDataStub, WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {ModifierPageModal} from './modifier-page-modal.component';


describe('ListModifierPage', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());

    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.next({
      enableEventDateModifier: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    });
  });

  it('should render', () => {
    const wrapper = shallow(<ModifierPageModal disabled={() => {}} wizard={{}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should display Only Age Event modifier for SURVEY', async() => {
    const survey = Domain.SURVEY;
    const wrapper = mount(<ModifierPageModal disabled={() => {
    }} wizard={{}}
                                        searchContext={{domain: survey, item: {modifiers: []}}}/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="' + ModifierType.AGEATEVENT + '"]').length)
      .toBeGreaterThan(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.NUMOFOCCURRENCES + '"]').length)
      .toBe(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.ENCOUNTERS + '"]').length)
      .toBe(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.EVENTDATE + '"]').length)
      .toBe(0);
  });
});
