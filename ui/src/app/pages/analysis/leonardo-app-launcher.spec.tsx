import * as React from 'react';
import { act } from 'react-dom/test-utils';
import Iframe from 'react-iframe';
import { Route, Router } from 'react-router-dom';
import { mount, ReactWrapper } from 'enzyme';
import { createMemoryHistory } from 'history';
import { mockNavigate } from 'setupTests';

import {
  DisksApi,
  ErrorCode,
  Runtime,
  RuntimeApi,
  RuntimeStatus,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  genericProgressStrings,
  LeoApplicationType,
  LeonardoAppLauncher,
  notebookProgressStrings,
  Progress,
  ProgressCardState,
} from 'app/pages/analysis/leonardo-app-launcher';
import {
  analysisTabName,
  analysisTabPath,
  workspacePath,
} from 'app/routing/utils';
import { registerApiClient as registerApiClientNotebooks } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import {
  profileStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';
import {
  JupyterApi,
  ProxyApi,
  RuntimesApi as LeoRuntimesApi,
} from 'notebooks-generated/fetch';

import {
  waitForFakeTimersAndUpdate,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { JupyterApiStub } from 'testing/stubs/jupyter-api-stub';
import { LeoRuntimesApiStub } from 'testing/stubs/leo-runtimes-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { ProxyApiStub } from 'testing/stubs/proxy-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { SecuritySuspendedMessage } from './notebook-frame-error';

function currentCardText(wrapper: ReactWrapper) {
  return wrapper.find('[data-test-id="current-progress-card"]').first().text();
}

function currentCardTextAlt() {
  return screen.getByTestId('current-progress-card').textContent;
}

function getCardSpinnerTestId(cardState: ProgressCardState) {
  return '[data-test-id="progress-card-spinner-' + cardState.valueOf() + '"]';
}

function getCardSpinnerTestIdAlt(cardState: ProgressCardState) {
  return 'progress-card-spinner-' + cardState.valueOf();
}

describe('NotebookLauncher', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub;
  let user;

  const notebookInitialUrl = `${analysisTabPath('namespace', 'id')}/wharrgarbl`;
  const history = createMemoryHistory({ initialEntries: [notebookInitialUrl] });

  const notebookComponent = async () => {
    const c = mount(
      <Router history={history}>
        <Route path={`/workspaces/:ns/:wsid/${analysisTabName}/:nbName`}>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.JupyterNotebook}
          />
        </Route>
      </Router>
    );
    return c;
  };

  const notebookComponentAlt = () => {
    return render(
      <Router history={history}>
        <Route path={`/workspaces/:ns/:wsid/${analysisTabName}/:nbName`}>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.JupyterNotebook}
          />
        </Route>
      </Router>
    );
  };

  async function updateRuntime(updateFn: (r: Runtime) => Runtime) {
    await act(() => {
      runtimeStub.runtime = updateFn(runtimeStub.runtime);
      runtimeStore.set({
        workspaceNamespace: workspace.namespace,
        runtime: runtimeStub.runtime,
        runtimeLoaded: true,
      });
      return Promise.resolve();
    });
  }

  // Runtime is on
  const updateRuntimeStatus = (status: RuntimeStatus) => {
    act(() => {
      runtimeStub.runtime.status = status;
      jest.runOnlyPendingTimers();
    });
  };

  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClient(DisksApi, new DisksApiStub());
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(ProxyApi, new ProxyApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    serverConfigStore.set({ config: { gsuiteDomain: 'x' } });
    history.push(notebookInitialUrl + '?kernelType=R?creating=true');
    currentWorkspaceStore.next(workspace);
    profileStore.set({ profile, load, reload, updateCache });
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: undefined,
      runtimeLoaded: false,
    });

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', async () => {
    notebookComponentAlt();
    expect(
      screen.getByRole('heading', {
        name: /loading notebook: wharrgarbl/i,
      })
    ).toBeInTheDocument();
  });

  it('should show redirect display before showing notebook', async () => {
    notebookComponentAlt();
    expect(screen.getByTestId('leo-app-launcher')).toBeInTheDocument();
  });

  it('should be "Initializing" until a Creating runtime for an existing notebook is running', async () => {
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    notebookComponentAlt();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.UnknownInitializingResuming)
    );

    await waitFor(() => {
      expect(currentCardTextAlt()).toContain(
        notebookProgressStrings.get(Progress.Initializing)
      );
    });

    updateRuntimeStatus(RuntimeStatus.RUNNING);

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );

    expect(currentCardTextAlt()).toContain(
      notebookProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should be "Resuming" until a Stopped runtime for an existing notebook is running', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime.status = RuntimeStatus.STOPPED;

    notebookComponentAlt();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.UnknownInitializingResuming)
    );

    await waitFor(() => {
      expect(currentCardTextAlt()).toContain(
        notebookProgressStrings.get(Progress.Resuming)
      );
    });

    updateRuntimeStatus(RuntimeStatus.RUNNING);

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );

    expect(currentCardTextAlt()).toContain(
      notebookProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should be "Resuming" until a Stopped runtime for a new notebook is running', async () => {
    runtimeStub.runtime.status = RuntimeStatus.STOPPED;

    notebookComponentAlt();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.UnknownInitializingResuming)
    );

    await waitFor(() => {
      expect(currentCardTextAlt()).toContain(
        notebookProgressStrings.get(Progress.Resuming)
      );
    });

    updateRuntimeStatus(RuntimeStatus.RUNNING);

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );
    expect(currentCardTextAlt()).toContain(
      notebookProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should be "Redirecting" when the runtime is initially Running for an existing notebook', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    notebookComponentAlt();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );
    expect(currentCardTextAlt()).toContain(
      notebookProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should be "Redirecting" when the runtime is initially Running for a new notebook', async () => {
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    notebookComponentAlt();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );
    expect(currentCardTextAlt()).toContain(
      notebookProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should navigate away after runtime transitions to deleting', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    notebookComponentAlt();

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });
    jest.runOnlyPendingTimers();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );

    await screen.findByTitle('Notebook Container');
    expect(mockNavigate).not.toHaveBeenCalled();

    // Simulate transition to deleting - should navigate away.
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.DELETING,
    }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled();
    });
  });

  it('should not navigate to notebook after runtime transitions to updating', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    notebookComponentAlt();

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });
    jest.runOnlyPendingTimers();

    await screen.findByTestId(
      getCardSpinnerTestIdAlt(ProgressCardState.Redirecting)
    );

    await screen.findByTitle('Notebook Container');
    expect(mockNavigate).not.toHaveBeenCalled();

    // Simulate transition to updating.
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.UPDATING,
    }));
    jest.runOnlyPendingTimers();

    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('should show error on initial compute suspension', async () => {
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: undefined,
      runtimeLoaded: false,
      loadingError: new ComputeSecuritySuspendedError({
        suspendedUntil: new Date('2000-01-01 03:00:00').toISOString(),
      }),
    });

    notebookComponentAlt();

    await screen.findByText(
      /your analysis environment was temporarily suspended but is now available for use\./i
    );
    expect(screen.queryByTitle('Notebook Container')).not.toBeInTheDocument();
  });

  it('should show error on mid-load compute suspension', async () => {
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.STARTING,
    }));

    runtimeStub.getRuntime = () =>
      Promise.reject(
        new Response(
          JSON.stringify({
            errorCode: ErrorCode.COMPUTE_SECURITY_SUSPENDED,
            parameters: {
              suspendedUntil: new Date('2000-01-01 03:00:00').toISOString(),
            },
          }),
          {
            status: 412,
          }
        )
      );

    notebookComponentAlt();

    await screen.findByText(
      /your analysis environment was temporarily suspended but is now available for use\./i
    );
    expect(screen.queryByTitle('Notebook Container')).not.toBeInTheDocument();
  });

  it('should show runtime initializer modal if runtime not found', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime = null;

    const wrapper = await notebookComponent();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists({ 'data-test-id': 'runtime-initializer-create' })
    ).toBeTruthy();
  });

  test.each(['cancel', 'configure'])(
    'should show retry message on runtime initializer %s',
    async (action) => {
      history.push(notebookInitialUrl + '?kernelType=R?creating=false');
      runtimeStub.runtime = null;

      const wrapper = await notebookComponent();
      await waitForFakeTimersAndUpdate(wrapper);

      wrapper
        .find({ 'data-test-id': `runtime-initializer-${action}` })
        .simulate('click');
      await waitForFakeTimersAndUpdate(wrapper);

      expect(
        wrapper.exists({ 'data-test-id': `runtime-initializer-${action}` })
      ).toBeFalsy();
      expect(wrapper.text()).toContain(
        'This action requires an analysis environment.'
      );
    }
  );

  it('should create runtime on runtime initializer create', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime = null;

    const wrapper = await notebookComponent();
    await waitForFakeTimersAndUpdate(wrapper);

    wrapper
      .find({ 'data-test-id': 'runtime-initializer-create' })
      .simulate('click');
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists({ 'data-test-id': 'runtime-initializer-create' })
    ).toBeFalsy();
    expect(runtimeStub.runtime).toBeTruthy();
  });
});

