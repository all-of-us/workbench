import {mount} from 'enzyme';
import * as React from 'react';
import {NotebookList} from './notebook-list';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi, WorkspacesApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

describe('NotebookList', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('should render notebooks', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(<NotebookList hideSpinner={() => {}} />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.text()).toMatch('mockFile');
  });
});
