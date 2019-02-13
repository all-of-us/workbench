import {mount} from 'enzyme';
import * as React from 'react';
import {NotebookList} from './component';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {WorkspaceAccessLevel} from 'generated';
import {ProfileApi, WorkspacesApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

describe('NotebookList', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('should render notebooks', async () => {
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    const wrapper = mount(<NotebookList />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.text()).toMatch('mockFile');
  });
});
