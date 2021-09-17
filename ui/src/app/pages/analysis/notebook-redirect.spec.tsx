import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';
import Iframe from 'react-iframe';
import {act} from 'react-dom/test-utils';

import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {profileStore, runtimeStore, serverConfigStore} from 'app/utils/stores';
import {RuntimeApi, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {RuntimesApi as LeoRuntimesApi, JupyterApi, ProxyApi} from 'notebooks-generated/fetch';
import {waitOneTickAndUpdate, waitForFakeTimersAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {ProxyApiStub} from 'testing/stubs/proxy-api-stub';
import {LeoRuntimesApiStub} from 'testing/stubs/leo-runtimes-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs} from 'testing/stubs/workspaces';
import {NotebookRedirect, Progress, ProgressCardState, progressStrings} from './notebook-redirect';
import { Route, Router } from 'react-router-dom';
import { createMemoryHistory } from 'history';

describe('NotebookRedirect', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub;

  const initialUrl = '/workspaces/namespace/id/notebooks/wharrgarbl'
  let history = createMemoryHistory({initialEntries: [initialUrl]});

  const component = async() => {
    const c = mount(<Router history={history}>
      <Route path='/workspaces/:ns/:wsid/notebooks/:nbName'>
        <NotebookRedirect hideSpinner={() => {}}
                          showSpinner={() => {}}/>
      </Route>
    </Router>);
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
    history.push(initialUrl + '?kernelType=R?creating=true');
    currentWorkspaceStore.next(workspace);
    profileStore.set({profile, load, reload, updateCache});
    runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined, runtimeLoaded: true});

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
    history.push(initialUrl + '?kernelType=R?creating=false');
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
    history.push(initialUrl + '?kernelType=R?creating=false');
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
});
