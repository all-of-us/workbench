import * as React from 'react';
import { mount } from 'enzyme';

import { AppsApi } from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import {
  withWorkspaceGkeApps,
  WithWorkspaceGKEAppsProps,
} from 'app/components/with-workspace-gke-apps';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';

describe(withWorkspaceGkeApps.name, () => {
  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    notificationStore.set(null);
  });

  const TestComponent = (_props) => <div />;

  const defaultProps = {
    workspaceNamespace: 'aou-rw-1234',
  };

  const createWrapper = (
    propOverrides: Partial<WithWorkspaceGKEAppsProps> = {}
  ) => {
    const props = {
      ...defaultProps,
      ...propOverrides,
    };
    const WrappedTestComponent = withWorkspaceGkeApps(TestComponent);
    return mount(<WrappedTestComponent {...props} />);
  };

  it('should show a loading spinner while waiting for the API call to return', async () => {
    jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve([]));
    const wrapper = createWrapper();

    expect(wrapper.find(Spinner).exists()).toBeTruthy();

    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find(Spinner).exists()).toBeFalsy();
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

  it('should pass the user apps to the wrapped component when the API call succeeds', async () => {
    const workspaceNamespace = 'aou-rw-1234';
    const apps = createListAppsCromwellResponse();
    const listAppsStub = jest
      .spyOn(appsApi(), 'listAppsInWorkspace')
      .mockImplementation((): Promise<any> => Promise.resolve(apps));

    const wrapper = createWrapper({ workspaceNamespace });
    await waitOneTickAndUpdate(wrapper);

    expect(listAppsStub).toHaveBeenCalledWith(workspaceNamespace);
    expect(wrapper.find(TestComponent).prop('gkeAppsInWorkspace')).toEqual(
      apps
    );
  });
});
