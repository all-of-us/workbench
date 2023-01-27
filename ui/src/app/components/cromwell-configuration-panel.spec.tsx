import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  DisksApi,
  ProfileApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';
import { AppsApi, RuntimeApi } from 'generated/fetch/api';

import { Spinner } from 'app/components/spinners';
import {
  appsApi,
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  clearCompoundRuntimeOperations,
  profileStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import {
  workspaceDataStub,
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { defaultCromwellConfig } from './apps-panel/utils';
import { CromwellConfigurationPanel } from './cromwell-configuration-panel';

interface Props {
  onClose: () => void;
}

describe('CromwellConfigurationPanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;
  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;
  let onClose: () => void;
  let freeTierBillingAccountId: string;

  const component = async (propOverrides?: object) => {
    const allProps = { ...props, ...propOverrides };
    const c = mountWithRouter(<CromwellConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
  };

  beforeEach(async () => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeApiStub.runtime,
      runtimeLoaded: true,
    });

    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName: 'billingAccounts/' + freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      googleProject: runtimeApiStub.runtime.googleProject,
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
        freeTierBillingAccountId: 'freetier',
        defaultFreeCreditsDollarLimit: 100.0,
        gsuiteDomain: '',
      },
    });

    registerApiClient(AppsApi, new AppsApiStub());

    onClose = jest.fn();
    props = {
      onClose,
    };
    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('should show loading spinner while loading', async () => {
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeApiStub.runtime,
      runtimeLoaded: false,
    });
    const wrapper = await component();

    expect(wrapper.exists('#cromwell-configuration-panel')).toEqual(false);
    expect(wrapper.exists(Spinner)).toEqual(true);
  });

  it('should show configuration panel while not loading', async () => {
    const wrapper = await component();

    expect(wrapper.exists('#cromwell-configuration-panel')).toEqual(true);
    expect(wrapper.exists(Spinner)).toEqual(false);
  });

  it('start button should create cromwell and close panel', async () => {
    const wrapper = await component();
    const spyCreateApp = jest.spyOn(appsApi(), 'createApp');

    const startButton = wrapper
      .find('#cromwell-cloud-environment-create-button')
      .first();
    startButton.simulate('click');
    expect(spyCreateApp).toHaveBeenCalledTimes(1);
    expect(spyCreateApp).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      defaultCromwellConfig
    );
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
