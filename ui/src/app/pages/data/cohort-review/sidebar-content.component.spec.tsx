import {cohortReviewStore} from 'app/services/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {CohortAnnotationDefinitionApi, CohortReviewApi} from 'generated/fetch';
import * as React from 'react';
import {CohortAnnotationDefinitionServiceStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {SidebarContent} from './sidebar-content.component';

describe('SidebarContent', () => {
  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortAnnotationDefinitionApi, new CohortAnnotationDefinitionServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
    cohortReviewStore.next(cohortReviewStubs[0]);
  });

  it('should render', () => {
    const wrapper = mount(<SidebarContent
      participant={{}}
      setParticipant={() => {}}
      annotations={[]}
      annotationDefinitions={[]}
      setAnnotations={() => {}}
      openCreateDefinitionModal={() => {}}
      openEditDefinitionsModal={() => {}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