describe('TerminalLauncher', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub;

  const terminalInitialUrl = workspacePath('namespace', 'id') + '/terminals';
  const history = createMemoryHistory({ initialEntries: [terminalInitialUrl] });

  const terminalComponent = async () => {
    const t = mount(
      <Router history={history}>
        <Route path='/workspaces/:ns/:wsid/terminals'>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.JupyterTerminal}
          />
        </Route>
      </Router>
    );
    await waitOneTickAndUpdate(t);
    return t;
  };

  async function updateRuntime(updateFn: (r: Runtime) => Runtime) {
    await act(() => {
      runtimeStub.runtime = updateFn(runtimeStub.runtime);
      runtimeStore.set({
        workspaceNamespace: workspace.namespace,
        runtime: runtimeStub.runtime,
        runtimeLoaded: true,
      });
      return Promise.resolve();
    });
  }

  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(ProxyApi, new ProxyApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    serverConfigStore.set({ config: { gsuiteDomain: 'x' } });
    currentWorkspaceStore.next(workspace);
    profileStore.set({ profile, load, reload, updateCache });
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: undefined,
      runtimeLoaded: true,
    });

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should display terminal state header correctly when RuntimeStatus changes', async () => {
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    const wrapper = await terminalComponent();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists(
        getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
      )
    ).toBeTruthy();
    expect(currentCardText(wrapper)).toContain(
      genericProgressStrings.get(Progress.Initializing)
    );

    runtimeStub.runtime.status = RuntimeStatus.RUNNING;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists(getCardSpinnerTestId(ProgressCardState.Redirecting))
    ).toBeTruthy();
    expect(currentCardText(wrapper)).toContain(
      genericProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should navigate away after runtime transitions to deleting', async () => {
    history.push(terminalInitialUrl);
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    const wrapper = await terminalComponent();
    await waitForFakeTimersAndUpdate(wrapper);

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(Iframe).exists()).toBeTruthy();
    expect(mockNavigate).not.toHaveBeenCalled();

    // Simulate transition to deleting - should navigate away.
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.DELETING,
    }));
    await waitForFakeTimersAndUpdate(wrapper);

    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      'defaultNamespace',
      '1',
      analysisTabName,
    ]);
  });
});

