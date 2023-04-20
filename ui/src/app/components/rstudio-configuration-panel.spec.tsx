import * as React from 'react';

import { DisksApi, WorkspaceAccessLevel } from 'generated/fetch';
import { AppsApi } from 'generated/fetch/api';

import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';

import { defaultRStudioConfig } from './apps-panel/utils';
import {
  BaseRStudioConfigurationPanel,
  RStudioConfigurationPanelProps,
} from './rstudio-configuration-panel';

describe('RStudioConfigurationPanel', () => {
  const onClose = jest.fn();
  const freeTierBillingAccountId = 'freetier';

  const DEFAULT_PROPS: RStudioConfigurationPanelProps = {
    onClose,
    creatorFreeCreditsRemaining: null,
    workspace: {
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName: 'billingAccounts/' + freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    },
    profileState: {
      profile: ProfileStubVariables.PROFILE_STUB,
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    },
    gkeAppsInWorkspace: [],
  };

  let disksApiStub: DisksApiStub;

  const component = async (
    propOverrides?: Partial<RStudioConfigurationPanelProps>
  ) => {
    const allProps = { ...DEFAULT_PROPS, ...propOverrides };
    const c = mountWithRouter(<BaseRStudioConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
  };

  beforeEach(async () => {
    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        freeTierBillingAccountId: freeTierBillingAccountId,
        defaultFreeCreditsDollarLimit: 100.0,
        gsuiteDomain: '',
      },
    });

    registerApiClient(AppsApi, new AppsApiStub());
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('start button should create rstudio and close panel', async () => {
    const wrapper = await component({
      gkeAppsInWorkspace: [],
    });
    await waitOneTickAndUpdate(wrapper);

    const spyCreateApp = jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());
    const startButton = wrapper
      .find('#RStudio-cloud-environment-create-button')
      .first();

    startButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spyCreateApp).toHaveBeenCalledTimes(1);
    expect(spyCreateApp).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      defaultRStudioConfig
    );
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should display a cost of $0.40 per hour when running and $0.21 per hour when paused', async () => {
    const wrapper = await component();

    const costEstimator = (w) => w.find('[data-test-id="cost-estimator"]');
    const runningCost = (w) =>
      costEstimator(w).find('[data-test-id="running-cost"]');
    const pausedCost = (w) =>
      costEstimator(w).find('[data-test-id="paused-cost"]');

    expect(costEstimator(wrapper).exists()).toBeTruthy();
    expect(runningCost(wrapper).text()).toEqual('$0.40 per hour');
    expect(pausedCost(wrapper).text()).toEqual('$0.21 per hour');
  });
});
