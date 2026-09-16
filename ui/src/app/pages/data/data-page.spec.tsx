import * as React from 'react';

import { Profile, WorkspacesApi } from 'generated/fetch';

import { act, screen } from '@testing-library/react';
import { DataComponent } from 'app/pages/data/data-component';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  ProfileStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';

import { renderWithRouter, waitForNoSpinner } from 'testing/react-test-helpers';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

describe('DataPage', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    profileStore.set({
      profile: { username: 'testUser' } as Profile,
    } as ProfileStore);
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  const component = () => {
    return renderWithRouter(
      <DataComponent hideSpinner={() => {}} showSpinner={() => {}} />
    );
  };

  it('should render', async () => {
    await act(async () => {
      component();
    });
    // Wait for loading to complete and verify the component has rendered
    await waitForNoSpinner();

    // Verify that the main sections are rendered
    expect(
      screen.getByRole('heading', { name: 'Cohorts' })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Datasets' })
    ).toBeInTheDocument();
    expect(screen.getByText('Show All')).toBeInTheDocument();
  });
});
