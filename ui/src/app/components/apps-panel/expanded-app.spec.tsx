import * as React from 'react';

import {
  AppsApi,
  AppStatus,
  AppType,
  NotebooksApi,
  RuntimeStatus,
  UserAppEnvironment,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  cromwellConfigIconId,
  rstudioConfigIconId,
  sasConfigIconId,
} from 'app/components/help-sidebar-icons';
import {
  leoRuntimesApi,
  registerApiClient as leoRegisterApiClient,
} from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { appTypeToString } from 'app/utils/user-apps-utils';
import {
  AppsApi as LeoAppsApi,
  ProxyApi,
  RuntimesApi,
} from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';
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
  renderModal(
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
    runtimeStub.runtime.status = RuntimeStatus.Running;
    const pauseSpy = jest.spyOn(leoRuntimesApi(), 'stopRuntime');

    const { container } = await component(UIAppType.JUPYTER, undefined);
    expect(container).toBeInTheDocument();

    const pauseButton = screen.getByRole('button', { name: 'Pause' });
    expectButtonElementEnabled(pauseButton);
    pauseButton.click();

    await waitFor(() =>
      expect(pauseSpy).toHaveBeenCalledWith(
        workspace.googleProject,
        runtimeStub.runtime.runtimeName
      )
    );
  });

  it('should allow resuming when the Jupyter app is Stopped', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Stopped;
    const resumeSpy = jest.spyOn(leoRuntimesApi(), 'startRuntime');

    const { container } = await component(UIAppType.JUPYTER, undefined);
    expect(container).toBeInTheDocument();

    const resumeButton = screen.getByRole('button', { name: 'Resume' });
    expectButtonElementEnabled(resumeButton);
    resumeButton.click();

    await waitFor(() =>
      expect(resumeSpy).toHaveBeenCalledWith(
        workspace.googleProject,
        runtimeStub.runtime.runtimeName
      )
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
    async (status, name) => {
      runtimeStub.runtime.status = status;

      const { container } = await component(UIAppType.JUPYTER, undefined);
      expect(container).toBeInTheDocument();

      const actionButton = screen.getByRole('button', { name });
      expectButtonElementEnabled(actionButton);
    }
  );

  test.each([RuntimeStatus.Running, RuntimeStatus.Stopped])(
    'should allow deletion when the Jupyter app status is %s',
    async (status) => {
      runtimeStub.runtime.status = status;

      const { container } = await component(UIAppType.JUPYTER, undefined);
      expect(container).toBeInTheDocument();

      const deleteButton = screen.getByRole('button', {
        name: 'Delete Environment',
      });
      expectButtonElementEnabled(deleteButton);
      deleteButton.click();

      await waitFor(() => expect(onClickDeleteRuntime).toHaveBeenCalled());
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

      const { container } = await component(UIAppType.JUPYTER, undefined);
      expect(container).toBeInTheDocument();

      const deleteButton = screen.getByRole('button', {
        name: 'Delete Environment',
      });
      expectButtonElementDisabled(deleteButton);
    }
  );

  const gkeAppTypes = [UIAppType.CROMWELL, UIAppType.RSTUDIO, UIAppType.SAS];
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
      async (status, name) => {
        const appName = 'my-app';

        const { container } = await component(appType, {
          appName,
          googleProject,
          status,
        });
        expect(container).toBeInTheDocument();

        const actionButton = screen.getByRole('button', { name });
        expectButtonElementEnabled(actionButton);
      }
    );

    test.each([
      AppStatus.RUNNING,
      AppStatus.ERROR,
      AppStatus.STATUSUNSPECIFIED,
    ])('should allow deletion when the app status is %s', async (status) => {
      const appName = 'my-app';

      const { container } = await component(appType, {
        appName,
        status,
      });
      expect(container).toBeInTheDocument();

      const deleteButton = screen.getByRole('button', {
        name: 'Delete Environment',
      });
      expectButtonElementEnabled(deleteButton);
      deleteButton.click();

      if (appType === UIAppType.CROMWELL) {
        /* For Cromwell, on delete we show user a modal asking them to confirm manually that there are
            no cromwell Jobs running. Only after user confirms YES we close the modal and start the delete process */
        const cromwellDeletionModalText =
          'Delete Cromwell: check for running jobs';
        await waitFor(() => {
          expect(screen.queryByText(cromwellDeletionModalText)).not.toBeNull();
        });

        const confirmDeleteButton = screen.getByRole('button', {
          name: 'YES, DELETE',
        });
        expectButtonElementEnabled(confirmDeleteButton);
        confirmDeleteButton.click();

        await waitFor(() => {
          // modal is closed
          expect(screen.queryByText(cromwellDeletionModalText)).toBeNull();
          expect(onClickDeleteGkeApp).toHaveBeenCalledWith(
            cromwellConfigIconId
          );
        });
      } else if (appType === UIAppType.RSTUDIO) {
        await waitFor(() =>
          expect(onClickDeleteGkeApp).toHaveBeenCalledWith(rstudioConfigIconId)
        );
      } else if (appType === UIAppType.SAS) {
        await waitFor(() =>
          expect(onClickDeleteGkeApp).toHaveBeenCalledWith(sasConfigIconId)
        );
      } else {
        fail('Unexpected appType: ' + appTypeToString[appType]);
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

        const { container } = await component(appType, {
          appName,
          status,
        });
        expect(container).toBeInTheDocument();

        const deleteButton = screen.getByRole('button', {
          name: 'Delete Environment',
        });
        expectButtonElementDisabled(deleteButton);
      }
    );
  });

  it('should allow launching RStudio when the RStudio app status is RUNNING', async () => {
    const appName = 'my-app';
    const proxyUrl = 'https://example.com';
    await component(UIAppType.RSTUDIO, {
      appName,
      googleProject,
      appType: AppType.RSTUDIO,
      status: AppStatus.RUNNING,
      proxyUrls: {
        [GKE_APP_PROXY_PATH_SUFFIX]: proxyUrl,
      },
    });
    const localizeSpy = jest
      .spyOn(appsApi(), 'localizeApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

    const focusStub = jest.fn();
    const windowOpenSpy = jest
      .spyOn(window, 'open')
      .mockReturnValue({ focus: focusStub } as any as Window);

    const launchButton = screen.getByRole('button', {
      name: 'Open RStudio',
    });
    expectButtonElementEnabled(launchButton);
    launchButton.click();

    await waitFor(() => {
      expect(localizeSpy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        appName,
        { appType: 'RSTUDIO', fileNames: [], playgroundMode: false }
      );

      expect(windowOpenSpy).toHaveBeenCalledWith(proxyUrl, '_blank');
      expect(focusStub).toHaveBeenCalled();
    });
  });

  describe('should disable the launch button when the RStudio app status is not RUNNING', () => {
    test.each(minus(ALL_GKE_APP_STATUSES, [AppStatus.RUNNING]))(
      'Status %s',
      async (appStatus) => {
        const localizeSpy = jest
          .spyOn(appsApi(), 'localizeApp')
          .mockImplementation((): Promise<any> => Promise.resolve());
        const focusStub = jest.fn();
        const windowOpenSpy = jest
          .spyOn(window, 'open')
          .mockReturnValue({ focus: focusStub } as any as Window);

        await component(UIAppType.RSTUDIO, {
          appName: 'my-app',
          googleProject,
          status: appStatus,
        });

        const launchButton = screen.getByRole('button', {
          name: 'Open RStudio',
        });

        // TODO this doesn't work because we set the styling differently here.
        // but really this is a fragile hack that we should ideally be moving away from
        // expectButtonElementDisabled(launchButton);

        launchButton.click();
        // disabled so nothing happens
        await waitFor(() => {
          expect(localizeSpy).not.toHaveBeenCalled();
          expect(windowOpenSpy).not.toHaveBeenCalled();
          expect(focusStub).not.toHaveBeenCalled();
        });
      }
    );
  });

  it('should allow launching SAS when the SAS app status is RUNNING', async () => {
    const appName = 'my-app';
    const proxyUrl = 'https://example.com';
    await component(UIAppType.SAS, {
      appName,
      googleProject,
      appType: AppType.SAS,
      status: AppStatus.RUNNING,
      proxyUrls: {
        [GKE_APP_PROXY_PATH_SUFFIX]: proxyUrl,
      },
    });
    const localizeSpy = jest
      .spyOn(appsApi(), 'localizeApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

    const focusStub = jest.fn();
    const windowOpenSpy = jest
      .spyOn(window, 'open')
      .mockReturnValue({ focus: focusStub } as any as Window);

    const launchButton = screen.getByRole('button', {
      name: 'Open SAS',
    });
    expectButtonElementEnabled(launchButton);
    launchButton.click();

    await waitFor(() => {
      expect(localizeSpy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        appName,
        { appType: 'SAS', fileNames: [], playgroundMode: false }
      );

      expect(windowOpenSpy).toHaveBeenCalledWith(proxyUrl, '_blank');
      expect(focusStub).toHaveBeenCalled();
    });
  });

  describe('should disable the launch button when the SAS app status is not RUNNING', () => {
    test.each(minus(ALL_GKE_APP_STATUSES, [AppStatus.RUNNING]))(
      'Status %s',
      async (appStatus) => {
        const localizeSpy = jest
          .spyOn(appsApi(), 'localizeApp')
          .mockImplementation((): Promise<any> => Promise.resolve());
        const focusStub = jest.fn();
        const windowOpenSpy = jest
          .spyOn(window, 'open')
          .mockReturnValue({ focus: focusStub } as any as Window);

        await component(UIAppType.SAS, {
          appName: 'my-app',
          googleProject,
          status: appStatus,
        });

        const launchButton = screen.getByRole('button', {
          name: 'Open SAS',
        });
        // TODO this doesn't work because we set the styling differently here.
        // but really this is a fragile hack that we should ideally be moving away from
        // expectButtonElementDisabled(launchButton);

        launchButton.click();
        // disabled so nothing happens
        await waitFor(() => {
          expect(localizeSpy).not.toHaveBeenCalled();
          expect(windowOpenSpy).not.toHaveBeenCalled();
          expect(focusStub).not.toHaveBeenCalled();
        });
      }
    );
  });
});
