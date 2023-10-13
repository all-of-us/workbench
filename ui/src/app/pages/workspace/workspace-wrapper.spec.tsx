import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { Route } from 'react-router-dom';

import { AppsApi, RuntimeApi, WorkspacesApi } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import {
  render,
  waitFor,
  waitForElementToBeRemoved,
} from '@testing-library/react';
import { WorkspaceWrapper } from 'app/pages/workspace/workspace-wrapper';
import {
  registerApiClient,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import {
  currentWorkspaceStore,
  nextWorkspaceWarmupStore,
} from 'app/utils/navigation';
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

    const workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
    workspacesApiStub.getWorkspace = jest
      .fn()
      .mockResolvedValue({ workspace: workspaceData });
  });

  const createWrapperAndWaitForLoad = async (
    expectingSpinner: boolean = false
  ) => {
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

    if (expectingSpinner) {
      await waitForElementToBeRemoved(() =>
        screen.getByTitle('loading workspaces spinner')
      );
    }

    await waitFor(() =>
      expect(screen.queryByText('Mock Workspace Routes')).toBeInTheDocument()
    );
  };

  it('loads the workspace specified by the route when the workspace stores are empty', async () => {
    currentWorkspaceStore.next(undefined);
    nextWorkspaceWarmupStore.next(undefined);

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    await createWrapperAndWaitForLoad();
    expect(spy).toHaveBeenCalledWith(
      workspaceDataStub.namespace,
      workspaceDataStub.id
    );

    // populated with the result of getWorkspace()
    expect(currentWorkspaceStore.getValue()).toEqual({
      ...workspaceData,
      accessLevel: workspaceData.accessLevel,
    });
    // unchanged
    expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
  });

  it('loads the workspace specified by the route when neither store matches the route', async () => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      namespace: 'something else',
      id: 'some other ID',
    });
    nextWorkspaceWarmupStore.next({
      ...workspaceDataStub,
      namespace: 'another one',
      id: 'definitely not a match',
    });

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    await createWrapperAndWaitForLoad();
    expect(spy).toHaveBeenCalledWith(
      workspaceDataStub.namespace,
      workspaceDataStub.id
    );

    // populated with the result of getWorkspace()
    expect(currentWorkspaceStore.getValue()).toEqual({
      ...workspaceData,
      accessLevel: workspaceData.accessLevel,
    });
    // clears because invalid
    expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
  });

  it('renders but does not need to load the workspace when the currentWorkspaceStore matches the route', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const nextWs = {
      ...workspaceDataStub,
      namespace: 'something else',
      id: 'some other ID',
    };
    nextWorkspaceWarmupStore.next(nextWs);

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    const expectingSpinner = false; // no spinner when loading from the store
    await createWrapperAndWaitForLoad(expectingSpinner);
    expect(spy).not.toHaveBeenCalled();

    // unchanged
    expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    expect(nextWorkspaceWarmupStore.getValue()).toEqual(nextWs);
  });

  it('renders but does not need to load the workspace when the nextWorkspaceWarmupStore matches the route', async () => {
    currentWorkspaceStore.next(undefined);
    nextWorkspaceWarmupStore.next(workspaceDataStub);

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    const expectingSpinner = false; // no spinner when loading from the store
    await createWrapperAndWaitForLoad(expectingSpinner);
    expect(spy).not.toHaveBeenCalled();

    // the nextWorkspaceWarmupStore is consumed/cleared
    expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
    // the currentWorkspaceStore is set to the previous nextWorkspaceWarmupStore
    expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
  });

  it(
    'renders but does not need to load the workspace when the nextWorkspaceWarmupStore matches the route' +
      'and the currentWorkspaceStore mismatches the route',
    async () => {
      currentWorkspaceStore.next({
        ...workspaceDataStub,
        namespace: 'something else',
        id: 'some other ID',
      });
      nextWorkspaceWarmupStore.next(workspaceDataStub);

      const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
      const expectingSpinner = false; // no spinner when loading from the store
      await createWrapperAndWaitForLoad(expectingSpinner);
      expect(spy).not.toHaveBeenCalled();

      // the nextWorkspaceWarmupStore is consumed/cleared
      expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
      // the currentWorkspaceStore is set to the previous nextWorkspaceWarmupStore
      expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    }
  );

  it('should show the multi-region workspace notification for workspaces created before 6/15/2022', async () => {
    currentWorkspaceStore.next(undefined);
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
    currentWorkspaceStore.next(undefined);
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
