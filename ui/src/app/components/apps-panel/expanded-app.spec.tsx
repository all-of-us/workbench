import * as React from 'react';
import { mount } from 'enzyme';

import {
  AppsApi,
  NotebooksApi,
  RuntimeApi,
  UserAppEnvironment,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
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
    registerApiClient(RuntimeApi, runtimeStub);
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
  });

  it('renders', async () => {
    const wrapper = await component(UIAppType.CROMWELL, {});
    expect(wrapper.exists()).toBeTruthy();
  });
});
