import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {Profile, ProfileApi, WorkspacesApi} from 'generated/fetch';
import * as React from 'react';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {AdminReviewWorkspace} from './admin-review-workspace';

describe('AdminReviewWorkspace', () => {
  let props: {profile: Profile, hideSpinner: Function, showSpinner: Function};

  const component = () => {
    return mount(<AdminReviewWorkspace {...props}/>);
  };

  beforeEach(() => {
    serverConfigStore.set({config: {
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    }});
    props = {
      profile: ProfileStubVariables.PROFILE_STUB,
      hideSpinner: () => {},
      showSpinner: () => {}
    };
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
  });

  it('should render and display a table', async () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="reviewWorkspacesTable"]').length).toBeGreaterThan(0);
  });

  it('should display workspaces for review', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="workspaceName"]').first().text())
      .toBe(workspaceStubs[0].name);
  });

  it('should no longer display the workspace once it has been approved or rejected', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const numWorkspaces = wrapper.find('[data-test-id="workspaceName"]').length;
    wrapper.find('[data-test-id="actionButtons"]').first()
      .find('[data-test-id="approve"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="workspaceName"]').length)
      .toBe(numWorkspaces - 1);
  });

});
