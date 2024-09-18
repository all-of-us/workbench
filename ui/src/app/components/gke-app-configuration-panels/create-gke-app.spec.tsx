import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppsApi,
  AppStatus,
  AppType,
  ConfigResponse,
  DisksApi,
  UserAppEnvironment,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import {
  appMaxDiskSize,
  appMinDiskSize,
  defaultAppRequest,
} from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  AutodeleteDaysThresholds,
  findMachineByName,
} from 'app/utils/machines';
import { serverConfigStore } from 'app/utils/stores';
import { appTypeToString } from 'app/utils/user-apps-utils';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectDropdown,
  getDropdownOption,
  pickSpinButtonValue,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
  createListAppsRStudioResponse,
  createListAppsSASResponse,
} from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { ALL_GKE_APP_TYPES, minus } from 'testing/utils';

import {
  CommonCreateGkeAppProps,
  CreateGkeApp,
  CreateGkeAppProps,
} from './create-gke-app';

const onClose = jest.fn();
const freeTierBillingAccountId = 'freetier';
export const defaultProps: CommonCreateGkeAppProps = {
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
  userApps: [],
  disk: undefined,
  onClickDeleteGkeApp: jest.fn(),
  onClickDeleteUnattachedPersistentDisk: jest.fn(),
};

const defaultApp: UserAppEnvironment = {
  appName: 'all-of-us-rstudio-123',
  googleProject: 'test-project',
  diskName: 'test-disk',
  status: 'RUNNING',
  appType: AppType.RSTUDIO,
  dateAccessed: new Date().toISOString(),
  autodeleteEnabled: false,
};

// when we need an arbitrary choice of a different type
const otherAppType: Record<AppType, AppType> = {
  [AppType.CROMWELL]: AppType.RSTUDIO,
  [AppType.RSTUDIO]: AppType.SAS,
  [AppType.SAS]: AppType.CROMWELL,
};

