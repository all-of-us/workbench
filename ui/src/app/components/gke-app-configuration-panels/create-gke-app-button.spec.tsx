import * as React from 'react';

import { AppsApi, AppStatus, BillingStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { defaultCromwellCreateRequest } from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

import {
  CreateGkeAppButton,
  CreateGKEAppButtonProps,
} from './create-gke-app-button';

describe(CreateGkeAppButton.name, () => {
  const defaultProps: CreateGKEAppButtonProps = {
    createAppRequest: defaultCromwellCreateRequest,
    existingApp: null,
    workspaceNamespace: 'aou-rw-test-1',
    onDismiss: () => {},
    username: ProfileStubVariables.PROFILE_STUB.username,
    billingStatus: BillingStatus.ACTIVE,
  };

  let user;

  const component = async (
    propOverrides?: Partial<CreateGKEAppButtonProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(<CreateGkeAppButton {...allProps} />);
  };

  const findCreateButton = () =>
    screen.getByRole('button', {
      name: 'Cromwell cloud environment create button',
    });

  const createEnabledStatuses = [AppStatus.DELETED, null, undefined];
  const createDisabledStatuses = minus(
    ALL_GKE_APP_STATUSES,
    createEnabledStatuses
  );

  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    user = userEvent.setup();
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('should allow creating a GKE app for certain app statuses', () => {
    test.each(createEnabledStatuses)('Status %s', async (appStatus) => {
      const createSpy = jest
        .spyOn(appsApi(), 'createApp')
        .mockImplementation((): Promise<any> => Promise.resolve());

      await component({
        createAppRequest: defaultCromwellCreateRequest,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });

      const button = await waitFor(() => {
        const createButton = findCreateButton();
        expectButtonElementEnabled(createButton);
        return createButton;
      });

      button.click();
      await waitFor(() => {
        expect(createSpy).toHaveBeenCalled();
      });
    });
  });

  describe('should not allow creating a GKE app for certain app statuses', () => {
    test.each(createDisabledStatuses)('Status %s', async (appStatus) => {
      await component({
        createAppRequest: defaultCromwellCreateRequest,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });
      const button = await waitFor(() => {
        const createButton = findCreateButton();
        expectButtonElementDisabled(createButton);
        return createButton;
      });

      await user.pointer([{ pointerName: 'mouse', target: button }]);

      await screen.findByText(`A Cromwell app exists or is being created`);
    });
  });

  it('should not allow creating a GKE app when billing status is not active.', async () => {
    await component({
      createAppRequest: defaultCromwellCreateRequest,
      billingStatus: BillingStatus.INACTIVE,
    });
    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await user.pointer([{ pointerName: 'mouse', target: button }]);

    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
  });
});
