import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {RecentResources} from 'app/pages/homepage/recent-resources';
import {UserMetricsApi, WorkspacesApi} from 'generated/fetch';
import {UserMetricsApiStub} from 'testing/stubs/user-metrics-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

describe('RecentResourcesComponent', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
  });

  it('should render outside of a workspace', () => {
    currentWorkspaceStore.next(undefined);
    const wrapper = mount(<RecentResources />);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should render in a workspace', () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(<RecentResources />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
