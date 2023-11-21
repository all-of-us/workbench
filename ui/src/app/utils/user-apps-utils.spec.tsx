import { mockNavigate } from 'setupTests';

import {
  AppsApi,
  AppStatus,
  AppType,
  CreateAppRequest,
  DiskType,
  ListAppsResponse,
  RuntimeStatus,
} from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { setSidebarActiveIconStore } from 'app/utils/navigation';

import { AppsApiStub } from 'testing/stubs/apps-api-stub';

import { userAppsStore } from './stores';
import * as userAppsUtils from './user-apps-utils';

const fakeCromwellConfig: CreateAppRequest = {
  appType: AppType.CROMWELL,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: 'Fake Machine Name',
    autoscalingEnabled: true,
  },
  persistentDiskRequest: {
    size: 50000,
    diskType: DiskType.SSD,
  },
};

describe('User Apps Helper functions', () => {
  let appsApiStub: AppsApiStub;
  beforeEach(async () => {
    jest.useFakeTimers();
    appsApiStub = new AppsApiStub();
    registerApiClient(AppsApi, appsApiStub);
    userAppsStore.set({ updating: false });
    setSidebarActiveIconStore.next(null);
  });

  afterEach(async () => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('Create User App without an existing update process', async () => {
    jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementationOnce(() => Promise.resolve({}));
    const spyListAppsAPI = jest.spyOn(appsApi(), 'listAppsInWorkspace');
    await userAppsUtils.createUserApp('fakeNameSpace', fakeCromwellConfig);
    expect(spyListAppsAPI).toHaveBeenCalledTimes(1);
  });

  it('Create User App with an existing update process', async () => {
    userAppsStore.set({ updating: true });
    jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementationOnce(() => Promise.resolve({}));
    const spyListAppsAPI = jest.spyOn(appsApi(), 'listAppsInWorkspace');
    await userAppsUtils.createUserApp('fakeNameSpace', fakeCromwellConfig);
    expect(spyListAppsAPI).toHaveBeenCalledTimes(0);
  });

  it('Update User Apps with an existing update process', async () => {
    userAppsStore.set({ updating: true });
    const spyListAppsAPI = jest.spyOn(appsApi(), 'listAppsInWorkspace');
    await userAppsUtils.maybeStartPollingForUserApps('fakeNameSpace');
    expect(spyListAppsAPI).toHaveBeenCalledTimes(0);
  });

  it('Update User Apps that does not require a subsequent update', async () => {
    const spyListAppsAPI = jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation(
        () =>
          new Promise<ListAppsResponse>(() => [
            { status: RuntimeStatus.RUNNING, appType: AppType.CROMWELL },
          ])
      );
    await userAppsUtils.maybeStartPollingForUserApps('fakeNameSpace');
    expect(spyListAppsAPI).toHaveBeenCalledTimes(1);
  });

  it('Update User Apps that requires a subsequent update', async () => {
    const spyListAppsAPI = jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementationOnce(() =>
        Promise.resolve([
          { status: AppStatus.STARTING, appType: AppType.CROMWELL },
        ])
      )
      .mockImplementationOnce(() =>
        Promise.resolve([
          { status: AppStatus.RUNNING, appType: AppType.CROMWELL },
        ])
      );
    await userAppsUtils.maybeStartPollingForUserApps('fakeNameSpace');

    jest.advanceTimersByTime(20e3);
    expect(spyListAppsAPI).toHaveBeenCalledTimes(2);
  });

  it('Opens Config panel if RStudio App is not running', async () => {
    const navigate = mockNavigate;
    expect(setSidebarActiveIconStore.value).toBeNull();

    userAppsUtils.openRStudioOrConfigPanel(
      'ws',
      'wsid',
      [{ status: AppStatus.STARTING, appType: AppType.RSTUDIO }],
      'newFile',
      navigate
    );
    expect(mockNavigate).not.toBeCalled();
    // Since RStudio is NOT in running state this will open the RStudio config side panel and
    // There will be no navigation to any other page
    expect(setSidebarActiveIconStore.value).toBe(rstudioConfigIconId);
  });
  it('Will not open Config panel if RStudio App is running', async () => {
    const navigate = mockNavigate;
    expect(setSidebarActiveIconStore.value).toBeNull();

    userAppsUtils.openRStudioOrConfigPanel(
      'ws',
      'wsid',
      [{ status: AppStatus.RUNNING, appType: AppType.RSTUDIO }],
      'newFile',
      navigate
    );
    // Since RStudio is running, navigate to open RStudio in iframe and do not open config panel
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      'ws',
      'wsid',
      UIAppType.RSTUDIO,
      'newFile',
    ]);
    expect(setSidebarActiveIconStore.value).toBeNull();
  });
});
