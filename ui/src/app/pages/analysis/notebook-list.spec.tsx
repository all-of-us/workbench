import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import { NotebooksApi, ProfileApi, WorkspacesApi} from 'generated/fetch';
import * as React from 'react';
import {MemoryRouter} from 'react-router';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {NotebookList} from './notebook-list';

describe('NotebookList', () => {
  beforeEach(() => {
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('should render notebooks', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(<MemoryRouter><NotebookList hideSpinner={() => {}} /></MemoryRouter>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.text()).toMatch('mockFile');
  });
});
