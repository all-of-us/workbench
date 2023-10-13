import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { Route } from 'react-router-dom';

import { AppsApi, RuntimeApi, WorkspacesApi } from 'generated/fetch';

import { currentWorkspaceStore } from '../../utils/navigation';
import { screen } from '@testing-library/dom';
import { render, waitForElementToBeRemoved } from '@testing-library/react';
import { WorkspaceWrapper } from 'app/pages/workspace/workspace-wrapper';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

// HelpSidebar has very many dependencies, and we don't care about it here
// so let's avoid getting distracted by HelpSidebar errors
jest.mock('app/components/help-sidebar', () => {
  return {
    HelpSidebar: () => <div>Mock Help Sidebar</div>,
  };
});

// WorkspaceRoutes is crashing when we set routes (unclear why) but we don't care about it here
// so let's avoid getting distracted by WorkspaceRoutes errors
jest.mock('app/routing/workspace-app-routing', () => {
  return {
    WorkspaceRoutes: () => <div>Mock Workspace Routes</div>,
  };
});

describe(WorkspaceWrapper.name, () => {
  let workspaceData: typeof workspaceDataStub = null;

  beforeEach(() => {
    workspaceData = { ...workspaceDataStub };
    serverConfigStore.set({ config: defaultServerConfig });
    cdrVersionStore.set(cdrVersionTiersResponse);
    registerApiClient(RuntimeApi, new RuntimeApiStub());
    registerApiClient(AppsApi, new AppsApiStub());

    const workspacesApi = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApi);
    workspacesApi.getWorkspace = jest
      .fn()
      .mockResolvedValue({ workspace: workspaceData });

    // this captures the behavior of a current bug (RW-11140) with this component:
    // it doesn't work when the currentWorkspaceStore matches the route being served
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      namespace: 'something else',
      id: 'some other ID',
    });
  });

  const createWrapperAndWaitForLoad = async () => {
    // adding initialEntries and Route breaks the test, but how?
    // this causes these route params to be populated, but why does this break the test?

    render(
      <MemoryRouter
        initialEntries={[
          `/workspaces/${workspaceDataStub.namespace}/${workspaceDataStub.id}`,
        ]}
      >
        <Route path='/workspaces/:ns/:wsid'>
          <WorkspaceWrapper hideSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );

    await waitForElementToBeRemoved(() =>
      screen.getByTitle('loading workspaces spinner')
    );
  };

  it('should show the multi-region workspace notification for workspaces created before 6/15/2022', async () => {
    workspaceData.creationTime = new Date(2022, 5, 14).getTime();
    await createWrapperAndWaitForLoad();
    expect(
      screen.queryByText(
        'Workspaces created before 6/15/2022 use multi-region buckets',
        { exact: false }
      )
    ).toBeInTheDocument();
  });

  it('should not show the multi-region workspace notification for workspaces created during or after 6/15/2022', async () => {
    workspaceData.creationTime = new Date(2022, 5, 15).getTime();
    await createWrapperAndWaitForLoad();
    expect(
      screen.queryByText(
        'Workspaces created before 6/15/2022 use multi-region buckets',
        { exact: false }
      )
    ).not.toBeInTheDocument();
  });
});
