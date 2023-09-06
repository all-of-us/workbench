import * as React from 'react';

import { AppStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { defaultCromwellConfig } from 'app/components/apps-panel/utils';
import {
  CreateGKEAppButton,
  CreateGKEAppButtonProps,
} from 'app/components/gke-app-configuration-panels/create-gke-app-button';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import { createListAppsCromwellResponse } from 'testing/stubs/apps-api-stub';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

describe(CreateGKEAppButton.name, () => {
  const DEFAULT_PROPS: CreateGKEAppButtonProps = {
    createAppRequest: defaultCromwellConfig,
    existingApp: null,
    workspaceNamespace: 'aou-rw-test-1',
    onDismiss: () => {},
  };

  const component = async (
    propOverrides?: Partial<CreateGKEAppButtonProps>
  ) => {
    const allProps = { ...DEFAULT_PROPS, ...propOverrides };
    return render(<CreateGKEAppButton {...allProps} />);
  };

  const createEnabledStatuses = [AppStatus.DELETED, null, undefined];
  const createDisabledStatuses = minus(
    ALL_GKE_APP_STATUSES,
    createEnabledStatuses
  );

  describe('should allow creating a GKE app for certain app statuses', () => {
    test.each(createEnabledStatuses)('Status %s', async (appStatus) => {
      await component({
        createAppRequest: defaultCromwellConfig,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });
      await waitFor(() => {
        const createButton = screen.getByRole('button', {
          name: 'Cromwell cloud environment create button',
        });
        expectButtonElementEnabled(createButton);
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
