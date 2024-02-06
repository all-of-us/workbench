import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { AppsApi, AppStatus, AppType, BillingStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { appDisplayPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { appTypeToString } from 'app/utils/user-apps-utils';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsRStudioResponse,
  createListAppsSASResponse,
} from 'testing/stubs/apps-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';

import { OpenGkeAppButton, OpenGkeAppButtonProps } from './open-gke-app-button';

describe(OpenGkeAppButton.name, () => {
  const defaultProps: OpenGkeAppButtonProps = {
    userApp: null,
    billingStatus: BillingStatus.ACTIVE,
    workspace: workspaceStubs[0],
    onClose: () => {},
  };

  let user;

  const component = async (propOverrides?: Partial<OpenGkeAppButtonProps>) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(<OpenGkeAppButton {...allProps} />);
  };

  const findOpenButton = (appType: AppType) =>
    screen.getByRole('button', {
      name: `${appTypeToString[appType]} cloud environment open button`,
    });

  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    user = userEvent.setup();
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should allow opening a running GKE app', async () => {
    const onClose = jest.fn();
    const userApp = createListAppsRStudioResponse({
      status: AppStatus.RUNNING,
    });
    await component({ userApp, onClose });

    const button = await waitFor(() => {
      const openButton = findOpenButton(userApp.appType);
      expectButtonElementEnabled(openButton);
      return openButton;
    });

    button.click();
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith([
        appDisplayPath(
          WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
          WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
          appTypeToString[userApp.appType]
        ),
      ]);
    });

    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });

  it('should not allow creating a GKE app when billing status is not active.', async () => {
    const userApp = createListAppsSASResponse({
      status: AppStatus.RUNNING,
    });
    await component({ userApp, billingStatus: BillingStatus.INACTIVE });
    const button = await waitFor(() => {
      const openButton = findOpenButton(userApp.appType);
      expectButtonElementDisabled(openButton);
      return openButton;
    });

    await user.pointer([{ pointerName: 'mouse', target: button }]);

    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
  });
});
