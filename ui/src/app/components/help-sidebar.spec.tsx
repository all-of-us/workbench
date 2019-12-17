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

const sidebarContent = require('assets/json/help-sidebar.json');

describe('HelpSidebar', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortAnnotationDefinitionApi, new CohortAnnotationDefinitionServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
    cohortReviewStore.next(cohortReviewStubs[0]);
  });

  it('should render', () => {
    const wrapper = mount(<HelpSidebar helpContent='data' sidebarOpen={true} />);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should update content when helpContent prop changes', () => {
    const wrapper = mount(<HelpSidebar helpContent='data' sidebarOpen={true} />);
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(sidebarContent.data[0].title);
    wrapper.setProps({helpContent: 'cohortBuilder'});
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(sidebarContent.cohortBuilder[0].title);
  });

  it('should update marginRight style when sidebarOpen prop changes', () => {
    const wrapper = mount(<HelpSidebar helpContent='data' sidebarOpen={true} />);
    expect(wrapper.find('[data-test-id="sidebar-content"]').prop('style').marginRight).toBe(0);
    wrapper.setProps({sidebarOpen: false});
    expect(wrapper.find('[data-test-id="sidebar-content"]').prop('style').marginRight).toBe('calc(-14rem - 40px)');
  });
});
