import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { mount, ReactWrapper } from 'enzyme';

import { AppsApi, AppType, DisksApi } from 'generated/fetch';

import { CromwellConfigurationPanel } from 'app/components/cromwell-configuration-panel';
import {
  GKEAppConfigurationPanel,
  GkeAppConfigurationPanelProps,
} from 'app/components/gke-app-configuration-panel';
import { RStudioConfigurationPanel } from 'app/components/rstudio-configuration-panel';
import { DEFAULT_PROPS as RSTUDIO_DEFAULT_PROPS } from 'app/components/rstudio-configuration-panel.spec';
import { ConfirmDeleteUnattachedPD } from 'app/components/runtime-configuration-panel/confirm-delete-unattached-pd';
import { Spinner } from 'app/components/spinners';
import {
  appsApi,
  disksApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { notificationStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';

async function validateInitialLoadingSpinner(wrapper: ReactWrapper) {
  expect(wrapper.childAt(0).is(Spinner)).toBeTruthy();

  await waitOneTickAndUpdate(wrapper);

  expect(wrapper.childAt(0).is(Spinner)).toBeFalsy();
}

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

  const DEFAULT_PROPS: GkeAppConfigurationPanelProps = {
    type: AppType.RSTUDIO,
    workspaceNamespace: 'aou-rw-1234',
    onClose: jest.fn(),

    // Use RSTUDIO_DEFAULT_PROPS for the rest of the props since they shouldn't affect this test
    creatorFreeCreditsRemaining:
      RSTUDIO_DEFAULT_PROPS.creatorFreeCreditsRemaining,
    workspace: RSTUDIO_DEFAULT_PROPS.workspace,
    profileState: RSTUDIO_DEFAULT_PROPS.profileState,
  };

  const createWrapper = (
    propOverrides: Partial<GkeAppConfigurationPanelProps> = {}
  ) => {
    const props = {
      ...DEFAULT_PROPS,
      ...propOverrides,
    };
    return mount(<GKEAppConfigurationPanel {...props} />);
  };

  it('should show a loading spinner while waiting for the list apps API call to return', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    await validateInitialLoadingSpinner(createWrapper());
  });

  it('should show a loading spinner while waiting for the list disks API call to return', async () => {
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    await validateInitialLoadingSpinner(createWrapper());
  });

  it('should show an error if the list apps API call fails', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.reject());

    expect(notificationStore.get()).toBeNull();

    const wrapper = createWrapper();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get().title).toBeTruthy();
    expect(notificationStore.get().message).toBeTruthy();
  });

  it('should show an error if the list disks API call fails', async () => {
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.reject());

    expect(notificationStore.get()).toBeNull();

    const wrapper = createWrapper();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get().title).toBeTruthy();
    expect(notificationStore.get().message).toBeTruthy();
  });

  it('should not show an error if both fetch API calls succeed', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));

    expect(notificationStore.get()).toBeNull();

    const wrapper = createWrapper();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get()).toBeNull();
  });

  it('should pass the user apps to the specific app component when the API call succeeds', async () => {
    const workspaceNamespace = 'aou-rw-1234';
    const apps = [createListAppsCromwellResponse()];
    const listAppsStub = jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve(apps));

    const wrapper = createWrapper({
      workspaceNamespace,
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    expect(listAppsStub).toHaveBeenCalledWith(workspaceNamespace);
    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    expect(cromwellPanel.prop('gkeAppsInWorkspace')).toEqual(apps);
  });

  it('should pass the relevant disk to the specific app if available', async () => {
    const workspaceNamespace = 'aou-rw-1234';
    const cromwellDisk = stubDisk();
    cromwellDisk.appType = AppType.CROMWELL;
    const rstudioDisk = stubDisk();
    rstudioDisk.appType = AppType.RSTUDIO;
    const listDisksStub = jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation(
        (): Promise<any> => Promise.resolve([cromwellDisk, rstudioDisk])
      );

    const wrapper = createWrapper({
      workspaceNamespace,
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    expect(listDisksStub).toHaveBeenCalledWith(workspaceNamespace);
    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    expect(cromwellPanel.prop('disk')).toEqual(cromwellDisk);
  });

  it('should pass no disk to the specific app component if no relevant disk is available', async () => {
    const workspaceNamespace = 'aou-rw-1234';
    const rstudioDisk = stubDisk();
    rstudioDisk.appType = AppType.RSTUDIO;
    const listDisksStub = jest
      .spyOn(disksApi(), 'listOwnedDisksInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([rstudioDisk]));

    const wrapper = createWrapper({
      workspaceNamespace,
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    expect(listDisksStub).toHaveBeenCalledWith(workspaceNamespace);
    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    expect(cromwellPanel.prop('disk')).toEqual(undefined);
  });

  it('should display the Cromwell panel when the type is Cromwell', async () => {
    const wrapper = createWrapper({
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    // check that an arbitrary CromwellConfigurationPanelProps prop is passed through
    expect(cromwellPanel.prop('onClose')).toEqual(DEFAULT_PROPS.onClose);
  });

  it('should display the RSudio panel when the type is RStudio', async () => {
    const wrapper = createWrapper({
      type: AppType.RSTUDIO,
    });
    await waitOneTickAndUpdate(wrapper);

    const rstudioPanel = wrapper.find(RStudioConfigurationPanel);
    expect(rstudioPanel.exists()).toBeTruthy();
    // check that an arbitrary RStudioConfigurationPanelProps prop is passed through
    expect(rstudioPanel.prop('onClose')).toEqual(DEFAULT_PROPS.onClose);
  });

  it('should change panels from CREATE to DELETE_UNATTACHED_PD after clicking the delete PD button', async () => {
    const wrapper = createWrapper({
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    expect(wrapper.find(ConfirmDeleteUnattachedPD).exists()).toBeFalsy();

    act(() => {
      cromwellPanel.prop('onClickDeleteUnattachedPersistentDisk')();
    });
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find(CromwellConfigurationPanel).exists()).toBeFalsy();
    expect(wrapper.find(ConfirmDeleteUnattachedPD).exists()).toBeTruthy();
  });

  it('should call the delete api after confirming deleting PD', async () => {
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
    const wrapper = createWrapper({
      onClose: onCloseStub,
      type: AppType.CROMWELL,
      workspaceNamespace,
    });
    await waitOneTickAndUpdate(wrapper);
    // Start with the DELETE_UNATTACHED_PD panel
    act(() => {
      wrapper
        .find(CromwellConfigurationPanel)
        .prop('onClickDeleteUnattachedPersistentDisk')();
    });
    await waitOneTickAndUpdate(wrapper);

    wrapper.find(ConfirmDeleteUnattachedPD).prop('onConfirm')();
    await waitOneTickAndUpdate(wrapper);

    expect(deleteUnattachedPDStub).toHaveBeenCalledWith(
      workspaceNamespace,
      disk.name
    );
    expect(onCloseStub).toHaveBeenCalled();

    // Validate there is no error modal if the call succeeds
    expect(notificationStore.get()).toBeNull();
  });

  it('should show an error modal if the delete PD API call fails', async () => {
    jest
      .spyOn(disksApi(), 'deleteDisk')
      .mockImplementation((): Promise<any> => Promise.reject());

    const wrapper = createWrapper({
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);
    // Start with the DELETE_UNATTACHED_PD panel
    act(() => {
      wrapper
        .find(CromwellConfigurationPanel)
        .prop('onClickDeleteUnattachedPersistentDisk')();
    });
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get()).toBeNull();

    wrapper.find(ConfirmDeleteUnattachedPD).prop('onConfirm')();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get().title).toBeTruthy();
    expect(notificationStore.get().message).toBeTruthy();
  });

  it('should change panels from DELETE_UNATTACHED_PD to CREATE after cancelling delete PD', async () => {
    const wrapper = createWrapper({
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);
    // Start with the DELETE_UNATTACHED_PD panel
    act(() => {
      wrapper
        .find(CromwellConfigurationPanel)
        .prop('onClickDeleteUnattachedPersistentDisk')();
    });
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find(CromwellConfigurationPanel).exists()).toBeFalsy();
    const confirmDeleteUnattachedPDPanel = wrapper.find(
      ConfirmDeleteUnattachedPD
    );
    expect(confirmDeleteUnattachedPDPanel.exists()).toBeTruthy();

    act(() => {
      confirmDeleteUnattachedPDPanel.prop('onCancel')();
    });
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find(CromwellConfigurationPanel).exists()).toBeTruthy();
    expect(wrapper.find(ConfirmDeleteUnattachedPD).exists()).toBeFalsy();
  });
});
