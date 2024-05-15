import * as React from 'react';

import {
  CohortBuilderApi,
  ConceptSetsApi,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { ConceptHomepage } from 'app/pages/data/concept/concept-homepage';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

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
    return render(
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

  it('should render', async () => {
    component();
    await screen.findByPlaceholderText(/search concepts in domain/i);
  });

  it('should have one card per domain.', async () => {
    component();
    const domainBoxes = await screen.findAllByTestId('domain-box');
    expect(domainBoxes.length).toBe(DomainStubVariables.STUB_DOMAINS.length);
  });

  it('should have one card per survey.', async () => {
    component();
    const surveyBoxes = await screen.findAllByTestId('survey-box');
    expect(surveyBoxes.length).toBe(SurveyStubVariables.STUB_SURVEYS.length);
  });
});
