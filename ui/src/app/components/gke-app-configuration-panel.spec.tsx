import * as React from 'react';

import {
  AppsApi,
  AppType,
  DisksApi,
  UserAppEnvironment,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import {
  appsApi,
  disksApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { notificationStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { expectButtonElementEnabled } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';

import {
  GKEAppConfigurationPanel,
  GkeAppConfigurationPanelProps,
  GKEAppPanelContent,
} from './gke-app-configuration-panel';
import { defaultProps as rstudioDefaultProps } from './gke-app-configuration-panels/create-rstudio.spec';

// component text for selectors

const cromwellIntroTextRegex = /Cromwell is a workflow execution engine/;
const defaultIntroTextRegex =
  /Your cloud environment is unique to this workspace and not shared with other users/;
const rstudioIntroTextRegex = defaultIntroTextRegex;
const deleteUnattachedPdRegex =
  /Deletes your persistent disk, which will also delete all files/;
const confirmDeleteGkeAppText =
  'You’re about to delete your cloud analysis environment.';
const confirmDeleteGkeAppWithPdText =
  'Keep persistent disk, delete environment';

const validateInitialLoadingSpinner = async () => {
  expect(screen.queryByLabelText('Please Wait')).not.toBeNull();
  return waitFor(() => {
    expect(screen.queryByLabelText('Please Wait')).toBeNull();
  });
};

describe(GKEAppConfigurationPanel.name, () => {
  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    registerApiClient(DisksApi, new DisksApiStub());
    notificationStore.set(null);

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
      },
    });

    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));

    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
  });

  const defaultProps: GkeAppConfigurationPanelProps = {
    appType: AppType.RSTUDIO,
    workspaceNamespace: 'aou-rw-1234',
    onClose: jest.fn(),
    initialPanelContent: null,
    creatorFreeCreditsRemaining: 300,

    // Use RSTUDIO_DEFAULT_PROPS for the rest of the props since they shouldn't affect this test
    workspace: rstudioDefaultProps.workspace,
    profileState: rstudioDefaultProps.profileState,
  };

  const createWrapper = (
    propOverrides: Partial<GkeAppConfigurationPanelProps> = {}
  ) => {
    const props = {
      ...defaultProps,
      ...propOverrides,
    };
    return render(<GKEAppConfigurationPanel {...props} />);
  };

  it('should show a loading spinner while waiting for the list apps API call to return', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    createWrapper();
    await validateInitialLoadingSpinner();
  });

  it('should show a loading spinner while waiting for the list disks API call to return', async () => {
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    createWrapper();
    await validateInitialLoadingSpinner();
  });

  it('should show an error if the list apps API call fails', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.reject());

    expect(notificationStore.get()).toBeNull();

    createWrapper();
    await waitFor(() => {
      expect(notificationStore.get().title).toBeTruthy();
      expect(notificationStore.get().message).toBeTruthy();
    });
  });

  it('should show an error if the list disks API call fails', async () => {
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.reject());

    expect(notificationStore.get()).toBeNull();

    createWrapper();
    await waitFor(() => {
      expect(notificationStore.get().title).toBeTruthy();
      expect(notificationStore.get().message).toBeTruthy();
    });
  });

  it('should not show an error if both fetch API calls succeed', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));

    expect(notificationStore.get()).toBeNull();

    createWrapper();
    await waitFor(() => {
      expect(notificationStore.get()).toBeNull();
    });
  });

  it('should display the Cromwell creation panel if no initial panel is passed', async () => {
    createWrapper({
      appType: AppType.CROMWELL,
      initialPanelContent: null,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
    });
  });

  it('should display the initial panel if an initial panel is passed', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation(
        (): Promise<any> => Promise.resolve([createListAppsCromwellResponse()])
      );
    const cromwellDisk = stubDisk();
    cromwellDisk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([cromwellDisk]));
    createWrapper({
      appType: AppType.CROMWELL,
      initialPanelContent: GKEAppPanelContent.DELETE_GKE_APP,
    });

    await waitFor(() => {
      expect(screen.queryByText(confirmDeleteGkeAppWithPdText)).not.toBeNull();
    });
  });

  it('should display the Cromwell panel when the type is Cromwell', async () => {
    createWrapper({
      appType: AppType.CROMWELL,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
      expect(screen.queryByText(rstudioIntroTextRegex)).toBeNull();
    });
  });

  it('should display the RStudio panel when the type is RStudio', async () => {
    createWrapper({
      appType: AppType.RSTUDIO,
    });

    await waitFor(() => {
      expect(screen.queryByText(rstudioIntroTextRegex)).not.toBeNull();
      expect(screen.queryByText(cromwellIntroTextRegex)).toBeNull();
    });
  });

  it('should change panels from CREATE to DELETE_UNATTACHED_PD after clicking the delete PD button', async () => {
    // add an unattached PD
    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));

    createWrapper({
      appType: AppType.CROMWELL,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
      expect(screen.queryByText(deleteUnattachedPdRegex)).toBeNull();
    });

    const deleteButton = screen.getByLabelText('Delete Persistent Disk');
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    await waitFor(() => {
      expect(screen.queryByText(deleteUnattachedPdRegex)).not.toBeNull();
      expect(screen.queryByText(cromwellIntroTextRegex)).toBeNull();
    });
  });

  it('should call the delete GKE app api after confirming deleting a GKE app', async () => {
    const deleteAppStub = jest
      .spyOn(appsApi(), 'deleteApp')
      .mockImplementation((): Promise<any> => Promise.resolve());
    const onCloseStub = jest.fn();

    // Setup: The DELETE_GKE_APP panel is open. A Cromwell disk exists and the current app is Cromwell.
    const workspaceNamespace = 'aou-rw-1234';
    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));
    const app = createListAppsCromwellResponse();
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([app]));
    createWrapper({
      onClose: onCloseStub,
      appType: AppType.CROMWELL,
      workspaceNamespace,
      initialPanelContent: GKEAppPanelContent.DELETE_GKE_APP,
    });

    // confirm that the correct panel is visible
    await waitFor(() => {
      expect(screen.queryByText(confirmDeleteGkeAppWithPdText)).not.toBeNull();
    });

    const deletePDRadioButton = screen.getByRole('radio', {
      name: 'Delete persistent disk and environment',
    });
    deletePDRadioButton.click();

    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    const deletePDSelected = true;
    await waitFor(() => {
      expect(deleteAppStub).toHaveBeenCalledWith(
        workspaceNamespace,
        app.appName,
        deletePDSelected
      );
      expect(onCloseStub).toHaveBeenCalled();
    });
  });

  it('should open the ConfirmDelete (without PD) panel when deleting a GKE app with no PD', async () => {
    const deleteAppStub = jest
      .spyOn(appsApi(), 'deleteApp')
      .mockImplementation((): Promise<any> => Promise.resolve());
    const onCloseStub = jest.fn();

    // Setup: The DELETE_GKE_APP panel is open. The current app is Cromwell, with no associated disks.

    const noDisks = [];
    const noDiskAppOverride: Partial<UserAppEnvironment> = { diskName: null };
    const app = createListAppsCromwellResponse(noDiskAppOverride);

    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve(noDisks));
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([app]));

    const workspaceNamespace = 'aou-rw-1234';
    createWrapper({
      onClose: onCloseStub,
      appType: AppType.CROMWELL,
      workspaceNamespace,
      initialPanelContent: GKEAppPanelContent.DELETE_GKE_APP,
    });

    // confirm that the correct panel is visible
    await waitFor(() => {
      expect(screen.queryByText(confirmDeleteGkeAppText)).not.toBeNull();
    });

    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    const deletePDSelected = false;
    await waitFor(() => {
      expect(deleteAppStub).toHaveBeenCalledWith(
        workspaceNamespace,
        app.appName,
        deletePDSelected
      );

      expect(onCloseStub).toHaveBeenCalled();
    });
  });

  it('should close the panel after cancelling deleting a GKE app', async () => {
    const onCloseStub = jest.fn();

    // Setup: The DELETE_GKE_APP panel is open. A Cromwell disk exists and the current app is Cromwell.
    const workspaceNamespace = 'aou-rw-1234';
    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));
    const app = createListAppsCromwellResponse();
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([app]));
    createWrapper({
      onClose: onCloseStub,
      appType: AppType.CROMWELL,
      workspaceNamespace,
      initialPanelContent: GKEAppPanelContent.DELETE_GKE_APP,
    });

    await waitFor(() => {
      expect(screen.queryByText(confirmDeleteGkeAppWithPdText)).not.toBeNull();
    });

    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    expectButtonElementEnabled(cancelButton);
    cancelButton.click();

    await waitFor(() => {
      expect(onCloseStub).toHaveBeenCalled();
    });
  });

  it('should call the delete PD api after confirming deleting PD', async () => {
    const deleteUnattachedPDStub = jest
      .spyOn(disksApi(), 'deleteDisk')
      .mockImplementation((): Promise<any> => Promise.resolve());
    const onCloseStub = jest.fn();

    // Setup: A Cromwell disk exists and the current app is Cromwell
    const workspaceNamespace = 'aou-rw-1234';
    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));
    createWrapper({
      onClose: onCloseStub,
      appType: AppType.CROMWELL,
      workspaceNamespace,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
    });

    const deleteButton = screen.getByRole('button', {
      name: 'Delete Persistent Disk',
    });
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    await waitFor(() => {
      expect(screen.queryByText(deleteUnattachedPdRegex)).not.toBeNull();
    });

    const deletePDRadioButton = screen.getByRole('radio', {
      name: 'Delete persistent disk',
    });
    deletePDRadioButton.click();

    const confirmDeleteButton = screen.getByRole('button', { name: 'Delete' });
    expectButtonElementEnabled(confirmDeleteButton);
    confirmDeleteButton.click();

    await waitFor(() => {
      expect(deleteUnattachedPDStub).toHaveBeenCalledWith(
        workspaceNamespace,
        disk.name
      );
      expect(onCloseStub).toHaveBeenCalled();

      // Validate there is no error modal if the call succeeds
      expect(notificationStore.get()).toBeNull();
    });
  });

  it('should show an error modal if the delete PD API call fails', async () => {
    jest
      .spyOn(disksApi(), 'deleteDisk')
      .mockImplementation((): Promise<any> => Promise.reject());

    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));

    createWrapper({
      appType: AppType.CROMWELL,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
      expect(notificationStore.get()).toBeNull(); // no errors
    });

    const deleteButton = screen.getByRole('button', {
      name: 'Delete Persistent Disk',
    });
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    await waitFor(() => {
      expect(screen.queryByText(deleteUnattachedPdRegex)).not.toBeNull();
    });

    const deletePDRadioButton = screen.getByRole('radio', {
      name: 'Delete persistent disk',
    });
    deletePDRadioButton.click();

    const confirmDeleteButton = screen.getByRole('button', { name: 'Delete' });
    expectButtonElementEnabled(confirmDeleteButton);
    confirmDeleteButton.click();

    await waitFor(() => {
      expect(notificationStore.get().title).toBeTruthy();
      expect(notificationStore.get().message).toBeTruthy();
    });
  });

  it('should change panels from DELETE_UNATTACHED_PD to CREATE after cancelling delete PD', async () => {
    // Setup: A Cromwell disk exists and the current app is Cromwell
    const workspaceNamespace = 'aou-rw-1234';
    const disk = stubDisk();
    disk.appType = AppType.CROMWELL;
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([disk]));
    createWrapper({
      appType: AppType.CROMWELL,
      workspaceNamespace,
    });

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
    });

    const deleteButton = screen.getByRole('button', {
      name: 'Delete Persistent Disk',
    });
    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    await waitFor(() => {
      expect(screen.queryByText(deleteUnattachedPdRegex)).not.toBeNull();
    });

    const deletePDRadioButton = screen.getByRole('radio', {
      name: 'Delete persistent disk',
    });
    deletePDRadioButton.click();

    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    expectButtonElementEnabled(cancelButton);
    cancelButton.click();

    await waitFor(() => {
      expect(screen.queryByText(cromwellIntroTextRegex)).not.toBeNull();
      expect(screen.queryByText(deleteUnattachedPdRegex)).toBeNull();
    });
  });
});
