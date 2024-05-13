import * as React from 'react';

import {
  CohortAnnotationDefinitionApi,
  CohortReviewApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortReviewStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';

import { renderWithRouter } from 'testing/react-test-helpers';
import { CohortAnnotationDefinitionServiceStub } from 'testing/stubs/cohort-annotation-definition-service-stub';
import {
  CohortReviewServiceStub,
  cohortReviewStubs,
} from 'testing/stubs/cohort-review-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { SidebarContent } from './sidebar-content.component';

describe('SidebarContent', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(
      CohortAnnotationDefinitionApi,
      new CohortAnnotationDefinitionServiceStub()
    );
    currentWorkspaceStore.next(workspaceDataStub);
    currentCohortReviewStore.next(cohortReviewStubs[0]);
  });

  it('should render', async () => {
    renderWithRouter(
      <SidebarContent
        participant={cohortReviewStubs[0].participantCohortStatuses[0]}
      />
    );
    await screen.findByText(/Choose a Review Status for Participant/i);
  });
});
