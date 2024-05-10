import * as React from 'react';
import { mount } from 'enzyme';

import {
  CohortBuilderApi,
  ConceptSetsApi,
  WorkspacesApi,
} from 'generated/fetch';

import { ConceptHomepage } from 'app/pages/data/concept/concept-homepage';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  CohortBuilderServiceStub,
  DomainStubVariables,
  SurveyStubVariables,
} from 'testing/stubs/cohort-builder-service-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

describe('ConceptHomepage', () => {
  const component = () => {
    return mount(
      <ConceptHomepage
        setConceptSetUpdating={() => {}}
        setShowUnsavedModal={() => {}}
        setUnsavedConceptChanges={() => {}}
        hideSpinner={() => {}}
        showSpinner={() => {}}
      />
    );
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should have one card per domain.', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box-name"]').length).toBe(
      DomainStubVariables.STUB_DOMAINS.length
    );
  });

  it('should have one card per survey.', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="survey-box-name"]').length).toBe(
      SurveyStubVariables.STUB_SURVEYS.length
    );
  });
});
