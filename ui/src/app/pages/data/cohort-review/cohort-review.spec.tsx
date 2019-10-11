
import {shallow} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from "app/services/swagger-fetch-clients";
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi, CohortReviewApi, CohortsApi, WorkspaceAccessLevel} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {CohortReview} from './cohort-review';

describe('CohortReview', () => {
  registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
  registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  registerApiClient(CohortsApi, new CohortsApiStub());
  beforeEach(() => {
    currentWorkspaceStore.next({
      accessLevel: WorkspaceAccessLevel.OWNER,
      cdrVersionId: '2',
      name: 'Test Workspace'
    });
  });

  it('should render CohortReview', () => {
    const wrapper = shallow(<CohortReview />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
