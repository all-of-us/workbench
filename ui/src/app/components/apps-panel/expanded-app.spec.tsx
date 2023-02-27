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
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient as leoRegisterApiClient } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { AppsApi as LeoAppsApi, RuntimesApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { RuntimesApiStub } from 'testing/stubs/runtimes-api-stub';
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
    leoRegisterApiClient(LeoAppsApi, new LeoAppsApiStub());
    leoRegisterApiClient(RuntimesApi, new RuntimesApiStub());
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

  // test.each([
  //   ['should', RuntimeStatus.Running, true],
  //   // ['should', RuntimeStatus.Stopped, true],
  //   // ['should not', RuntimeStatus.Stopping, false],
  //   // ['should not', RuntimeStatus.Starting, false],
  // ])(
  //   '%s allow pausing when the Jupyter app status is %s',
  //   async (shouldOrNot, status, expectedCanPause) => {
  //     runtimeStub.runtime.status = status;
  //
  //     const wrapper = await component(UIAppType.JUPYTER, undefined);
  //     expect(wrapper.exists()).toBeTruthy();
  //
  //     const pauseButton = wrapper.find({ 'data-test-id': 'pause-resume-Jupyter' });
  //     expect(pauseButton.exists()).toBeTruthy();
  //     const cursorStyle = pauseButton.prop('style').cursor;
  //
  //     if (expectedCanPause) {
  //       expect(cursorStyle).not.toEqual('not-allowed');
  //
  //       const pauseSpy = jest.spyOn(leoRuntimesApi(), 'stopRuntime');
  //       const { onClick } = pauseButton.props();
  //       await onClick();
  //       await waitOneTickAndUpdate(wrapper);
  //
  //       expect(pauseSpy).toHaveBeenCalledWith(
  //         workspace.googleProject,
  //         runtimeStub.runtime.runtimeName
  //       );
  //     } else {
  //       expect(cursorStyle).toEqual('not-allowed');
  //     }
  //   }
  // );

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
      const appName = 'my-cromwell';
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

  test.each([
    ['should', 'pausing', AppStatus.RUNNING, true, false],
    ['should', 'resuming', AppStatus.STOPPED, false, true],
  ])(
    '%s allow %s when the Cromwell app status is %s',
    async (
      shouldOrNot,
      action,
      status,
      expectedCanPause,
      expectedCanResume
    ) => {
      const appName = 'my-cromwell';

      const wrapper = await component(UIAppType.CROMWELL, {
        appName,
        status,
        appType: AppType.CROMWELL,
      });
      expect(wrapper.exists()).toBeTruthy();

      const pauseButton = wrapper.find({
        'data-test-id': 'pause-resume-CROMWELL',
      });
      expect(pauseButton.exists()).toBeTruthy();
      const cursorStyle = pauseButton.prop('style').cursor;

      if (expectedCanPause) {
        expect(cursorStyle).not.toEqual('not-allowed');

        const pauseSpy = jest.spyOn(leoAppsApi(), 'stopApp');
        const { onClick } = pauseButton.props();
        await onClick();

        expect(pauseSpy).toHaveBeenCalledWith(workspace.googleProject, appName);
      } else if (expectedCanResume) {
        expect(cursorStyle).not.toEqual('not-allowed');

        const resumeSpy = jest.spyOn(leoAppsApi(), 'startApp');
        const { onClick } = pauseButton.props();
        await onClick();

        expect(resumeSpy).toHaveBeenCalledWith(
          workspace.googleProject,
          appName
        );
      } else {
        expect(cursorStyle).toEqual('not-allowed');
      }
    }
  );
});
