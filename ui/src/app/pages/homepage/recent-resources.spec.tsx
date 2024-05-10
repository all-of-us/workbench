import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { UserMetricsApi, WorkspaceAccessLevel } from 'generated/fetch';

import { findByText, render, screen, waitFor } from '@testing-library/react';
import {
  MODIFIED_DATE_COLUMN_NUMBER,
  NAME_COLUMN_NUMBER,
  RESOURCE_TYPE_COLUMN_NUMBER,
  resourceTableColumns,
} from 'app/components/resources/resource-list.spec';
import { RecentResources } from 'app/pages/homepage/recent-resources';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  renderWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  UserMetricsApiStub,
  userMetricsApiStubResources,
} from 'testing/stubs/user-metrics-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

describe('RecentResourcesComponent', () => {
  beforeEach(() => {
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
  });

  it('should render resources in a workspace', async () => {
    renderWithRouter(
      <RecentResources
        workspaces={[
          {
            workspace: workspaceStubs[0],
            accessLevel: WorkspaceAccessLevel.OWNER,
          },
        ]}
      />
    );
    await screen.findByText('Recently Accessed Items');

    expect(screen.getByText('Cohort')).toBeInTheDocument();
    expect(
      screen.getByText(userMetricsApiStubResources[0].cohort.name)
    ).toBeInTheDocument();
  });

  it('should not render resources when their workspace is not available', async () => {
    renderWithRouter(<RecentResources workspaces={[]} />);

    await screen.findByText('Recently Accessed Items');

    expect(screen.queryByText(/item type/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/name/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/last modified date/i)).not.toBeInTheDocument();
  });
});
