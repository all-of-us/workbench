import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { AppsApi, RuntimeApi, WorkspacesApi } from 'generated/fetch';

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
  });

  const createWrapperAndWaitForLoad = async () => {
    render(
      <MemoryRouter>
        <WorkspaceWrapper hideSpinner={() => {}} />
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
