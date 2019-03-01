import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {DataSet} from 'app/views/dataset/component';
import {WorkspaceAccessLevel} from 'generated';
import {CohortsApi, ConceptsApi, ConceptSetsApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

describe('DataSet', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  });

  it('should render', async() => {
    const wrapper = mount(<DataSet />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

});
