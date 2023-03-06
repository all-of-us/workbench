import * as React from 'react';
import { mount } from 'enzyme';

import {
  AppsApi,
  AppStatus,
  NotebooksApi,
  RuntimeStatus,
  UserAppEnvironment,
} from 'generated/fetch';

import {
  leoAppsApi,
  leoRuntimesApi,
  registerApiClient as leoRegisterApiClient,
} from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { AppsApi as LeoAppsApi, RuntimesApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { RuntimesApiStub } from 'testing/stubs/runtimes-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ExpandedApp } from './expanded-app';
import { UIAppType } from './utils';

const googleProject = 'project-for-test';
const workspace = {
  ...workspaceDataStub,
  googleProject,
};
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
  runtimeStub.runtime.googleProject = googleProject;
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
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

  it('should allow pausing when the Jupyter app is Running', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component(UIAppType.JUPYTER, undefined);
    expect(wrapper.exists()).toBeTruthy();

    const pauseButton = wrapper.find({
      'data-test-id': 'apps-panel-button-Pause',
    });
    expect(pauseButton.exists()).toBeTruthy();
    const { disabled } = pauseButton.props();
    expect(disabled).toBeFalsy();

    const pauseSpy = jest.spyOn(leoRuntimesApi(), 'stopRuntime');
    const { onClick } = pauseButton.props();
    await onClick();
    await waitOneTickAndUpdate(wrapper);

    expect(pauseSpy).toHaveBeenCalledWith(
      workspace.googleProject,
      runtimeStub.runtime.runtimeName
    );
  });

  it('should allow resuming when the Jupyter app is Stopped', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Stopped;

    const wrapper = await component(UIAppType.JUPYTER, undefined);
    expect(wrapper.exists()).toBeTruthy();

    const pauseButton = wrapper.find({
      'data-test-id': 'apps-panel-button-Resume',
    });
    expect(pauseButton.exists()).toBeTruthy();
    const { disabled } = pauseButton.props();
    expect(disabled).toBeFalsy();

    const resumeSpy = jest.spyOn(leoRuntimesApi(), 'startRuntime');
    const { onClick } = pauseButton.props();
    await onClick();
    await waitOneTickAndUpdate(wrapper);

    expect(resumeSpy).toHaveBeenCalledWith(
      workspace.googleProject,
      runtimeStub.runtime.runtimeName
    );
  });

  test.each([
    [RuntimeStatus.Stopping, 'Pausing'],
    [RuntimeStatus.Starting, 'Resuming'],

    [RuntimeStatus.Deleted, 'Pause'],
    [RuntimeStatus.Deleting, 'Pause'],
    [RuntimeStatus.Unknown, 'Pause'],
    [RuntimeStatus.Error, 'Pause'],
    [RuntimeStatus.Creating, 'Pause'],
    [RuntimeStatus.Updating, 'Pause'],

    [undefined, 'Pause'],
    [null, 'Pause'],
  ])(
    'should not allow clicking pause/resume when the Jupyter app status is %s',
    async (status, buttonText) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const pauseButton = wrapper.find({
        'data-test-id': `apps-panel-button-${buttonText}`,
      });
      expect(pauseButton.exists()).toBeTruthy();
      const { disabled } = pauseButton.props();
      expect(disabled).toBeTruthy();
    }
  );

  test.each([RuntimeStatus.Running, RuntimeStatus.Stopped])(
    'should allow deletion when the Jupyter app status is %s',
    async (status) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper.find({
        'data-test-id': 'Jupyter-delete-button',
      });
      expect(deletion.exists()).toBeTruthy();
      const { disabled } = deletion.props();
      expect(disabled).toBeFalsy();

      const { onClick } = deletion.props();
      await onClick();

      expect(onClickDeleteRuntime).toHaveBeenCalled();
    }
  );

  test.each([
    RuntimeStatus.Stopping,
    RuntimeStatus.Starting,
    RuntimeStatus.Error,
    RuntimeStatus.Unknown,
    undefined,
    null,
  ])(
    'should not allow deletion when the Jupyter app status is %s',
    async (status) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper.find({
        'data-test-id': 'Jupyter-delete-button',
      });
      expect(deletion.exists()).toBeTruthy();
      const { disabled } = deletion.props();
      expect(disabled).toBeTruthy();
    }
  );

  const gkeAppTypes = [UIAppType.CROMWELL, UIAppType.RSTUDIO];
  describe.each(gkeAppTypes)('GKE App %s', (appType) => {
    it('should allow clicking Pause when the app status is RUNNING', async () => {
      const appName = 'my-app';

      const wrapper = await component(appType, {
        appName,
        googleProject,
        status: AppStatus.RUNNING,
      });
      expect(wrapper.exists()).toBeTruthy();

      const pauseButton = wrapper.find({
        'data-test-id': 'apps-panel-button-Pause',
      });
      expect(pauseButton.exists()).toBeTruthy();
      const { disabled } = pauseButton.props();
      expect(disabled).toBeFalsy();

      const pauseSpy = jest.spyOn(leoAppsApi(), 'stopApp');
      const { onClick } = pauseButton.props();
      await onClick();

      expect(pauseSpy).toHaveBeenCalledWith(workspace.googleProject, appName);
    });

    it('should allow clicking Resume when the app status is STOPPED', async () => {
      const appName = 'my-app';

      const wrapper = await component(appType, {
        appName,
        googleProject,
        status: AppStatus.STOPPED,
      });
      expect(wrapper.exists()).toBeTruthy();

      const pauseButton = wrapper.find({
        'data-test-id': 'apps-panel-button-Resume',
      });
      expect(pauseButton.exists()).toBeTruthy();
      const { disabled } = pauseButton.props();
      expect(disabled).toBeFalsy();

      const resumeSpy = jest.spyOn(leoAppsApi(), 'startApp');
      const { onClick } = pauseButton.props();
      await onClick();

      expect(resumeSpy).toHaveBeenCalledWith(workspace.googleProject, appName);
    });

    test.each([
      [AppStatus.STARTING, 'Resuming'],
      [AppStatus.STOPPING, 'Pausing'],

      [AppStatus.DELETED, 'Pause'],
      [AppStatus.DELETING, 'Pause'],
      [AppStatus.ERROR, 'Pause'],
      [AppStatus.PROVISIONING, 'Pause'],
      [AppStatus.STATUSUNSPECIFIED, 'Pause'],

      [undefined, 'Pause'],
      [null, 'Pause'],
    ])(
      'should not allow clicking pause/resume when the app status is %s',
      async (status, buttonText) => {
        const appName = 'my-app';

        const wrapper = await component(appType, {
          appName,
          googleProject,
          status,
        });
        expect(wrapper.exists()).toBeTruthy();

        const pauseButton = wrapper.find({
          'data-test-id': `apps-panel-button-${buttonText}`,
        });
        expect(pauseButton.exists()).toBeTruthy();
        const { disabled } = pauseButton.props();
        expect(disabled).toBeTruthy();
      }
    );

    test.each([
      AppStatus.RUNNING,
      AppStatus.ERROR,
      AppStatus.STATUSUNSPECIFIED,
    ])('should allow deletion when the app status is %s', async (status) => {
      const appName = 'my-app';
      const deleteDiskWithUserApp = true; // always true currently

      const wrapper = await component(appType, {
        appName,
        status,
      });
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper.find({
        'data-test-id': `${appType}-delete-button`,
      });
      expect(deletion.exists()).toBeTruthy();
      const { disabled } = deletion.props();
      expect(disabled).toBeFalsy();

      const deleteSpy = jest.spyOn(appsApi(), 'deleteApp');
      const { onClick } = deletion.props();
      await onClick();

      expect(deleteSpy).toHaveBeenCalledWith(
        workspace.namespace,
        appName,
        deleteDiskWithUserApp
      );
    });

    test.each([
      AppStatus.STOPPED,
      AppStatus.STARTING,
      AppStatus.STOPPING,
      undefined,
      null,
    ])(
      'should not allow deletion when the app status is %s',
      async (status) => {
        const appName = 'my-app';

        const wrapper = await component(appType, {
          appName,
          status,
        });
        expect(wrapper.exists()).toBeTruthy();

        const deletion = wrapper.find({
          'data-test-id': `${appType}-delete-button`,
        });
        expect(deletion.exists()).toBeTruthy();
        const { disabled } = deletion.props();
        expect(disabled).toBeTruthy();
      }
    );
  });
});
