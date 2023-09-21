import * as React from 'react';

import { AppsApi, AppStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { defaultCromwellConfig } from 'app/components/apps-panel/utils';
import {
  CreateGkeAppButton,
  CreateGKEAppButtonProps,
} from 'app/components/gke-app-configuration-panels/create-gke-app-button';
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

describe(CreateGkeAppButton.name, () => {
  const defaultProps: CreateGKEAppButtonProps = {
    createAppRequest: defaultCromwellConfig,
    existingApp: null,
    workspaceNamespace: 'aou-rw-test-1',
    onDismiss: () => {},
    username: ProfileStubVariables.PROFILE_STUB.username,
  };

  const component = async (
    propOverrides?: Partial<CreateGKEAppButtonProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(<CreateGkeAppButton {...allProps} />);
  };

  const createEnabledStatuses = [AppStatus.DELETED, null, undefined];
  const createDisabledStatuses = minus(
    ALL_GKE_APP_STATUSES,
    createEnabledStatuses
  );

  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
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
        createAppRequest: defaultCromwellConfig,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });

      const button = await waitFor(() => {
        const createButton = screen.getByRole('button', {
          name: 'Cromwell cloud environment create button',
        });
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
        createAppRequest: defaultCromwellConfig,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });
      await waitFor(() => {
        const createButton = screen.getByRole('button', {
          name: 'Cromwell cloud environment create button',
        });
        expectButtonElementDisabled(createButton);
      });
    });
  });
});
