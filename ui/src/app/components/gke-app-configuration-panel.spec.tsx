import * as React from 'react';
import { mount } from 'enzyme';

import { AppsApi, AppType } from 'generated/fetch';

import { CromwellConfigurationPanel } from 'app/components/cromwell-configuration-panel';
import {
  GKEAppConfigurationPanel,
  GkeAppConfigurationPanelProps,
} from 'app/components/gke-app-configuration-panel';
import { RStudioConfigurationPanel } from 'app/components/rstudio-configuration-panel';
import { DEFAULT_PROPS as RSTUDIO_DEFAULT_PROPS } from 'app/components/rstudio-configuration-panel.spec';
import { Spinner } from 'app/components/spinners';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { notificationStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';

describe(GKEAppConfigurationPanel.name, () => {
  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    notificationStore.set(null);

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
      },
    });

    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
  });

  const DEFAULT_PROPS = {
    type: AppType.RSTUDIO,
    workspaceNamespace: 'aou-rw-1234',
    ...RSTUDIO_DEFAULT_PROPS,
  };

  const createWrapper = (
    propOverrides: Partial<GkeAppConfigurationPanelProps> = {}
  ) => {
    const props = {
      ...DEFAULT_PROPS,
      ...propOverrides,
    };
    return mount(<GKEAppConfigurationPanel {...props} />);
  };

  it('should show a loading spinner while waiting for the API call to return', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    const wrapper = createWrapper();

    expect(wrapper.childAt(0).is(Spinner)).toBeTruthy();

    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.childAt(0).is(Spinner)).toBeFalsy();
  });

  it('should show an error if the API call fails', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.reject());

    expect(notificationStore.get()).toBeNull();

    const wrapper = createWrapper();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get().title).toBeTruthy();
    expect(notificationStore.get().message).toBeTruthy();
  });

  it('should not show an error if API call succeeds', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));

    expect(notificationStore.get()).toBeNull();

    const wrapper = createWrapper();
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get()).toBeNull();
  });

  it('should pass the user apps to the specific app component when the API call succeeds', async () => {
    const workspaceNamespace = 'aou-rw-1234';
    const apps = [createListAppsCromwellResponse()];
    const listAppsStub = jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve(apps));

    const wrapper = createWrapper({
      workspaceNamespace,
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    expect(listAppsStub).toHaveBeenCalledWith(workspaceNamespace);
    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    expect(cromwellPanel.prop('gkeAppsInWorkspace')).toEqual(apps);
  });

  it('should display the Cromwell panel when the type is Cromwell', async () => {
    const wrapper = createWrapper({
      type: AppType.CROMWELL,
    });
    await waitOneTickAndUpdate(wrapper);

    const cromwellPanel = wrapper.find(CromwellConfigurationPanel);
    expect(cromwellPanel.exists()).toBeTruthy();
    // check that an arbitrary CromwellConfigurationPanelProps prop is passed through
    expect(cromwellPanel.prop('onClose')).toEqual(DEFAULT_PROPS.onClose);
  });

  it('should display the RSudio panel when the type is RStudio', async () => {
    const wrapper = createWrapper({
      type: AppType.RSTUDIO,
    });
    await waitOneTickAndUpdate(wrapper);

    const rstudioPanel = wrapper.find(RStudioConfigurationPanel);
    expect(rstudioPanel.exists()).toBeTruthy();
    // check that an arbitrary RStudioConfigurationPanelProps prop is passed through
    expect(rstudioPanel.prop('onClose')).toEqual(DEFAULT_PROPS.onClose);
  });
});
