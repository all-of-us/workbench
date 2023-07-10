import * as React from 'react';

import { WorkspaceAccessLevel, WorkspacesApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceCard } from './workspace-card';

describe('WorkspaceCard', () => {
  const reload = jest.fn();

  const component = (accessLevel: WorkspaceAccessLevel) => {
    return mountWithRouter(
      <WorkspaceCard
        accessLevel={accessLevel}
        reload={reload}
        workspace={workspaceStubs[0]}
      />,
      { attachTo: document.getElementById('root') }
    );
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    serverConfigStore.set({ config: { gsuiteDomain: 'abc' } });
  });

  it('should not show locked status for workspace that has adminLocked false', async () => {
    const wrapper = component(WorkspaceAccessLevel.OWNER);
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.exists('[data-test-id="workspace-lock"]')).toBeFalsy();
  });

  it('show locked status for workspace that has adminLocked true', async () => {
    workspaceStubs[0].adminLocked = true;
    const wrapper = component(WorkspaceAccessLevel.OWNER);
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.exists('[data-test-id="workspace-lock"]')).toBeTruthy();
  });
});
