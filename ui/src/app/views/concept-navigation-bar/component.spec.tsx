import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptNavigationBar} from 'app/views/concept-navigation-bar/component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptsApi, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspaceAccessLevel} from 'generated';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';

describe('ConceptNavigationBar', () => {

  const component = () => {
    return mount(<ConceptNavigationBar ns='test' wsId='1' showConcepts={true}/>);
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