describe('SparkConsoleLauncher', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub;

  const terminalInitialUrl =
    workspacePath('namespace', 'id') + '/spark/apphistory';
  const history = createMemoryHistory({ initialEntries: [terminalInitialUrl] });

  const terminalComponent = async () => {
    const t = mount(
      <Router history={history}>
        <Route path='/workspaces/:ns/:wsid/spark/:sparkConsolePath'>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.SparkConsole}
          />
        </Route>
      </Router>
    );
    await waitOneTickAndUpdate(t);
    return t;
  };

  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(ProxyApi, new ProxyApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    serverConfigStore.set({ config: { gsuiteDomain: 'x' } });
    currentWorkspaceStore.next(workspace);
    profileStore.set({ profile, load, reload, updateCache });
    runtimeStore.set({
      workspaceNamespace: workspace.namespace,
      runtime: undefined,
      runtimeLoaded: true,
    });

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should display progress correctly when RuntimeStatus changes', async () => {
    runtimeStub.runtime.status = RuntimeStatus.CREATING;

    const wrapper = await terminalComponent();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists(
        getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
      )
    ).toBeTruthy();
    expect(currentCardText(wrapper)).toContain(
      genericProgressStrings.get(Progress.Initializing)
    );

    runtimeStub.runtime.status = RuntimeStatus.RUNNING;
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.exists(getCardSpinnerTestId(ProgressCardState.Redirecting))
    ).toBeTruthy();
    expect(currentCardText(wrapper)).toContain(
      genericProgressStrings.get(Progress.Redirecting)
    );
  });
});
