import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore, serverConfigStore, urlParamsStore, userProfileStore} from 'app/utils/navigation';
import {Kernels} from 'app/utils/notebook-kernels';
import {ClusterApi, ClusterStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {ClusterApi as NotebooksClusterApi, JupyterApi, NotebooksApi} from 'notebooks-generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {NotebooksApiStub} from 'testing/stubs/notebooks-api-stub';
import {NotebooksClusterApiStub} from 'testing/stubs/notebooks-cluster-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';
import {NotebookRedirect, ProgressCardState} from './notebook-redirect';

describe('NotebookRedirect', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const reload = jest.fn();
  const updateCache = jest.fn();

  let clusterStub: ClusterApiStub;

  const mountedComponent = () => {
    return mount(<NotebookRedirect/>);
  };

  const getCardSpinnerTestId = (cardState: ProgressCardState) => {
    return '[data-test-id="progress-card-spinner-' + cardState.valueOf() + '"]';
  };

  beforeEach(() => {
    clusterStub = new ClusterApiStub();
    clusterStub.cluster.status = ClusterStatus.Creating;

    registerApiClient(ClusterApi, clusterStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(NotebooksApi, new NotebooksApiStub());
    registerApiClientNotebooks(NotebooksClusterApi, new NotebooksClusterApiStub());

    serverConfigStore.next({gsuiteDomain: 'x'});
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
    userProfileStore.next({profile, reload, updateCache});

    // mock timers
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mountedComponent();
    expect(wrapper).toBeTruthy();
    wrapper.unmount();
  });

  it('should show redirect display before showing notebook', async() => {
    const wrapper = mountedComponent();
    expect(wrapper.exists('[data-test-id="notebook-redirect"]')).toBeTruthy();
    wrapper.unmount();
  });

  it('should be "Initializing" until a Creating cluster for an existing notebook is running', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: false});
    clusterStub.cluster.status = ClusterStatus.Creating;

    await expectRedirectingAfterRunning(wrapper);

    wrapper.unmount();
  });

  it('should be "Initializing" until a Creating cluster for a new notebook is running', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: true});
    clusterStub.cluster.status = ClusterStatus.Creating;

    await expectRedirectingAfterRunning(wrapper);

    wrapper.unmount();
  });

  it('should be "Resuming" until a Stopped cluster for an existing notebook is running', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: false});
    clusterStub.cluster.status = ClusterStatus.Stopped;

    await expectRedirectingAfterRunning(wrapper);

    wrapper.unmount();
  });

  it('should be "Resuming" until a Stopped cluster for a new notebook is running', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: true});
    clusterStub.cluster.status = ClusterStatus.Stopped;

    await expectRedirectingAfterRunning(wrapper);

    wrapper.unmount();
  });

  async function expectRedirectingAfterRunning(wrapper) {
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();

    clusterStub.cluster.status = ClusterStatus.Running;
    jest.runOnlyPendingTimers();
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
  }
});
