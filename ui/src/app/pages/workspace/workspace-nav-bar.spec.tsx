import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import { mockNavigate } from 'setupTests';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WorkspaceNavBar } from 'app/pages/workspace/workspace-nav-bar';
import { analysisTabName } from 'app/routing/utils';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('WorkspaceNavBar', () => {
  let props: {};
  let user;

  const component = () => {
    return render(
      <MemoryRouter
        initialEntries={[
          `/${workspaceDataStub.namespace}/${workspaceDataStub.id}`,
        ]}
      >
        <Route path='/:ns/:terraName'>
          <WorkspaceNavBar {...props} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    props = {};

    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({
      config: {
        gsuiteDomain: 'fake-research-aou.org',
      },
    });
    cdrVersionStore.set(cdrVersionTiersResponse);
    user = userEvent.setup();
  });

  it('should render the Data tab by default', async () => {
    component();
    const dataTab = await screen.findByLabelText('Data');
    waitFor(() => expect(dataTab).toHaveAttribute('aria-selected', 'true'));
  });

  it('should highlight the active tab', async () => {
    props = { tabPath: 'about' };
    component();
    const aboutTab = await screen.findByLabelText('About');
    waitFor(() => expect(aboutTab).toHaveAttribute('aria-selected', 'true'));
  });

  it('should navigate on tab click', async () => {
    component();

    await user.click(await screen.findByLabelText('Analysis'));
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      analysisTabName,
    ]);

    await user.click(await screen.findByLabelText('Data'));
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'data',
    ]);

    await user.click(await screen.findByLabelText('About'));
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'about',
    ]);
  });

  const setAdminLocked = (adminLocked: boolean) => {
    currentWorkspaceStore.next({ ...workspaceDataStub, adminLocked });
  };

  it('should not navigate on tab click if tab is disabled because it is admin-locked', async () => {
    setAdminLocked(true);

    component();

    await user.click(await screen.findByLabelText('Data'));
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'data',
    ]);

    await user.click(await screen.findByLabelText('Analysis'));
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      analysisTabName,
    ]);
  });

  it('should disable Data and Analysis tab if the workspace is admin-locked', () => {
    setAdminLocked(true);

    component();

    expectButtonElementDisabled(screen.getByLabelText('Data'));
    expectButtonElementDisabled(screen.getByLabelText('Analysis'));
    expectButtonElementEnabled(screen.getByLabelText('About'));
  });

  it('should display the default CDR Version with no new version flag or upgrade modal visible', () => {
    component();

    expect(
      screen.getByText(CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION)
    ).toBeInTheDocument();
    expect(screen.queryByTestId('new-version-flag')).not.toBeInTheDocument();
    expect(
      screen.queryByTestId('cdr-version-upgrade-modal')
    ).not.toBeInTheDocument();
  });

  it('should display an alternative CDR Version with a new version flag', () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId =
      CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    component();

    expect(
      screen.getByText(CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION)
    ).toBeInTheDocument();

    expect(screen.getByTestId('new-version-flag')).toBeInTheDocument();
    expect(
      screen.queryByTestId('cdr-version-upgrade-modal')
    ).not.toBeInTheDocument();
  });

  it('clicks the new version flag which should pop up the version upgrade modal', async () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId =
      CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    component();

    expect(
      screen.queryByTestId('cdr-version-upgrade-modal')
    ).not.toBeInTheDocument();

    await user.click(screen.getByTestId('new-version-flag'));

    expect(screen.getByTestId('cdr-version-upgrade-modal')).toBeInTheDocument();
  });
});
