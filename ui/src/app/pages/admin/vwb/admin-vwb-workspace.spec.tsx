import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';

import {
  Authority,
  Profile,
  UserRole,
  VwbWorkspace,
  VwbWorkspaceAdminApi,
  VwbWorkspaceAdminView,
  VwbWorkspaceAuditLog,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AdminVwbWorkspace } from './admin-vwb-workspace';

const ADMIN_PROFILE: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [
    Authority.RESEARCHER_DATA_VIEW,
    Authority.SECURITY_ADMIN,
    Authority.EGRESS_EVENTS,
  ],
};

const TEST_WORKSPACE: VwbWorkspace = {
  id: 'workspace-uuid-123',
  userFacingId: 'test-workspace-ufid',
  displayName: 'Test Workspace',
  createdBy: 'creator@example.com',
  creationTime: '2024-01-01T00:00:00Z',
  googleProjectId: 'test-gcp-project',
  description: 'Test workspace description',
  researchPurpose: JSON.stringify([
    {
      form_data: {
        intendedStudy: 'Test study',
        scientificApproach: 'Test approach',
        anticipatedFindings: 'Test findings',
      },
    },
  ]),
};

const TEST_COLLABORATORS: UserRole[] = [
  {
    email: 'owner@example.com',
    role: WorkspaceAccessLevel.OWNER,
  },
  {
    email: 'writer@example.com',
    role: WorkspaceAccessLevel.WRITER,
  },
];

const TEST_AUDIT_LOGS: VwbWorkspaceAuditLog[] = [
  {
    workspaceId: 'workspace-uuid-123',
    changeType: 'CREATE_WORKSPACE',
    changeTime: '2024-01-01T00:00:00Z',
    actorEmail: 'creator@example.com',
  },
  {
    workspaceId: 'workspace-uuid-123',
    changeType: 'GRANT_WORKSPACE_ROLE',
    changeTime: '2024-01-02T00:00:00Z',
    actorEmail: 'owner@example.com',
  },
];

class VwbWorkspaceAdminApiStub extends VwbWorkspaceAdminApi {
  getVwbWorkspaceAdminView = jest.fn(
    async (userFacingId: string): Promise<VwbWorkspaceAdminView> => {
      return {
        workspace: TEST_WORKSPACE,
        collaborators: TEST_COLLABORATORS,
      };
    }
  );

  getVwbWorkspaceAuditLogs = jest.fn(
    async (workspaceId: string): Promise<VwbWorkspaceAuditLog[]> => {
      return TEST_AUDIT_LOGS;
    }
  );

  getVwbWorkspaceResources = jest.fn(async (workspaceId: string) => {
    // Return 403 to simulate AoD not active
    throw new Response(null, { status: 403 });
  });

  enableAccessOnDemandByUserFacingId = jest.fn(async () => {
    return {};
  });

  deleteVwbWorkspaceResource = jest.fn(async () => {
    return {};
  });
}

describe('AdminVwbWorkspace', () => {
  let user;
  let vwbWorkspaceAdminApiStub: VwbWorkspaceAdminApiStub;

  const component = (ufid: string = 'test-workspace-ufid') => {
    return render(
      <MemoryRouter initialEntries={[`/admin/vwb/workspaces/${ufid}`]}>
        <Route path='/admin/vwb/workspaces/:ufid'>
          <AdminVwbWorkspace
            match={{ params: { ufid } } as any}
            hideSpinner={() => {}}
            showSpinner={() => {}}
          />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });

    profileStore.set({
      profile: ADMIN_PROFILE,
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    vwbWorkspaceAdminApiStub = new VwbWorkspaceAdminApiStub();
    registerApiClient(VwbWorkspaceAdminApi, vwbWorkspaceAdminApiStub);
    user = userEvent.setup();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const waitUntilPageLoaded = async () => {
    await screen.findByText('Test Workspace');
  };

  it('should render the workspace details', async () => {
    component();
    await waitUntilPageLoaded();

    expect(screen.getByText('Test Workspace')).toBeInTheDocument();
    expect(screen.getByText('creator@example.com')).toBeInTheDocument();
    expect(screen.getByText('test-gcp-project')).toBeInTheDocument();
  });

  it('should display collaborators', async () => {
    component();
    await waitUntilPageLoaded();

    await waitFor(() => {
      expect(screen.getByText('owner@example.com')).toBeInTheDocument();
      expect(screen.getByText('writer@example.com')).toBeInTheDocument();
    });
  });

  it('should display workspace activity', async () => {
    component();
    await waitUntilPageLoaded();

    await waitFor(() => {
      expect(screen.getByText('CREATE_WORKSPACE')).toBeInTheDocument();
      expect(screen.getByText('GRANT_WORKSPACE_ROLE')).toBeInTheDocument();
    });
  });

  it('should call getVwbWorkspaceAdminView API on mount', async () => {
    component();
    await waitUntilPageLoaded();

    expect(
      vwbWorkspaceAdminApiStub.getVwbWorkspaceAdminView
    ).toHaveBeenCalledWith('test-workspace-ufid');
  });

  it('should call getVwbWorkspaceAuditLogs API on mount', async () => {
    component();
    await waitUntilPageLoaded();

    await waitFor(() => {
      expect(
        vwbWorkspaceAdminApiStub.getVwbWorkspaceAuditLogs
      ).toHaveBeenCalledWith('workspace-uuid-123');
    });
  });

  it('should handle workspace not found', async () => {
    vwbWorkspaceAdminApiStub.getVwbWorkspaceAdminView = jest.fn(async () => {
      throw new Response('Not found', { status: 404 });
    });

    component('nonexistent-ufid');

    await waitFor(() => {
      expect(screen.getByText(/Error loading data/i)).toBeInTheDocument();
    });
  });

  it('should show manual access section with AoD button', async () => {
    component();
    await waitUntilPageLoaded();

    expect(
      screen.getByText('Manual Access to Workspace (Optional)')
    ).toBeInTheDocument();
    expect(screen.getByText('Request Temporary Access')).toBeInTheDocument();
  });

  it('should show workspace resources section', async () => {
    component();
    await waitUntilPageLoaded();

    expect(screen.getByText('Workspace Resources')).toBeInTheDocument();
    expect(screen.getByText('Fetch Workspace Data')).toBeInTheDocument();
  });

  it('should not show workspace resources if user lacks authority', async () => {
    profileStore.set({
      profile: {
        ...ADMIN_PROFILE,
        authorities: [Authority.RESEARCHER_DATA_VIEW],
      },
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    component();
    await waitUntilPageLoaded();

    expect(screen.queryByText('Workspace Resources')).not.toBeInTheDocument();
  });
});
