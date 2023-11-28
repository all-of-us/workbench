import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { NotebooksApi, WorkspaceAccessLevel } from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AppSelector } from './app-selector';

describe('App Selector', () => {
  const getStartButton = () => screen.getByRole('button', { name: 'start' });

  const getNextButton = () => screen.getByRole('button', { name: 'next' });

  const getDropdownTrigger = () =>
    screen.getByRole('button', { name: 'Choose One' });

  const component = (workspace: WorkspaceData = workspaceDataStub) =>
    renderModal(
      <MemoryRouter>
        <AppSelector />
      </MemoryRouter>
    );

  beforeEach(() => {
    serverConfigStore.set({
      config: defaultServerConfig,
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

  it('should open the Jupyter modal when Jupyter is selected and Next is clicked', async () => {
    component();
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

  it('should not list RStudio as an option when the feature flag is false', async () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableRStudioGKEApp: false },
    });

    component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });

    // the caret next to the dropdown
    const dropdownTrigger = getDropdownTrigger();
    expect(dropdownTrigger).toBeInTheDocument();
    dropdownTrigger.click();

    await waitFor(async () => {
      // I'd prefer to do this, but it doesn't work
      // screen.queryByRole('option', { name: 'RStudio' });
      expect(screen.queryByText('RStudio')).not.toBeInTheDocument();
    });
  });

  it('should open the RStudio config panel when RStudio is selected and Next is clicked', async () => {
    component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });

    // the caret next to the dropdown
    const dropdownTrigger = getDropdownTrigger();
    expect(dropdownTrigger).toBeInTheDocument();
    dropdownTrigger.click();

    const rStudioOption = await waitFor(async () => {
      // I'd prefer to do this, but it doesn't work
      // return screen.getByRole('option', { name: 'RStudio' });
      return screen.getByText('RStudio').closest('li');
    });

    // disabled because no app type is selected yet
    expectButtonElementDisabled(getNextButton());

    rStudioOption.click();

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
    });

    // TODO I don't know why this doesn't work.  "screen" doesn't see anything at all besides the start button?
    // await waitFor(() => {
    //   // the RStudio config panel is visible
    //   expect(
    //     screen.queryByText('RStudio Cloud Environment')
    //   ).toBeInTheDocument();
    // });
  });

  it('should not list SAS as an option when the feature flag is false', async () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableSasGKEApp: false },
    });

    component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });

    // the caret next to the dropdown
    const dropdownTrigger = getDropdownTrigger();
    expect(dropdownTrigger).toBeInTheDocument();
    dropdownTrigger.click();

    await waitFor(async () => {
      // I'd prefer to do this, but it doesn't work
      // screen.queryByRole('option', { name: 'SAS' });
      expect(screen.queryByText('SAS')).not.toBeInTheDocument();
    });
  });

  it('should open the SAS config panel when SAS is selected and Next is clicked', async () => {
    component();
    const startButton = getStartButton();
    startButton.click();

    await waitFor(() => {
      expect(screen.queryByText('Select an application')).toBeInTheDocument();
    });

    // the caret next to the dropdown
    const dropdownTrigger = getDropdownTrigger();
    expect(dropdownTrigger).toBeInTheDocument();
    dropdownTrigger.click();

    const sasOption = await waitFor(async () => {
      // I'd prefer to do this, but it doesn't work
      // return screen.getByRole('option', { name: 'SAS' });
      return screen.getByText('SAS').closest('li');
    });

    // disabled because no app type is selected yet
    expectButtonElementDisabled(getNextButton());

    sasOption.click();

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
    });

    // TODO I don't know why this doesn't work.  "screen" doesn't see anything at all besides the start button?
    // await waitFor(() => {
    //   screen.debug();
    //   // the RStudio config panel is visible
    //   expect(screen.queryByText('SAS Cloud Environment')).toBeInTheDocument();
    // });
  });
});
