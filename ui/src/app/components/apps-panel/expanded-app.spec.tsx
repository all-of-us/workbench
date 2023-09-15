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

import {
  cromwellConfigIconId,
  rstudioConfigIconId,
} from 'app/components/help-sidebar-icons';
import {
  leoRuntimesApi,
  registerApiClient as leoRegisterApiClient,
} from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import {
  AppsApi as LeoAppsApi,
  ProxyApi,
  RuntimesApi,
} from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { LeoProxyApiStub } from 'testing/stubs/leo-proxy-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { RuntimesApiStub } from 'testing/stubs/runtimes-api-stub';
import {
  workspaceDataStub,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

import { ExpandedApp } from './expanded-app';
import { UIAppType } from './utils';

const googleProject = 'project-for-test';
const workspace = {
  ...workspaceDataStub,
  googleProject,
};
const onClickRuntimeConf = jest.fn();
const onClickDeleteRuntime = jest.fn();
const onClickDeleteGkeApp = jest.fn();

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
        onClickDeleteGkeApp,
      }}
    />
  );

describe('ExpandedApp', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  const leoProxyApiStub = new LeoProxyApiStub();
  runtimeStub.runtime.googleProject = googleProject;
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    leoRegisterApiClient(LeoAppsApi, new LeoAppsApiStub());
    leoRegisterApiClient(ProxyApi, leoProxyApiStub);
    leoRegisterApiClient(RuntimesApi, new RuntimesApiStub());
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });

    window.open = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should allow pausing when the Jupyter app is Running', async () => {
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    const wrapper = await component(UIAppType.JUPYTER, undefined);
    expect(wrapper.exists()).toBeTruthy();

    const pauseButton = wrapper
      .find({
        'data-test-id': 'apps-panel-button-Pause',
      })
      .first();
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

    const pauseButton = wrapper
      .find({
        'data-test-id': 'apps-panel-button-Resume',
      })
      .first();
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
    [RuntimeStatus.DELETING, 'Pause'],
    [RuntimeStatus.Unknown, 'Pause'],
    [RuntimeStatus.Error, 'Pause'],
    [RuntimeStatus.CREATING, 'Pause'],
    [RuntimeStatus.Updating, 'Pause'],

    [undefined, 'Pause'],
    [null, 'Pause'],
  ])(
    'should not allow clicking pause/resume when the Jupyter app status is %s',
    async (status, buttonText) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const pauseButton = wrapper
        .find({
          'data-test-id': `apps-panel-button-${buttonText}`,
        })
        .first();
      expect(pauseButton.exists()).toBeTruthy();
      const { disabled } = pauseButton.props();
      expect(disabled).toBeTruthy();
    }
  );

  test.each([RuntimeStatus.RUNNING, RuntimeStatus.Stopped])(
    'should allow deletion when the Jupyter app status is %s',
    async (status) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component(UIAppType.JUPYTER, undefined);
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper
        .find({
          'data-test-id': 'Jupyter-delete-button',
        })
        .first();
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

      const deletion = wrapper
        .find({
          'data-test-id': 'Jupyter-delete-button',
        })
        .first();
      expect(deletion.exists()).toBeTruthy();
      const { disabled } = deletion.props();
      expect(disabled).toBeTruthy();
    }
  );

  const gkeAppTypes = [UIAppType.CROMWELL, UIAppType.RSTUDIO];
  describe.each(gkeAppTypes)('GKE App %s', (appType) => {
    test.each([
      [AppStatus.RUNNING, 'Pause'],
      [AppStatus.STOPPED, 'Resume'],

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

        const pauseButton = wrapper
          .find({
            'data-test-id': `apps-panel-button-${buttonText}`,
          })
          .first();
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

      const wrapper = await component(appType, {
        appName,
        status,
      });
      expect(wrapper.exists()).toBeTruthy();

      const deletion = wrapper
        .find({
          'data-test-id': `${appType}-delete-button`,
        })
        .first();
      expect(deletion.exists()).toBeTruthy();
      const { disabled } = deletion.props();
      expect(disabled).toBeFalsy();

      deletion.simulate('click');
      if (appType === UIAppType.CROMWELL) {
        /* For Cromwell, on delete we show user a modal asking them to confirm manually that there are
            no cromwell Jobs running. Only after user confirming YES we close the modal and start the delete process */
        let cromwell_delete_modal = wrapper.find({
          'data-test-id': 'delete-cromwell-modal',
        });

        expect(cromwell_delete_modal).toBeTruthy();
        const button_delete_cromwell = cromwell_delete_modal.find({
          'data-test-id': 'delete-cromwell-btn',
        });

        button_delete_cromwell.simulate('click');

        // Clicking button YES i.e confirming deletion of cromwell should close the modal
        cromwell_delete_modal = wrapper.find({
          'data-test-id': 'delete-cromwell-modal',
        });
        expect(cromwell_delete_modal.length).toBe(0);

        expect(onClickDeleteGkeApp).toHaveBeenCalledWith(cromwellConfigIconId);
      } else {
        expect(onClickDeleteGkeApp).toHaveBeenCalledWith(rstudioConfigIconId);
      }
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

        const deletion = wrapper
          .find({
            'data-test-id': `${appType}-delete-button`,
          })
          .first();
        expect(deletion.exists()).toBeTruthy();
        const { disabled } = deletion.props();
        expect(disabled).toBeTruthy();
      }
    );
  });

  it('should allow launching RStudio when the RStudio app status is RUNNING', async () => {
    const appName = 'my-app';
    const proxyUrl = 'https://example.com';
    const wrapper = await component(UIAppType.RSTUDIO, {
      appName,
      googleProject,
      appType: AppType.RSTUDIO,
      status: AppStatus.RUNNING,
      proxyUrls: {
        [GKE_APP_PROXY_PATH_SUFFIX]: proxyUrl,
      },
    });
    const localizeSpy = jest.spyOn(appsApi(), 'localizeApp');

    const focusStub = jest.fn();
    const windowOpenSpy = jest
      .spyOn(window, 'open')
      .mockReturnValue({ focus: focusStub } as any as Window);

    const launchButton = wrapper.find({
      'data-test-id': 'open-RStudio-button',
    });
    expect(launchButton.exists()).toBeTruthy();
    expect(launchButton.prop('disabled')).toBeFalsy();

    await launchButton.simulate('click');

    expect(localizeSpy).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      appName,
      { appType: 'RSTUDIO', fileNames: [], playgroundMode: false }
    );

    expect(windowOpenSpy).toHaveBeenCalledWith(proxyUrl, '_blank');
    expect(focusStub).toHaveBeenCalled();
  });

  describe('should disable the launch button when the RStudio app status is not RUNNING', () => {
    test.each(minus(ALL_GKE_APP_STATUSES, [AppStatus.RUNNING]))(
      'Status %s',
      async (appStatus) => {
        const wrapper = await component(UIAppType.RSTUDIO, {
          appName: 'my-app',
          googleProject,
          status: appStatus,
        });

        const launchButton = wrapper.find({
          'data-test-id': 'open-RStudio-button',
        });
        expect(launchButton.prop('disabled')).toBeTruthy();
      }
    );
  });
});
