import * as React from 'react';

import {
  FeaturedWorkspaceCategory,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceCard } from './workspace-card';

describe('WorkspaceCard', () => {
  const reload = jest.fn();

  const component = (accessLevel: WorkspaceAccessLevel) => {
    return renderWithRouter(
      <WorkspaceCard
        accessLevel={accessLevel}
        reload={reload}
        workspace={workspaceStubs[0]}
      />
    );
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    serverConfigStore.set({ config: { gsuiteDomain: 'abc' } });
  });

  it('should not show locked status for workspace that has adminLocked false', async () => {
    component(WorkspaceAccessLevel.OWNER);
    expect(await screen.findByTestId('workspace-card')).toBeInTheDocument();
    expect(screen.queryByTestId('workspace-lock')).not.toBeInTheDocument();
  });

  it('show locked status for workspace that has adminLocked true', async () => {
    workspaceStubs[0].adminLocked = true;
    component(WorkspaceAccessLevel.OWNER);
    expect(await screen.findByTestId('workspace-lock')).toBeInTheDocument();
  });

  it('show Community workspace icon if workspace is published by owner', async () => {
    let communityWorkspaceImg = screen.queryByRole('img', {
      name: /community workspace/i,
    });
    expect(communityWorkspaceImg).not.toBeInTheDocument();

    // Mark the workspace as a community workspace
    workspaceStubs[0].featuredCategory = FeaturedWorkspaceCategory.COMMUNITY;
    component(WorkspaceAccessLevel.OWNER);

    communityWorkspaceImg = screen.queryByRole('img', {
      name: /community workspace/i,
    });
    expect(communityWorkspaceImg).toBeInTheDocument();
  });
});
