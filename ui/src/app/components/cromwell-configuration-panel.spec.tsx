import * as React from 'react';

import {
  AppStatus,
  DisksApi,
  ProfileApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';
import { AppsApi } from 'generated/fetch/api';

import {
  appsApi,
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

import { defaultCromwellConfig } from './apps-panel/utils';
import { CromwellConfigurationPanel } from './cromwell-configuration-panel';

describe('CromwellConfigurationPanel', () => {
  const onClose = jest.fn();
  const freeTierBillingAccountId = 'freetier';
  const DEFAULT_PROPS = {
    onClose,
  };

  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;

  const component = async (propOverrides?: object) => {
    const allProps = { ...DEFAULT_PROPS, ...propOverrides };
    const c = mountWithRouter(<CromwellConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
  };

  beforeEach(async () => {
    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName: 'billingAccounts/' + freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });

    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        freeTierBillingAccountId: freeTierBillingAccountId,
        defaultFreeCreditsDollarLimit: 100.0,
        gsuiteDomain: '',
      },
    });

    registerApiClient(AppsApi, new AppsApiStub());
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('should show configuration panel while not loading', async () => {
    const wrapper = await component();
    expect(wrapper.exists('#cromwell-configuration-panel')).toEqual(true);
    expect(wrapper.exists('cromwell-configuration-panel-spinner')).toEqual(
      false
    );
  });

  it('start button should create cromwell and close panel', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementationOnce(() => Promise.resolve([]));
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);

    const spyCreateApp = jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());
    const startButton = wrapper
      .find('#cromwell-cloud-environment-create-button')
      .first();

    startButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spyCreateApp).toHaveBeenCalledTimes(1);
    expect(spyCreateApp).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      defaultCromwellConfig
    );
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  const createEnabledStatuses = [AppStatus.DELETED, null, undefined];
  const createDisabledStatuses = minus(
    ALL_GKE_APP_STATUSES,
    createEnabledStatuses
  );

  describe('should allow creating a Cromwell app for certain app statuses', () => {
    test.each(createEnabledStatuses)('Status %s', async (appStatus) => {
      jest
        .spyOn(appsApi(), 'listAppsInWorkspace')
        .mockImplementationOnce(() =>
          Promise.resolve([
            createListAppsCromwellResponse({ status: appStatus }),
          ])
        );
      const wrapper = await component();
      expect(
        wrapper
          .find('#cromwell-cloud-environment-create-button')
          .first()
          .prop('disabled')
      ).toBeFalsy();
    });
  });

  describe('should allow creating a Cromwell app for certain app statuses', () => {
    test.each(createDisabledStatuses)('Status %s', async (appStatus) => {
      jest
        .spyOn(appsApi(), 'listAppsInWorkspace')
        .mockImplementationOnce(() =>
          Promise.resolve([
            createListAppsCromwellResponse({ status: appStatus }),
          ])
        );
      const wrapper = await component();
      expect(
        wrapper
          .find('#cromwell-cloud-environment-create-button')
          .first()
          .prop('disabled')
      ).toBeTruthy();
    });
  });
});
