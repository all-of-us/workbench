import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { MemoryRouter } from 'react-router';

import { NotebooksApi, WorkspaceAccessLevel } from 'generated/fetch';

import { WorkspaceData } from '../../utils/workspace-data';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UIAppType } from 'app/components/apps-panel/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
  simulateComponentChange,
} from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AppSelector } from './app-selector';
import { APP_LIST } from './app-selector-modal';

describe('App Selector', () => {
  const getStartButton = () => screen.getByRole('button', { name: 'start' });

  const getNextButton = () => screen.getByRole('button', { name: 'next' });

  const getDropdownTrigger = () =>
    screen.getByRole('button', { name: 'Choose One' });

  const applicationListDropDownWrapper = (container) => {
    return container
      .querySelector('[data-test-id="application-list-dropdown"]')
      .first();
  };

  const component = (workspace: WorkspaceData = workspaceDataStub) =>
    renderModal(
      <MemoryRouter>
        <AppSelector {...{ workspace }} />
      </MemoryRouter>
    );

  beforeEach(() => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
    registerApiClient(NotebooksApi, new NotebooksApiStub());
  });

  it('should enable START button if user has OWNER access', async () => {
    // By default the workspace access level is OWNER
    const { container } = component();
    expect(container).toBeInTheDocument();
    expectButtonElementEnabled(getStartButton());
  });

  it('should enable START button if user has WRITER access', async () => {
    component({
      ...workspaceDataStub,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    expectButtonElementEnabled(getStartButton());
  });

  it('should disable START button if user has READER access', async () => {
    component({
      ...workspaceDataStub,
      accessLevel: WorkspaceAccessLevel.READER,
    });
    expectButtonElementDisabled(getStartButton());
  });

  it('should open the select application modal on clicking start button', async () => {
    component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });
  });

  it('should open jupyter modal when Jupyter application is selected and next button is clicked', async () => {
    const { container } = component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });

    // the caret next to the dropdown
    const dropdownTrigger = getDropdownTrigger();
    expect(dropdownTrigger).toBeInTheDocument();
    dropdownTrigger.click();

    const jupyterOption = await waitFor(async () => {
      // I'd prefer to do this, but it doesn't work
      // return screen.getByRole('option', { name: 'Jupyter' });
      return screen.getByText('Jupyter').closest('li');
    });

    // disabled because no app type is selected yet
    expectButtonElementDisabled(getNextButton());

    jupyterOption.click();

    // now enabled
    const nextButton = await waitFor(() => {
      const next = getNextButton();
      expectButtonElementEnabled(next);
      return next;
    });
    nextButton.click();

    await waitFor(() => {
      // the selection modal is gone
      expect(
        screen.queryByText('Select an application')
      ).not.toBeInTheDocument();
      // the Jupyter modal is visible
      expect(screen.queryByText('New Notebook')).toBeInTheDocument();
    });
  });
});
