import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { Route } from 'react-router-dom';

import { AppsApi, RuntimeApi, WorkspacesApi } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import { WorkspaceWrapper } from 'app/pages/workspace/workspace-wrapper';
import { workspacePath } from 'app/routing/utils';
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
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    cdrVersionStore.set(cdrVersionTiersResponse);
    registerApiClient(RuntimeApi, new RuntimeApiStub());
    registerApiClient(AppsApi, new AppsApiStub());

    const workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    workspacesApiStub.getWorkspace = jest.fn().mockResolvedValue({
      workspace: workspaceDataStub,
      accessLevel: workspaceDataStub.accessLevel,
    });
  });

  const createWrapperAndWaitForLoad = async () => {
    render(
      <MemoryRouter
        initialEntries={[
          workspacePath(
            workspaceDataStub.namespace,
            workspaceDataStub.terraName
          ),
        ]}
      >
        <Route path='/workspaces/:ns/:terraName'>
          <WorkspaceWrapper hideSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );

    return waitFor(() =>
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
      workspaceDataStub.terraName
    );

    // populated with the result of getWorkspace()
    expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    // unchanged
    expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
  });

  it('loads the workspace specified by the route when neither store matches the route', async () => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      namespace: 'something else',
      terraName: 'some other terraName',
    });
    nextWorkspaceWarmupStore.next({
      ...workspaceDataStub,
      namespace: 'another one',
      terraName: 'definitely not a match',
    });

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    await createWrapperAndWaitForLoad();
    expect(spy).toHaveBeenCalledWith(
      workspaceDataStub.namespace,
      workspaceDataStub.terraName
    );

    // populated with the result of getWorkspace()
    expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    // clears because invalid
    expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
  });

  it('renders but does not need to load the workspace when the currentWorkspaceStore matches the route', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const nextWs = {
      ...workspaceDataStub,
      namespace: 'something else',
      terraName: 'some other terraName',
    };
    nextWorkspaceWarmupStore.next(nextWs);

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    await createWrapperAndWaitForLoad();
    expect(spy).not.toHaveBeenCalled();

    // unchanged
    expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    expect(nextWorkspaceWarmupStore.getValue()).toEqual(nextWs);
  });

  it('renders but does not need to load the workspace when the nextWorkspaceWarmupStore matches the route', async () => {
    currentWorkspaceStore.next(undefined);
    nextWorkspaceWarmupStore.next(workspaceDataStub);

    const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
    await createWrapperAndWaitForLoad();
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
        terraName: 'some other terraName',
      });
      nextWorkspaceWarmupStore.next(workspaceDataStub);

      const spy = jest.spyOn(workspacesApi(), 'getWorkspace');
      await createWrapperAndWaitForLoad();
      expect(spy).not.toHaveBeenCalled();

      // the nextWorkspaceWarmupStore is consumed/cleared
      expect(nextWorkspaceWarmupStore.getValue()).toBeUndefined();
      // the currentWorkspaceStore is set to the previous nextWorkspaceWarmupStore
      expect(currentWorkspaceStore.getValue()).toEqual(workspaceDataStub);
    }
  );

  it('should show the multi-region workspace notification for workspaces created before 6/15/2022', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creationTime: new Date(2022, 5, 14).getTime(),
    });
    await createWrapperAndWaitForLoad();
    expect(
      screen.queryByText(
        'Workspaces created before 6/15/2022 use multi-region buckets',
        { exact: false }
      )
    ).toBeInTheDocument();
  });

  it('should not show the multi-region workspace notification for workspaces created during or after 6/15/2022', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creationTime: new Date(2022, 5, 15).getTime(),
    });
    await createWrapperAndWaitForLoad();
    expect(
      screen.queryByText(
        'Workspaces created before 6/15/2022 use multi-region buckets',
        { exact: false }
      )
    ).not.toBeInTheDocument();
  });

  it('should show UnlinkedBillingNotification when workspace billing is unlinked', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableUnlinkBillingForInitialCredits: true,
      },
    });

    currentWorkspaceStore.next({
      ...workspaceDataStub,
      initialCredits: {
        exhausted: true,
        expirationBypassed: false,
        expirationEpochMillis: Date.now() - 1000,
      },
    });

    await createWrapperAndWaitForLoad();

    expect(screen.getByText(/Add a payment method/i)).toBeInTheDocument();
    expect(
      screen.getByText(/Without a new payment method to cover ongoing costs/i)
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Cloud billing account/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /billing project/i })
    ).toBeInTheDocument();
  });

  it('should not show UnlinkedBillingNotification when feature flag is disabled', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableUnlinkBillingForInitialCredits: false,
      },
    });
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      initialCredits: {
        exhausted: true,
        expirationBypassed: false,
        expirationEpochMillis: Date.now() - 1000,
      },
    });
    await createWrapperAndWaitForLoad();
    expect(screen.queryByText(/Add a payment method/i)).not.toBeInTheDocument();
  });

  it('should not show UnlinkedBillingNotification when initialCredits is missing', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableUnlinkBillingForInitialCredits: true,
      },
    });
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      initialCredits: undefined,
    });
    await createWrapperAndWaitForLoad();
    expect(screen.queryByText(/Add a payment method/i)).not.toBeInTheDocument();
  });

  it('should not show UnlinkedBillingNotification when initialCredits not exhausted and not expired', async () => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      initialCredits: {
        exhausted: false,
        expirationBypassed: false,
        expirationEpochMillis: Date.now() + 100000,
      },
    });
    await createWrapperAndWaitForLoad();
    expect(screen.queryByText(/Add a payment method/i)).not.toBeInTheDocument();
  });
});
