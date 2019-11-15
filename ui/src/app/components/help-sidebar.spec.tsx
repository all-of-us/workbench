import {mount} from 'enzyme';
import * as React from 'react';

import {cohortReviewStore} from 'app/services/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortAnnotationDefinitionApi, CohortReviewApi} from 'generated/fetch';
import {CohortAnnotationDefinitionServiceStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';

import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {HelpSidebar} from './help-sidebar';

describe('SidebarContent', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortAnnotationDefinitionApi, new CohortAnnotationDefinitionServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
    cohortReviewStore.next(cohortReviewStubs[0]);
  });

  it('should render', () => {
    const wrapper = mount(<HelpSidebar location='data' />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
