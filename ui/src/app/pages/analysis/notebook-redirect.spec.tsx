import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';
import Iframe from 'react-iframe';
import {act} from 'react-dom/test-utils';

import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore, urlParamsStore} from 'app/utils/navigation';
import {profileStore, runtimeStore, serverConfigStore} from 'app/utils/stores';
import {Kernels} from 'app/utils/notebook-kernels';
import {RuntimeApi, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {RuntimesApi as LeoRuntimesApi, JupyterApi, ProxyApi} from 'notebooks-generated/fetch';
import {waitOneTickAndUpdate, waitForFakeTimersAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {ProxyApiStub} from 'testing/stubs/proxy-api-stub';
import {LeoRuntimesApiStub} from 'testing/stubs/leo-runtimes-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {navigateSpy} from '../../../testing/navigation-mock';

import {NotebookRedirect, Progress, ProgressCardState, progressStrings} from './notebook-redirect';

describe('NotebookRedirect', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub: RuntimeApiStub;

  const component = async() => {
    const c = mount(<NotebookRedirect hideSpinner={() => {}}
                                      showSpinner={() => {}}/>);
    await waitOneTickAndUpdate(c);
    return c;
  };

  function currentCardText(wrapper: ReactWrapper) {
    return wrapper.find('[data-test-id="current-progress-card"]').first().text();
  }

  function getCardSpinnerTestId(cardState: ProgressCardState) {
    return '[data-test-id="progress-card-spinner-' + cardState.valueOf() + '"]';
  }

  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(ProxyApi, new ProxyApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    serverConfigStore.set({config: {gsuiteDomain: 'x'}});
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      nbName: 'blah blah'
    });
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: true
    });
    currentWorkspaceStore.next(workspace);
    profileStore.set({profile, load, reload, updateCache});
    runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', async() => {
    const wrapper = await component();
    expect(wrapper).toBeTruthy();
  });

  it('should show redirect display before showing notebook', async() => {
    const wrapper = await component();
    expect(wrapper.exists('[data-test-id="notebook-redirect"]')).toBeTruthy();
  });

  it('should be "Initializing" until a Creating runtime for an existing notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Initializing));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Initializing" until a Creating runtime for a new notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Initializing));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Resuming" until a Stopped runtime for an existing notebook is running', async() => {
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Stopped;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Resuming));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Resuming" until a Stopped runtime for a new notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Stopped;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Resuming));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Redirecting" when the runtime is initially Running for an existing notebook', async() => {
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });


  it('should be "Redirecting" when the runtime is initially Running for a new notebook', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should navigate away after runtime transitions to deleting', async() => {
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(Iframe).exists()).toBeTruthy();
    expect(navigateSpy).not.toHaveBeenCalled();

    // Simulate transition to deleting - should navigate away.
    act(() => {
      runtimeStub.runtime = {...runtimeStub.runtime, status: RuntimeStatus.Deleting};
      runtimeStore.set({
        workspaceNamespace: workspace.namespace,
        runtime: runtimeStub.runtime
      });
    });
    await waitForFakeTimersAndUpdate(wrapper);

    expect(navigateSpy).toHaveBeenCalled();
  });


  it('should not navigate after runtime transitions to updating', async() => {
    const navSpy = jest.fn();
    NavStore.navigate = navSpy;

    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(Iframe).exists()).toBeTruthy();
    expect(navSpy).not.toHaveBeenCalled();

    // Simulate transition to updating.
    act(() => {
      runtimeStub.runtime = {...runtimeStub.runtime, status: RuntimeStatus.Updating};
      runtimeStore.set({
        workspaceNamespace: workspace.namespace,
        runtime: runtimeStub.runtime
      });
    });
    await waitForFakeTimersAndUpdate(wrapper);

    expect(navSpy).not.toHaveBeenCalled();
  });
});