// tests for behavior common to all GKE Apps.  For app-type-specific tests, see e.g. create-cromwell-spec
describe(CreateGkeApp.name, () => {
  let disksApiStub: DisksApiStub;
  let user: UserEvent;

  const component = async (
    appType: AppType,
    propOverrides?: Partial<CreateGkeAppProps>
  ) =>
    render(
      <CreateGkeApp {...{ ...defaultProps, appType, ...propOverrides }} />
    );

  const spinDiskElement = (diskName: string): HTMLElement =>
    screen.getByRole('spinbutton', {
      name: diskName,
    });

  beforeEach(async () => {
    user = userEvent.setup();

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
  });

  const startButtonText = (appType: AppType): string =>
    `${appTypeToString[appType]} cloud environment create button`;

  // this indirection is because spyOn() can't be called here, since it requires the API to be registered first
  const getCreateAppSpy = () =>
    jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

  const expectCreation = async (
    appType: AppType
  ): Promise<jest.SpyInstance> => {
    const createAppSpy = getCreateAppSpy();

    const startButton = screen.getByLabelText(startButtonText(appType));
    expectButtonElementEnabled(startButton);
    startButton.click();

    await waitFor(() => expect(createAppSpy).toHaveBeenCalledTimes(1));
    return createAppSpy;
  };

  // note: update these if we add more app types
  describe.each([
    [AppType.CROMWELL, createListAppsCromwellResponse, 'RStudio and SAS'],
    [AppType.RSTUDIO, createListAppsRStudioResponse, 'Cromwell and SAS'],
    [AppType.SAS, createListAppsSASResponse, 'Cromwell and RStudio'],
  ])(
    '%s',
    (
      appType: AppType,
      listAppsResponse: () => UserAppEnvironment,
      otherAppTypes: string
    ) => {
      it('Should create an app and close the panel when the create button is clicked', async () => {
        await component(appType);

        const spyCreateApp = await expectCreation(appType);
        await waitFor(() => {
          expect(spyCreateApp).toHaveBeenCalledWith(
            WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
            defaultAppRequest[appType]
          );
          expect(onClose).toHaveBeenCalledTimes(1);
        });
      });

      it('should use the existing PD when creating', async () => {
        const disk = stubDisk();
        await component(appType, { disk });

        const spyCreateApp = await expectCreation(appType);
        await waitFor(() => {
          expect(spyCreateApp.mock.calls[0][1].persistentDiskRequest).toEqual(
            disk
          );
          expect(onClose).toHaveBeenCalledTimes(1);
        });
      });

      it('should allow deleting the environment when an app is running', async () => {
        const disk = stubDisk();
        const onClickDeleteGkeApp = jest.fn();

        await component(appType, {
          userApps: [listAppsResponse()],
          disk,
          onClickDeleteGkeApp,
        });

        const deleteButton = screen.queryByLabelText('Delete Environment');
        expectButtonElementEnabled(deleteButton);
        deleteButton.click();

        await waitFor(() => {
          expect(onClickDeleteGkeApp).toHaveBeenCalledTimes(1);
        });
      });

      it('should not render a Delete Environment link when no app is present', async () => {
        await component(appType);
        const deleteButton = screen.queryByLabelText('Delete Environment');
        expect(deleteButton).not.toBeInTheDocument();
      });

      it('should allow deletion of a Persistent Disk when a disk is present but no app', async () => {
        const disk = stubDisk();
        const onClickDeleteUnattachedPersistentDisk = jest.fn();

        await component(appType, {
          disk,
          onClickDeleteUnattachedPersistentDisk,
        });

        const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
        expectButtonElementEnabled(deleteButton);
        deleteButton.click();

        await waitFor(() => {
          expect(onClickDeleteUnattachedPersistentDisk).toHaveBeenCalledTimes(
            1
          );
        });
      });

      it('should not render a Delete Persistent Disk link when an app is present', async () => {
        const disk = stubDisk();

        await component(appType, { userApps: [listAppsResponse()], disk });

        const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
        expect(deleteButton).toBeNull();
      });

      it('should not render a Delete Persistent Disk link when no disk is present', async () => {
        await component(appType);

        const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
        expect(deleteButton).toBeNull();
      });

      it('should allow setting the autodelete threshold', async () => {
        const { container } = await component(appType);

        // must match one of the choices
        const autodeleteDays = AutodeleteDaysThresholds[1];
        const autodeleteThreshold = autodeleteDays * 24 * 60;

        // sanity check: does not match default
        expect(autodeleteThreshold).not.toEqual(
          defaultAppRequest[appType].autodeleteThreshold
        );

        const autodeleteId = `${appTypeToString[appType]}-autodelete-threshold-dropdown`;
        const autodeleteOption = await getDropdownOption(
          container,
          autodeleteId,
          `Idle for ${autodeleteDays} days`
        );
        await userEvent.click(autodeleteOption);

        const spyCreateApp = await expectCreation(appType);
        await waitFor(() => {
          expect(spyCreateApp).toHaveBeenCalledWith(
            WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
            { ...defaultAppRequest[appType], autodeleteThreshold }
          );
          expect(onClose).toHaveBeenCalledTimes(1);
        });
      });

      const autodeleteTestApp: UserAppEnvironment = {
        ...defaultApp,
        appType,
        autodeleteEnabled: true,
        kubernetesRuntimeConfig: {
          machineType: 'n1-standard-4',
          numNodes: 0,
          autoscalingEnabled: false,
        },
      };

      it('should correctly calculate autodeleteRemainingDays', async () => {
        const now = new Date();
        now.setDate(now.getDate() - 2); // Subtract 2 days
        const dateAccessed = now;
        const autodeleteThreshold = 7 * 24 * 60 + 1; // 7 days in minutes plus a small buffer

        await component(appType, {
          userApps: [
            {
              ...autodeleteTestApp,
              dateAccessed: dateAccessed.toISOString(),
              autodeleteThreshold,
            },
          ],
        });

        expect(
          screen.queryByLabelText('Autodelete remaining days')
        ).toHaveTextContent('5 days remain until deletion');
      });

      it('should show correct message when autodeleteRemainingDays is 0', async () => {
        const now = new Date();
        now.setDate(now.getDate() - 2); // Subtract 2 days
        const dateAccessed = now;
        const autodeleteThreshold = 2 * 24 * 60 + 1; // 2 days in minutes plus small buffer.

        await component(appType, {
          userApps: [
            {
              ...autodeleteTestApp,
              dateAccessed: dateAccessed.toISOString(),
              autodeleteThreshold,
            },
          ],
        });

        // less than 1 day (0 days) remaining until deletion.
        expect(
          screen.queryByLabelText('Autodelete remaining days')
        ).toHaveTextContent('App will be deleted within 1 day');
      });

      it('should not show autodeleteRemainingDays when app is not running', async () => {
        // Render the component with the app not running
        await component(appType);

        // Query for the autodeleteRemainingDays text
        const autodeleteRemainingDaysText = screen.queryByLabelText(
          'Autodelete remaining days'
        );

        // Assert that the autodeleteRemainingDays text is not in the document
        expect(autodeleteRemainingDaysText).not.toBeInTheDocument();
      });

      // positive tests for machine type configuration

      it.each([
        ['there are no running apps', {}],
        [
          'the current app is DELETED',
          {
            userApps: [{ ...listAppsResponse(), status: AppStatus.DELETED }],
          },
        ],
      ])(
        `should allow machine type configuration when %s`,
        async (_, propOverrides: Partial<CreateGkeAppProps>) => {
          serverConfigStore.set({
            config: {
              ...serverConfigStore.get().config,
              enableGKEAppMachineTypeChoice: true,
            },
          });

          const { container } = await component(appType, propOverrides);

          // default is 4 CPUs, 15 GB RAM, n1-standard-4
          const differentCpuCount = '2';
          const differentRamCount = '7.5';
          const differentMachineType = 'n1-standard-2';

          // sanity check: does not match default
          expect(differentMachineType).not.toEqual(
            listAppsResponse().kubernetesRuntimeConfig.machineType
          );

          const cpuId = `${appTypeToString[appType]}-cpu`;
          const differentCpuOption = await getDropdownOption(
            container,
            cpuId,
            differentCpuCount
          );
          await userEvent.click(differentCpuOption);

          const ramId = `${appTypeToString[appType]}-ram`;
          const differentRamOption = await getDropdownOption(
            container,
            ramId,
            differentRamCount
          );
          await userEvent.click(differentRamOption);

          const spyCreateApp = await expectCreation(appType);
          await waitFor(() => {
            expect(spyCreateApp).toHaveBeenCalledWith(
              WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
              expect.objectContaining({
                kubernetesRuntimeConfig: {
                  ...listAppsResponse().kubernetesRuntimeConfig,
                  machineType: differentMachineType,
                },
              })
            );
          });
        }
      );

      it(`should display the correct sharing text for ${appType} when machine configuration is enabled`, async () => {
        serverConfigStore.set({
          config: {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
        });

        const { container } = await component(appType);

        // show that machine configuration is enabled

        const cpuId = `${appTypeToString[appType]}-cpu`;
        const cpuDropdown = expectDropdown(container, cpuId);
        expect(cpuDropdown).toBeInTheDocument();

        expect(
          screen.getByText(
            `Your ${appTypeToString[appType]} environment will share CPU and RAM resources with any ` +
              otherAppTypes +
              ' environments you run in this workspace.',
            { exact: false }
          )
        ).toBeInTheDocument();
      });

      it(`should use the default machine type for ${appType} when there are no running apps`, async () => {
        const { cpu, memory } = findMachineByName(
          defaultAppRequest[appType].kubernetesRuntimeConfig.machineType
        );

        serverConfigStore.set({
          config: {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
        });

        const { container } = await component(appType);

        // show that the default machine configuration is selected

        const cpuId = `${appTypeToString[appType]}-cpu`;
        const defaultCpuOption = await getDropdownOption(
          container,
          cpuId,
          cpu.toString()
        );
        expect(defaultCpuOption).toBeInTheDocument();
        expect(defaultCpuOption).toHaveAttribute('aria-selected', 'true');

        const ramId = `${appTypeToString[appType]}-ram`;
        const defaultRamOption = await getDropdownOption(
          container,
          ramId,
          memory.toString()
        );
        expect(defaultRamOption).toBeInTheDocument();
        expect(defaultRamOption).toHaveAttribute('aria-selected', 'true');
      });

      // negative tests for machine type configuration

      it.each([
        [
          'the feature flag is false',
          {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: false,
          },
          {},
          `The cloud compute profile for ${appTypeToString[appType]} beta is non-configurable.`,
          /4 CPUS, 15GB RAM/, // default machine type
        ],
        [
          'the app is running',
          {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
          {
            userApps: [
              {
                ...listAppsResponse(),
                status: AppStatus.RUNNING,
                kubernetesRuntimeConfig: {
                  ...listAppsResponse().kubernetesRuntimeConfig,
                  machineType: 'n1-standard-8',
                },
              },
            ],
          },
          /Cannot configure the compute profile of a running application/,
          /8 CPUS, 30GB RAM/, // n1-standard-8 - matches the running app
        ],
        [
          'the app is deleting',
          {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
          {
            userApps: [{ ...listAppsResponse(), status: AppStatus.DELETING }],
          },
          /Cannot configure the compute profile of an application which is being deleted/,
          /4 CPUS, 15GB RAM/,
        ],
        [
          'another app exists',
          {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
          {
            userApps: [
              {
                ...listAppsResponse(),
                appType: otherAppType[appType],
                status: AppStatus.RUNNING,
                kubernetesRuntimeConfig: {
                  ...listAppsResponse().kubernetesRuntimeConfig,
                  machineType: 'n1-standard-8',
                },
              },
            ],
          },
          /Cannot configure the compute profile when there are applications running in the workspace/,
          /8 CPUS, 30GB RAM/, // n1-standard-8 - matches the other app
        ],
      ])(
        `should not allow machine type configuration when %s`,
        async (
          _,
          config: ConfigResponse,
          propOverrides: Partial<CreateGkeAppProps>,
          tooltip: string | RegExp,
          expectedConfiguration: RegExp
        ) => {
          serverConfigStore.set({ config });

          const { container } = await component(appType, propOverrides);

          const cpuDropdown = container.querySelector(
            `${appTypeToString[appType]}-cpu`
          );
          expect(cpuDropdown).not.toBeInTheDocument();

          const fixedConfiguration = screen.getByText(expectedConfiguration, {
            exact: false,
          });
          expect(fixedConfiguration).toBeInTheDocument();
          await user.hover(fixedConfiguration);

          expect(screen.getByText(tooltip)).toBeInTheDocument();
        }
      );

      it(`should display the correct sharing text for ${appType} when machine configuration is disabled`, async () => {
        serverConfigStore.set({
          config: {
            ...serverConfigStore.get().config,
            enableGKEAppMachineTypeChoice: true,
          },
        });

        const { container } = await component(appType);

        // show that machine configuration is disabled

        const cpuDropdown = container.querySelector(
          `${appTypeToString[appType]}-cpu`
        );
        expect(cpuDropdown).not.toBeInTheDocument();

        expect(
          screen.getByText(
            `Your ${appTypeToString[appType]} environment will share CPU and RAM resources with any ` +
              otherAppTypes +
              ' environments you run in this workspace.',
            { exact: false }
          )
        ).toBeInTheDocument();
      });

      // Disk size Tests
      it(`Display default disk Size for ${appType}`, async () => {
        await component(appType);
        const defaultDiskSize =
          defaultAppRequest[appType].persistentDiskRequest.size;

        expect(spinDiskElement('gke-app-disk').getAttribute('value')).toEqual(
          defaultDiskSize.toString()
        );
      });

      it(`Should allow to create app request for valid disk size for ${appType}`, async () => {
        const spyCreateApp = getCreateAppSpy();

        // Arrange
        await component(appType);

        const diskSize = 750;

        const appRequest = {
          ...defaultAppRequest[appType],
          persistentDiskRequest: {
            ...defaultAppRequest[appType].persistentDiskRequest,
            size: diskSize,
          },
        };

        // Act
        // Confirm the disk size is in valid range and set it
        expect(diskSize).toBeGreaterThan(appMinDiskSize[appType]);
        expect(diskSize).toBeLessThanOrEqual(appMaxDiskSize);

        await pickSpinButtonValue(user, 'gke-app-disk', diskSize);
        expect(spinDiskElement('gke-app-disk').getAttribute('value')).toEqual(
          diskSize.toString()
        );

        // As disk size is a valid size, there SHOULD NOT be any disk size error message
        expect(screen.queryByText(/disk size must be between/i)).toBeNull();

        const startButton = screen.getByLabelText(startButtonText(appType));
        expectButtonElementEnabled(startButton);
        startButton.click();

        // Assert
        await waitFor(() => {
          expect(spyCreateApp).toHaveBeenCalledTimes(1);
          expect(spyCreateApp).toHaveBeenCalledWith(
            WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
            appRequest
          );
          expect(onClose).toHaveBeenCalledTimes(1);
        });
      });

      it(`Should not allow disk size less than minimum for ${appType}`, async () => {
        // Arrange
        await component(appType);
        const minAppSize = appMinDiskSize[appType];

        // Act
        await pickSpinButtonValue(user, 'gke-app-disk', minAppSize);
        expect(spinDiskElement('gke-app-disk').getAttribute('value')).toEqual(
          minAppSize.toString()
        );
        expectButtonElementEnabled(
          screen.queryByLabelText(startButtonText(appType))
        );

        // Set disk size one less than minimum size to trigger warning
        await pickSpinButtonValue(user, 'gke-app-disk', minAppSize - 1);

        // Assert
        // Expect a warning message now and create button should be disabled
        expect(screen.getByText(/disk size must be between/i)).toBeTruthy();
        expect(spinDiskElement('gke-app-disk').getAttribute('value')).toEqual(
          (minAppSize - 1).toString()
        );
        expectButtonElementDisabled(
          screen.queryByLabelText(startButtonText(appType))
        );
      });

      it(`Should not allow disk size more than maximum for ${appType}`, async () => {
        // Arrange
        await component(appType);
        const createButton = `${appTypeToString[appType]} cloud environment create button`;

        // Act
        await pickSpinButtonValue(user, 'gke-app-disk', appMaxDiskSize);
        let spinDiskInitValue = parseInt(
          spinDiskElement('gke-app-disk')
            .getAttribute('value')
            .replace(/,/g, ''),
          10
        );
        expect(spinDiskInitValue).toEqual(appMaxDiskSize);
        expectButtonElementEnabled(screen.queryByLabelText(createButton));

        // Set disk size one more than maximum size to trigger warning
        await pickSpinButtonValue(user, 'gke-app-disk', appMaxDiskSize + 1);

        // Assert
        // We do this to remove an comma from numbers
        spinDiskInitValue = parseInt(
          spinDiskElement('gke-app-disk')
            .getAttribute('value')
            .replace(/,/g, ''),
          10
        );
        // Expect a warning message now and create button should be disabled
        expect(screen.getByText(/disk size must be between/i)).toBeTruthy();
        expect(spinDiskInitValue).toEqual(appMaxDiskSize + 1);
        expectButtonElementDisabled(screen.queryByLabelText(createButton));
      });

      it('Should disable disk size input if App exists', async () => {
        // Arrange
        const disk = stubDisk();

        await component(appType, {
          userApps: [listAppsResponse()],
          disk,
        });

        // Assert
        expect(
          spinDiskElement('gke-app-disk').attributes.getNamedItem('disabled')
        ).toBeTruthy();
      });

      it('Should enable disk size input if App does not exist but PD is present', async () => {
        // Arrange
        const disk = stubDisk();
        await component(appType, { disk });

        // Assert
        expect(
          spinDiskElement('gke-app-disk').attributes.getNamedItem('disabled')
        ).toBeFalsy();
      });
    }
  );

  // disallow disabling Autodelete for SAS

  describe(AppType.SAS, () => {
    it('should disallow disabling autodelete', async () => {
      const appType = AppType.SAS;
      const createAppSpy = getCreateAppSpy();
      await component(appType);

      const autodeleteCheckbox = screen.getByRole('checkbox', {
        name: 'Auto-deletion toggle',
      });
      expect(autodeleteCheckbox).toBeChecked(); // default = true
      autodeleteCheckbox.click();

      // can't wait for a thing not to happen, so we have to check for the rejection
      await expect(
        waitFor(() => expect(createAppSpy).toHaveBeenCalled())
      ).rejects.toThrow();

      expect(autodeleteCheckbox).toBeChecked();
      expect(onClose).not.toHaveBeenCalled();
    });
  });

  // allow disabling Autodelete for every type except SAS

  const allExceptSAS: AppType[] = minus(ALL_GKE_APP_TYPES, [AppType.SAS]);

  describe.each(allExceptSAS)('%s', (appType: AppType) => {
    it('should allow disabling autodelete', async () => {
      await component(appType);

      const autodeleteCheckbox = screen.getByRole('checkbox', {
        name: 'Auto-deletion toggle',
      });
      expect(autodeleteCheckbox).toBeChecked(); // default = true
      autodeleteCheckbox.click();
      expect(autodeleteCheckbox).not.toBeChecked();

      const spyCreateApp = await expectCreation(appType);
      await waitFor(() => {
        expect(spyCreateApp).toHaveBeenCalledWith(
          WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
          { ...defaultAppRequest[appType], autodeleteEnabled: false }
        );
        expect(onClose).toHaveBeenCalledTimes(1);
      });
    });
  });
});
