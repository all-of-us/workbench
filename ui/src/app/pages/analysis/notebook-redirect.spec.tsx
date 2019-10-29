import {mount} from 'enzyme';
import * as React from 'react';

import {NotebookRedirect} from './notebook-redirect';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {ClusterApi, ClusterLocalizeRequest, ClusterStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {ClusterApi as NotebooksClusterApi, JupyterApi, NotebooksApi} from 'notebooks-generated/fetch';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {NotebooksApiStub} from 'testing/stubs/notebooks-api-stub';
import {queryParamsStore, serverConfigStore} from 'app/utils/navigation';
import {Kernels} from 'app/utils/notebook-kernels';
import {NotebooksClusterApiStub} from 'testing/stubs/notebooks-cluster-api-stub';
import {workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore, urlParamsStore, userProfileStore} from '../../utils/navigation';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

import {exportFunctions} from 'app/utils/index';

describe('NotebookRedirect', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const reload = jest.fn();
  const updateCache = jest.fn();

  let clusterStub: ClusterApiStub;

  const progressCardIds = {
    initializing: 0,
    authenticating: 1,
    loading: 2,
    redirecting: 3
  };



  const component = () => {
    return mount(<NotebookRedirect/>);
  };

  const getCardSpinnerTestId = (cardId) => {
    return '[data-test-id="progress-card-spinner-' + cardId + '"]';
  };

  beforeEach(() => {
    clusterStub = new ClusterApiStub();
    clusterStub.cluster.status = ClusterStatus.Creating;

    registerApiClient(ClusterApi, clusterStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(NotebooksApi, new NotebooksApiStub());
    registerApiClientNotebooks(NotebooksClusterApi, new NotebooksClusterApiStub());

    serverConfigStore.next({useBillingProjectBuffer: false, gsuiteDomain: 'x'});
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
    exportFunctions.timeout = jest.fn();
    jest.useFakeTimers();

  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should show redirect display before showing notebook', async() => {
    const wrapper = component();
    expect(wrapper.exists('[data-test-id="notebook-redirect"]')).toBeTruthy();
  });

  it('redirect state should be "Initializing" until cluster is running', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper
      .exists(getCardSpinnerTestId(progressCardIds.initializing)))
      .toBeTruthy();
    clusterStub.cluster.status = ClusterStatus.Running;
    // TODO: not sure how to get repoll to actually happen, or if repoll is
    // happening and we are just never changing the cluster status to running
    // so it goes into an infinite loop waiting for the cluster.
    await waitOneTickAndUpdate(wrapper);
    jest.runAllTimers();
    await Promise.resolve();
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(progressCardIds.initializing)))
      .toBeFalsy();
    expect(wrapper.exists(getCardSpinnerTestId(progressCardIds.authenticating))).toBeTruthy();
  });


});