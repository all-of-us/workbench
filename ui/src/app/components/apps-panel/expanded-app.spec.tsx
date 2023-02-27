import * as React from 'react';
import { mount } from 'enzyme';

import {
  AppsApi,
  AppStatus,
  AppType,
  NotebooksApi,
  RuntimeStatus,
  UserAppEnvironment,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ExpandedApp } from './expanded-app';
import { UIAppType } from './utils';

const workspace = workspaceDataStub;
const onClickRuntimeConf = jest.fn();
const onClickDeleteRuntime = jest.fn();

const component = async (
  appType: UIAppType,
  initialUserAppInfo: UserAppEnvironment
) =>
  mount(
    <ExpandedApp
      {...{
        appType,
        initialUserAppInfo,
        workspace,
        onClickRuntimeConf,
        onClickDeleteRuntime,
      }}
    />
  );

describe('ExpandedApp', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    environment.showAppsPanel = true;
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    // registerApiClient(LeoAppsApi, new LeoAppsApiStub());
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
  });

  test.each([
    ['should', RuntimeStatus.Running, true],
    ['should', RuntimeStatus.Stopped, true],
    ['should not', RuntimeStatus.Stopping, false],
    ['should not', RuntimeStatus.Starting, false],
  ])(
    '%s allow deletion when the Jupyter app status is %s',
    async (shouldOrNot, status, expectedCanDelete) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper.find({ 'data-test-id': 'delete-Jupyter' });
      expect(deletion.exists()).toBeTruthy();
      const cursorStyle = deletion.prop('style').cursor;

      if (expectedCanDelete) {
        expect(cursorStyle).not.toEqual('not-allowed');

        const { onClick } = deletion.props();
        await onClick();

        expect(onClickDeleteRuntime).toHaveBeenCalled();
      } else {
        expect(cursorStyle).toEqual('not-allowed');
      }
    }
  );

  test.each([
    ['should', AppStatus.RUNNING, true],
    ['should', AppStatus.ERROR, true],
    ['should', AppStatus.STATUSUNSPECIFIED, true],
    ['should not', AppStatus.STOPPED, false],
    ['should not', AppStatus.STARTING, false],
    ['should not', AppStatus.STOPPING, false],
  ])(
    '%s allow deletion when the Cromwell app status is %s',
    async (shouldOrNot, status, expectedCanDelete) => {
      const appName = 'my-running-cromwell';
      const deleteDiskWithUserApp = true; // always true currently

      const wrapper = await component(UIAppType.CROMWELL, {
        appName,
        status,
        appType: AppType.CROMWELL,
      });
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper.find({ 'data-test-id': 'delete-Cromwell' });
      expect(deletion.exists()).toBeTruthy();
      const cursorStyle = deletion.prop('style').cursor;

      if (expectedCanDelete) {
        expect(cursorStyle).not.toEqual('not-allowed');

        const deleteSpy = jest.spyOn(appsApi(), 'deleteApp');
        const { onClick } = deletion.props();
        await onClick();

        expect(deleteSpy).toHaveBeenCalledWith(
          workspace.namespace,
          appName,
          deleteDiskWithUserApp
        );
      } else {
        expect(cursorStyle).toEqual('not-allowed');
      }
    }
  );
});
