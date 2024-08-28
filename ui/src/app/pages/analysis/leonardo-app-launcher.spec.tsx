import '@testing-library/jest-dom';

import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { Route, Router } from 'react-router-dom';
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

import { render, screen, waitFor, within } from '@testing-library/react';
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

import { waitForNoSpinner } from 'testing/react-test-helpers';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { JupyterApiStub } from 'testing/stubs/jupyter-api-stub';
import { LeoRuntimesApiStub } from 'testing/stubs/leo-runtimes-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { ProxyApiStub } from 'testing/stubs/proxy-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';

function currentCardText() {
  return screen.getByTestId('current-progress-card').textContent;
}

function getCardSpinnerTestId(cardState: ProgressCardState) {
  return 'progress-card-spinner-' + cardState.valueOf();
}

const updateRuntimeStatus = (
  status: RuntimeStatus,
  runtimeStub: RuntimeApiStub
) => {
  act(() => {
    runtimeStub.runtime.status = status;
    jest.runOnlyPendingTimers();
  });
};

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

  const notebookName = 'wharrgarbl';
  const notebookInitialUrl = `${analysisTabPath(
    'namespace',
    'id'
  )}/${notebookName}`;
  const history = createMemoryHistory({ initialEntries: [notebookInitialUrl] });

  const notebookComponent = () => {
    return render(
      <Router history={history}>
        <Route path={`/workspaces/:ns/:terraName/${analysisTabName}/:nbName`}>
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
    user = userEvent.setup();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', async () => {
    notebookComponent();
    expect(
      screen.getByRole('heading', {
        name: new RegExp(`loading notebook: ${notebookName}`, 'i'),
      })
    ).toBeInTheDocument();
  });

  it('should show redirect display before showing notebook', async () => {
    notebookComponent();
    expect(screen.getByTestId('leo-app-launcher')).toBeInTheDocument();
  });

  test.each([
    ['a new', notebookInitialUrl + '?kernelType=R?creating=false'],
    ['an existing', notebookInitialUrl],
  ])(
    'should be "Initializing" until a Creating runtime for %s notebook is running',
    async (url) => {
      history.push(url);
      runtimeStub.runtime.status = RuntimeStatus.CREATING;
      notebookComponent();

      await screen.findByTestId(
        getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
      );

      await waitFor(() => {
        expect(currentCardText()).toContain(
          notebookProgressStrings.get(Progress.Initializing)
        );
      });

      updateRuntimeStatus(RuntimeStatus.RUNNING, runtimeStub);

      await screen.findByTestId(
        getCardSpinnerTestId(ProgressCardState.Redirecting)
      );

      expect(currentCardText()).toContain(
        notebookProgressStrings.get(Progress.Redirecting)
      );
    }
  );

  test.each([
    ['a new', notebookInitialUrl + '?kernelType=R?creating=false'],
    ['an existing', notebookInitialUrl],
  ])(
    'should be "Resuming" until a Stopped runtime for %s is running',
    async (url) => {
      history.push(url);
      runtimeStub.runtime.status = RuntimeStatus.STOPPED;

      notebookComponent();

      await screen.findByTestId(
        getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
      );

      await waitFor(() => {
        expect(currentCardText()).toContain(
          notebookProgressStrings.get(Progress.Resuming)
        );
      });

      updateRuntimeStatus(RuntimeStatus.RUNNING, runtimeStub);

      await screen.findByTestId(
        getCardSpinnerTestId(ProgressCardState.Redirecting)
      );
      expect(currentCardText()).toContain(
        notebookProgressStrings.get(Progress.Redirecting)
      );
    }
  );

  test.each([
    ['a new', notebookInitialUrl + '?kernelType=R?creating=false'],
    ['an existing', notebookInitialUrl],
  ])(
    'should be "Redirecting" when the runtime is initially Running for %s notebook',
    async (url) => {
      history.push(url);
      runtimeStub.runtime.status = RuntimeStatus.RUNNING;

      notebookComponent();

      await screen.findByTestId(
        getCardSpinnerTestId(ProgressCardState.Redirecting)
      );
      expect(currentCardText()).toContain(
        notebookProgressStrings.get(Progress.Redirecting)
      );
    }
  );

  it('should navigate away after runtime transitions to deleting', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    notebookComponent();

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.Redirecting)
    );
    act(() => jest.runOnlyPendingTimers());
    await waitForNoSpinner();
    await screen.findByTitle('Iframe Container');
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

    notebookComponent();

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.Redirecting)
    );
    act(() => jest.runOnlyPendingTimers());
    await waitForNoSpinner();
    await screen.findByTitle('Iframe Container');
    expect(mockNavigate).not.toHaveBeenCalled();

    // Simulate transition to updating.
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.UPDATING,
    }));
    act(() => jest.runOnlyPendingTimers());

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

    notebookComponent();

    await screen.findByText(
      /your analysis environment was temporarily suspended but is now available for use\./i
    );
    expect(screen.queryByTitle('Iframe Container')).not.toBeInTheDocument();
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

    notebookComponent();

    await screen.findByText(
      /your analysis environment was temporarily suspended but is now available for use\./i
    );
    expect(screen.queryByTitle('Iframe Container')).not.toBeInTheDocument();
  });

  it('should show runtime initializer modal if runtime not found', async () => {
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime = null;

    notebookComponent();

    await screen.findByText('Create an Analysis Environment');
  });

  test.each(['cancel', 'configure'])(
    'should show retry message on runtime initializer %s',
    async () => {
      jest.useRealTimers();
      history.push(notebookInitialUrl + '?kernelType=R?creating=false');
      runtimeStub.runtime = null;

      notebookComponent();

      const dialog = await screen.findByRole('dialog');
      await user.click(
        within(dialog).getByRole('button', {
          name: /cancel/i,
        })
      );

      await waitFor(() => {
        expect(dialog).not.toBeInTheDocument();
      });

      expect(
        screen.getByText(/This action requires an analysis environment\./i)
      ).toBeInTheDocument();
    }
  );

  it('should create runtime on runtime initializer create', async () => {
    jest.useRealTimers();
    history.push(notebookInitialUrl + '?kernelType=R?creating=false');
    runtimeStub.runtime = null;

    notebookComponent();

    const dialog = await screen.findByRole('dialog');
    await user.click(
      within(dialog).getByRole('button', {
        name: /create environment/i,
      })
    );

    await waitFor(() => {
      expect(dialog).not.toBeInTheDocument();
    });

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
    const t = render(
      <Router history={history}>
        <Route path='/workspaces/:ns/:terraName/terminals'>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.JupyterTerminal}
          />
        </Route>
      </Router>
    );
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

    await terminalComponent();

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
    );
    await waitFor(() => {
      expect(currentCardText()).toContain(
        genericProgressStrings.get(Progress.Initializing)
      );
    });

    updateRuntimeStatus(RuntimeStatus.RUNNING, runtimeStub);
    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.Redirecting)
    );
    expect(currentCardText()).toContain(
      genericProgressStrings.get(Progress.Redirecting)
    );
  });

  it('should navigate away after runtime transitions to deleting', async () => {
    history.push(terminalInitialUrl);
    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    await terminalComponent();

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => {
      jest.advanceTimersByTime(2000);
    });
    jest.runOnlyPendingTimers();

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.Redirecting)
    );
    act(() => jest.runOnlyPendingTimers());
    await waitForNoSpinner();
    await screen.findByTitle('Iframe Container');
    expect(mockNavigate).not.toHaveBeenCalled();

    // Simulate transition to deleting - should navigate away.
    await updateRuntime((runtime) => ({
      ...runtime,
      status: RuntimeStatus.DELETING,
    }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith([
        'workspaces',
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
        analysisTabName,
      ]);
    });
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
    const t = render(
      <Router history={history}>
        <Route path='/workspaces/:ns/:terraName/spark/:sparkConsolePath'>
          <LeonardoAppLauncher
            hideSpinner={() => {}}
            showSpinner={() => {}}
            leoAppType={LeoApplicationType.SparkConsole}
          />
        </Route>
      </Router>
    );
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

    await terminalComponent();

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)
    );
    await waitFor(() => {
      expect(currentCardText()).toContain(
        genericProgressStrings.get(Progress.Initializing)
      );
    });

    updateRuntimeStatus(RuntimeStatus.RUNNING, runtimeStub);

    runtimeStub.runtime.status = RuntimeStatus.RUNNING;

    await screen.findByTestId(
      getCardSpinnerTestId(ProgressCardState.Redirecting)
    );

    await waitFor(() => {
      expect(currentCardText()).toContain(
        genericProgressStrings.get(Progress.Redirecting)
      );
    });
  });
});
