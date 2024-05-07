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

import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { defaultAppRequest } from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { appTypeToString } from 'app/utils/user-apps-utils';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementEnabled,
  expectDropdown,
  getDropdownOption,
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

// tests for behavior common to all GKE Apps.  For app-specific tests, see e.g. create-cromwell-spec
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

  describe.each([
    [AppType.CROMWELL, createListAppsCromwellResponse],
    [AppType.RSTUDIO, createListAppsRStudioResponse],
    [AppType.SAS, createListAppsSASResponse],
  ])('%s', (appType: AppType, listAppsResponse: () => UserAppEnvironment) => {
    const startButtonText = `${appTypeToString[appType]} cloud environment create button`;

    it('Should create an app and close the panel when the create button is clicked', async () => {
      await component(appType);

      const spyCreateApp = jest
        .spyOn(appsApi(), 'createApp')
        .mockImplementation((): Promise<any> => Promise.resolve());

      const startButton = screen.getByLabelText(startButtonText);
      expectButtonElementEnabled(startButton);
      startButton.click();

      await waitFor(() => {
        expect(spyCreateApp).toHaveBeenCalledTimes(1);
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

      const spyCreateApp = jest
        .spyOn(appsApi(), 'createApp')
        .mockImplementation((): Promise<any> => Promise.resolve());

      const startButton = screen.getByLabelText(startButtonText);
      expectButtonElementEnabled(startButton);
      startButton.click();

      await waitFor(() => {
        expect(spyCreateApp).toHaveBeenCalledTimes(1);
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

      await component(appType, { disk, onClickDeleteUnattachedPersistentDisk });

      const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
      expectButtonElementEnabled(deleteButton);
      deleteButton.click();

      await waitFor(() => {
        expect(onClickDeleteUnattachedPersistentDisk).toHaveBeenCalledTimes(1);
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
        const cpuDropdown = expectDropdown(container, cpuId);
        expect(cpuDropdown).toBeInTheDocument();
        await userEvent.click(cpuDropdown);

        const differentCpuOption = getDropdownOption(
          container,
          cpuId,
          differentCpuCount
        );
        await userEvent.click(differentCpuOption);

        const ramId = `${appTypeToString[appType]}-ram`;
        const ramDropdown = expectDropdown(container, ramId);
        expect(ramDropdown).toBeInTheDocument();
        await userEvent.click(ramDropdown);

        const differentRamOption = getDropdownOption(
          container,
          ramId,
          differentRamCount
        );
        await userEvent.click(differentRamOption);

        const spyCreateApp = jest
          .spyOn(appsApi(), 'createApp')
          .mockImplementation((): Promise<any> => Promise.resolve());

        const startButton = screen.getByLabelText(startButtonText);
        expectButtonElementEnabled(startButton);
        startButton.click();

        await waitFor(() => {
          expect(spyCreateApp).toHaveBeenCalledTimes(1);
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
      ],
      [
        'the app is running',
        {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
        {
          userApps: [{ ...listAppsResponse(), status: AppStatus.RUNNING }],
        },
        /Cannot configure the compute profile of a running application/,
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
              status: AppStatus.DELETED,
            },
            {
              ...listAppsResponse(),
              appType: otherAppType[appType],
              status: AppStatus.RUNNING,
            },
          ],
        },
        /Cannot configure the compute profile when there are applications running in the workspace/,
      ],
    ])(
      `should not allow machine type configuration when %s`,
      async (
        _,
        config: ConfigResponse,
        propOverrides: Partial<CreateGkeAppProps>,
        tooltip: string | RegExp
      ) => {
        serverConfigStore.set({ config });

        const { container } = await component(appType, propOverrides);

        const cpuDropdown = container.querySelector(
          `${appTypeToString[appType]}-cpu`
        );
        expect(cpuDropdown).not.toBeInTheDocument();

        // matches the default configuration text output: 4 CPUS, 15GB RAM, 50GB disk
        const fixedConfiguration = screen.getByText(/CPUS/);
        await user.hover(fixedConfiguration);

        expect(screen.getByText(tooltip)).toBeInTheDocument();
      }
    );
  });
});
