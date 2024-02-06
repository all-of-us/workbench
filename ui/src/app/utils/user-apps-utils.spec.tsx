import { mockNavigate } from 'setupTests';

import {
  AppsApi,
  AppStatus,
  AppType,
  CreateAppRequest,
  DiskType,
} from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import { appDisplayPath } from 'app/routing/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { sidebarActiveIconStore } from 'app/utils/navigation';

import { AppsApiStub } from 'testing/stubs/apps-api-stub';

import { getLastActiveEpochMillis, setLastActive } from './inactivity';
import { userAppsStore } from './stores';
import * as userAppsUtils from './user-apps-utils';
import { updateLastActive } from './user-apps-utils';

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
    sidebarActiveIconStore.next(null);
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
      .mockImplementation(() =>
        Promise.resolve([
          { status: AppStatus.RUNNING, appType: AppType.CROMWELL },
        ])
      );
    await userAppsUtils.maybeStartPollingForUserApps('fakeNameSpace');
    expect(spyListAppsAPI).toHaveBeenCalledTimes(1);

    // advance by 2x the transition polling timeout value
    jest.advanceTimersByTime(20e3);

    // it does not call list-apps again
    expect(spyListAppsAPI).toHaveBeenCalledTimes(1);

    // advance by the non-transitional polling timeout value
    jest.advanceTimersByTime(5 * 60e3);

    // now it calls list-apps again
    expect(spyListAppsAPI).toHaveBeenCalledTimes(2);
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
    expect(spyListAppsAPI).toHaveBeenCalledTimes(1);

    // advance by 2x the transition polling timeout value
    jest.advanceTimersByTime(20e3);

    // it calls list-apps again once but not twice, because we have transitioned
    expect(spyListAppsAPI).toHaveBeenCalledTimes(2);
  });

  it('Opens Config panel if RStudio App is not running', async () => {
    const navigate = mockNavigate;
    expect(sidebarActiveIconStore.value).toBeNull();

    userAppsUtils.openAppOrConfigPanel(
      'ws',
      'wsid',
      [{ status: AppStatus.STARTING, appType: AppType.RSTUDIO }],
      UIAppType.RSTUDIO,
      navigate
    );

    // Since RStudio is NOT in running state this will open the RStudio config side panel and
    // There will be no navigation to any other page
    expect(mockNavigate).not.toBeCalled();
    expect(sidebarActiveIconStore.value).toBe(rstudioConfigIconId);
  });

  it('Will open RStudio App in iframe if app is running', async () => {
    const navigate = mockNavigate;
    expect(sidebarActiveIconStore.value).toBeNull();

    userAppsUtils.openAppOrConfigPanel(
      'ws',
      'wsid',
      [{ status: AppStatus.RUNNING, appType: AppType.RSTUDIO }],
      UIAppType.RSTUDIO,
      navigate
    );
    // Since RStudio is running, navigate to open RStudio in iframe and do not open config panel
    expect(mockNavigate).toHaveBeenCalledWith([
      appDisplayPath('ws', 'wsid', UIAppType.RSTUDIO),
    ]);
    expect(sidebarActiveIconStore.value).toBeNull();
  });
});

describe(updateLastActive.name, () => {
  it('does nothing when there are no userApps', () => {
    const lastActiveInUI = 123;
    setLastActive(lastActiveInUI);
    updateLastActive([]);
    expect(getLastActiveEpochMillis()).toEqual(lastActiveInUI);
  });

  it('updates the last active value in local storage when local storage is empty', () => {
    const lastActiveInUserApp = 789654;
    updateLastActive([
      { dateAccessed: new Date(lastActiveInUserApp).toISOString() },
    ]);
    expect(getLastActiveEpochMillis()).toEqual(lastActiveInUserApp);
  });

  it('does nothing when local storage has recorded more recent activity than userApps', () => {
    const lastActiveInUI = 12345;
    setLastActive(lastActiveInUI);
    updateLastActive([
      { dateAccessed: new Date(10000).toISOString() },
      { dateAccessed: new Date(11000).toISOString() },
      { dateAccessed: new Date(12000).toISOString() },
    ]);
    expect(getLastActiveEpochMillis()).toEqual(lastActiveInUI);
  });

  it('updates the last active value in local storage when the userApps have more recent activity', () => {
    const lastActiveInUI = 12345;
    setLastActive(lastActiveInUI);
    updateLastActive([
      { dateAccessed: new Date(10000).toISOString() },
      { dateAccessed: new Date(13000).toISOString() },
    ]);
    expect(getLastActiveEpochMillis()).toBeGreaterThan(lastActiveInUI);
    expect(getLastActiveEpochMillis()).toEqual(13000);
  });
});
