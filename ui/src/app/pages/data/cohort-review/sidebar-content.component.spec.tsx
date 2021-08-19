import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentCohortReviewStore, currentWorkspaceStore} from 'app/utils/navigation';
import {CohortAnnotationDefinitionApi, CohortReviewApi} from 'generated/fetch';
import {CohortAnnotationDefinitionServiceStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {SidebarContent} from './sidebar-content.component';
import { MemoryRouter, Route } from 'react-router-dom';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'), // use actual for all non-hook parts
  matchPath: (location, match) => ({
    params: {
      pid: cohortReviewStubs[0].participantCohortStatuses[0].participantId
    }
  })
}));

describe('SidebarContent', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortAnnotationDefinitionApi, new CohortAnnotationDefinitionServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
    currentCohortReviewStore.next(cohortReviewStubs[0]);
  });

  it('should render', () => {
    const {namespace, id} = workspaceDataStub;
    const {cohortId, participantCohortStatuses} = cohortReviewStubs[0]
    const wrapper = mount(
        <MemoryRouter>
          <SidebarContent
              location={{
                pathname: `/workspaces/${namespace}/${id}/data/cohorts/` +
                    `${cohortId}/review/participants/${participantCohortStatuses[0].participantId}`
              }}
          />
        </MemoryRouter>
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
