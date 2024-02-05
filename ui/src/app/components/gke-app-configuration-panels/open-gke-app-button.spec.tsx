import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { AppsApi, AppStatus, BillingStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UIAppType } from 'app/components/apps-panel/utils';
import { appDisplayPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
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

  const findOpenButton = () =>
    screen.getByRole('button', {
      name: 'Cromwell cloud environment open button',
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
    await component({
      userApp: createListAppsCromwellResponse({ status: AppStatus.RUNNING }),
      onClose,
    });

    const button = await waitFor(() => {
      const openButton = findOpenButton();
      expectButtonElementEnabled(openButton);
      return openButton;
    });

    button.click();
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith([
        appDisplayPath(
          WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
          WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
          UIAppType.CROMWELL
        ),
      ]);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('should not allow creating a GKE app when billing status is not active.', async () => {
    await component({
      userApp: createListAppsCromwellResponse({ status: AppStatus.RUNNING }),
      billingStatus: BillingStatus.INACTIVE,
    });
    const button = await waitFor(() => {
      const openButton = findOpenButton();
      expectButtonElementDisabled(openButton);
      return openButton;
    });

    await user.pointer([{ pointerName: 'mouse', target: button }]);

    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
  });
});
