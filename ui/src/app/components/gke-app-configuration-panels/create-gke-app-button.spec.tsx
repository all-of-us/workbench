import * as React from 'react';

import {
  AppsApi,
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  DisksApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  appMaxDiskSize,
  appMinDiskSize,
  defaultCromwellCreateRequest,
} from 'app/components/apps-panel/utils';
import {
  appsApi,
  disksApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { nowPlusDays } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectTooltip,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

import {
  CreateGkeAppButton,
  CreateGKEAppButtonProps,
} from './create-gke-app-button';

describe(CreateGkeAppButton.name, () => {
  const workspaceNamespace = 'aou-rw-test-1';
  let defaultProps: CreateGKEAppButtonProps;

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

  // can't declare spies yet because the API is not registered

  const getCreateSpy = () =>
    jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

  const getUpdateDiskSpy = () =>
    jest
      .spyOn(disksApi(), 'updateDisk')
      .mockImplementation((): Promise<any> => Promise.resolve());

  beforeEach(() => {
    const freeTierBillingAccountId = 'FreeTierBillingAccountId';
    const workspaceStub = buildWorkspaceStub();
    const oneMinute = 60 * 1000;
    defaultProps = {
      createAppRequest: defaultCromwellCreateRequest,
      existingApp: null,
      workspace: {
        ...workspaceStub,
        billingAccountName: `billingAccounts/${freeTierBillingAccountId}`,
        namespace: workspaceNamespace,
        initialCredits: {
          ...workspaceStub.initialCredits,
          expirationEpochMillis: new Date().getTime() + 2 * oneMinute,
        },
      },
      onDismiss: () => {},
      username: ProfileStubVariables.PROFILE_STUB.username,
    };

    serverConfigStore.set({
      config: { ...defaultServerConfig, freeTierBillingAccountId },
    });
    registerApiClient(AppsApi, new AppsApiStub());
    registerApiClient(DisksApi, new DisksApiStub());
    user = userEvent.setup();
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('should allow creating a GKE app for certain app statuses', () => {
    test.each(createEnabledStatuses)('Status %s', async (appStatus) => {
      const createSpy = getCreateSpy();
      const updateDiskSpy = getUpdateDiskSpy();

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
        expect(createSpy).toHaveBeenCalledWith(
          workspaceNamespace,
          defaultCromwellCreateRequest
        );
      });
      expect(updateDiskSpy).not.toHaveBeenCalled();
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

      await expectTooltip(
        button,
        'A Cromwell app exists or is being created',
        user
      );
    });
  });

  it('should not allow creating a GKE app when initial credits are expired', async () => {
    await component({
      createAppRequest: defaultCromwellCreateRequest,
      workspace: {
        ...defaultProps.workspace,
        initialCredits: {
          exhausted: false,
          expirationEpochMillis: new Date().getTime() - 1, // Expired in past
          expirationBypassed: false,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await expectTooltip(
      button,
      'You have either run out of initial credits or have an inactive billing account.',
      user
    );
  });

  it('should allow creating a GKE app when initial credits are expired and bypassed', async () => {
    await component({
      createAppRequest: defaultCromwellCreateRequest,
      workspace: {
        ...defaultProps.workspace,
        initialCredits: {
          exhausted: false,
          expirationEpochMillis: new Date().getTime() - 1, // Expired in past
          expirationBypassed: true,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementEnabled(createButton);
      return createButton;
    });
    expect(button).toBeInTheDocument();
  });

  it('should not allow creating a GKE app when initial credits are exhausted', async () => {
    await component({
      createAppRequest: defaultCromwellCreateRequest,
      workspace: {
        ...defaultProps.workspace,
        initialCredits: {
          exhausted: true,
          expirationEpochMillis: nowPlusDays(1),
          expirationBypassed: false,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await expectTooltip(
      button,
      'You have either run out of initial credits or have an inactive billing account.',
      user
    );
  });

  it('should allow creating a GKE app when user is not using initial credits', async () => {
    await component({
      createAppRequest: defaultCromwellCreateRequest,
      workspace: {
        ...defaultProps.workspace,
        billingAccountName: 'userProvidedBillingAccount',
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementEnabled(createButton);
      return createButton;
    });
    expect(button).toBeInTheDocument();
  });

  it('should not allow creating a GKE app with a disk that is too small.', async () => {
    // Cromwell chosen arbitrarily
    const tooSmall = appMinDiskSize[AppType.CROMWELL] - 1;
    await component({
      createAppRequest: {
        ...defaultCromwellCreateRequest,
        persistentDiskRequest: {
          ...defaultCromwellCreateRequest.persistentDiskRequest,
          size: tooSmall,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await expectTooltip(
      button,
      'Disk cannot be more than 1000 GB or less than 50 GB',
      user
    );
  });

  it('should not allow creating a GKE app with a disk that is too large.', async () => {
    const tooLarge = appMaxDiskSize + 1;
    await component({
      createAppRequest: {
        ...defaultCromwellCreateRequest,
        persistentDiskRequest: {
          ...defaultCromwellCreateRequest.persistentDiskRequest,
          size: tooLarge,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await expectTooltip(
      button,
      'Disk cannot be more than 1000 GB or less than 50 GB',
      user
    );
  });

  it('should allow creating a GKE app with an existing disk.', async () => {
    const createSpy = getCreateSpy();
    const updateDiskSpy = getUpdateDiskSpy();

    const existingDiskSize = appMinDiskSize[AppType.CROMWELL] + 10;
    const diskName = 'arbitrary';

    const existingDisk: Disk = {
      ...stubDisk(),
      size: existingDiskSize,
      name: diskName,
      diskType: defaultCromwellCreateRequest.persistentDiskRequest.diskType,
      zone: 'us-central1-a',
    };
    const createAppRequest: CreateAppRequest = {
      ...defaultCromwellCreateRequest,
      persistentDiskRequest: {
        ...defaultCromwellCreateRequest.persistentDiskRequest,
        name: diskName,
        size: existingDiskSize,
      },
    };
    await component({ existingDisk, createAppRequest });
    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementEnabled(createButton);
      return createButton;
    });

    button.click();
    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        workspaceNamespace,
        createAppRequest
      );
    });
    expect(updateDiskSpy).not.toHaveBeenCalled();
  });

  it('should not allow creating a GKE app with a smaller disk.', async () => {
    const existingDiskSize = appMinDiskSize[AppType.CROMWELL] + 10;
    const smallerDiskSize = existingDiskSize - 1;
    const diskName = 'arbitrary';

    const existingDisk: Disk = {
      ...stubDisk(),
      size: existingDiskSize,
      name: diskName,
      diskType: defaultCromwellCreateRequest.persistentDiskRequest.diskType,
      zone: 'us-central1-a',
    };
    await component({
      existingDisk,
      createAppRequest: {
        ...defaultCromwellCreateRequest,
        persistentDiskRequest: {
          ...defaultCromwellCreateRequest.persistentDiskRequest,
          name: diskName,
          size: smallerDiskSize,
        },
      },
    });

    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementDisabled(createButton);
      return createButton;
    });

    await expectTooltip(
      button,
      /Preventing creation because this would cause data loss./,
      user
    );
  });

  it('should allow creating a GKE app with a larger disk.', async () => {
    const createSpy = getCreateSpy();
    const updateDiskSpy = getUpdateDiskSpy();

    const existingDiskSize = appMinDiskSize[AppType.CROMWELL] + 10;
    const largerDiskSize = existingDiskSize + 10;
    const diskName = 'arbitrary';

    const existingDisk: Disk = {
      ...stubDisk(),
      size: existingDiskSize,
      name: diskName,
      diskType: defaultCromwellCreateRequest.persistentDiskRequest.diskType,
    };
    const createAppRequest: CreateAppRequest = {
      ...defaultCromwellCreateRequest,
      persistentDiskRequest: {
        ...defaultCromwellCreateRequest.persistentDiskRequest,
        name: diskName,
        size: largerDiskSize,
      },
    };
    await component({ existingDisk, createAppRequest });
    const button = await waitFor(() => {
      const createButton = findCreateButton();
      expectButtonElementEnabled(createButton);
      return createButton;
    });

    button.click();
    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        workspaceNamespace,
        createAppRequest
      );
    });
    expect(updateDiskSpy).toHaveBeenCalledWith(
      workspaceNamespace,
      diskName,
      largerDiskSize
    );
  });
});
