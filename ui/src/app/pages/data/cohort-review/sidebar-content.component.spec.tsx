import { mount } from 'enzyme';
import * as React from 'react';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortReviewStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import {
  CohortAnnotationDefinitionApi,
  CohortReviewApi,
} from 'generated/fetch';
import { CohortAnnotationDefinitionServiceStub } from 'testing/stubs/cohort-annotation-definition-service-stub';
import {
  CohortReviewServiceStub,
  cohortReviewStubs,
} from 'testing/stubs/cohort-review-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { SidebarContent } from './sidebar-content.component';
import { MemoryRouter } from 'react-router-dom';

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

  it('should render', () => {
    const wrapper = mount(
      <MemoryRouter>
        <SidebarContent
          participant={cohortReviewStubs[0].participantCohortStatuses[0]}
        />
      </MemoryRouter>
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
