import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';

import {CohortList} from 'app/views/cohort-list';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortsApi, WorkspacesApi} from 'generated/fetch';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {workspaceDataStub, WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

describe('CohortList', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should display cohorts', async () => {
    const wrapper = mount(<CohortList />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.text()).toMatch(exampleCohortStubs[0].name);
  });
});
