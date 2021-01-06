import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptNavigationBar} from 'app/pages/data/concept/concept-navigation-bar';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

describe('ConceptNavigationBar', () => {

  const component = () => {
    return mount(<ConceptNavigationBar ns='test' wsId='1' showConcepts={true}/>);
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
