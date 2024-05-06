import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppsApi,
  AppStatus,
  AppType,
  DisksApi,
  UserAppEnvironment,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { defaultAppRequest } from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { appTypeToString } from 'app/utils/user-apps-utils';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementEnabled,
  expectDropdown,
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

  const component = async (
    appType: AppType,
    propOverrides?: Partial<CreateGkeAppProps>
  ) =>
    render(
      <CreateGkeApp {...{ ...defaultProps, appType, ...propOverrides }} />
    );

  beforeEach(async () => {
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

    it('should allow machine type configuration when there are no running apps', async () => {
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      // can't configure when running, so set to deleted
      const { container } = await component(appType);

      const cpuDropdown = expectDropdown(
        container,
        `${appTypeToString[appType]}-cpu`
      );

      expect(cpuDropdown).toBeInTheDocument();

      // TODO now do something with the dropdown
    });

    it('should allow machine type configuration when the current app is DELETED', async () => {
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      // can't configure when running, so set to deleted
      const { container } = await component(appType, {
        userApps: [{ ...listAppsResponse(), status: AppStatus.DELETED }],
      });

      const cpuDropdown = expectDropdown(
        container,
        `${appTypeToString[appType]}-cpu`
      );

      expect(cpuDropdown).toBeInTheDocument();

      // TODO now do something with the dropdown
    });

    it('should allow machine type configuration when an existing app has the same configuration', async () => {
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      const thisAppConfig: UserAppEnvironment = {
        ...listAppsResponse(),
        status: AppStatus.DELETED,
      };
      const otherAppConfig: UserAppEnvironment = {
        ...listAppsResponse(),
        appType: otherAppType[appType],
        status: AppStatus.RUNNING,
      };

      // sanity check: same machine type
      expect(thisAppConfig.kubernetesRuntimeConfig.machineType).toEqual(
        otherAppConfig.kubernetesRuntimeConfig.machineType
      );

      const { container } = await component(appType, {
        userApps: [thisAppConfig, otherAppConfig],
      });

      const cpuDropdown = expectDropdown(
        container,
        `${appTypeToString[appType]}-cpu`
      );

      expect(cpuDropdown).toBeInTheDocument();

      // TODO now do something with the dropdown
    });

    it('should not allow machine type configuration when the feature flag is false', async () => {
      const user = userEvent.setup();
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: false,
        },
      });

      // can't configure when running, so set to deleted
      const { container } = await component(appType, {
        userApps: [{ ...listAppsResponse(), status: AppStatus.DELETED }],
      });

      const cpuDropdown = container.querySelector(
        `${appTypeToString[appType]}-cpu`
      );
      expect(cpuDropdown).not.toBeInTheDocument();

      // matches the default configuration text output: 4 CPUS, 15GB RAM, 50GB disk
      const fixedConfiguration = screen.getByText(/CPUS/);
      await user.hover(fixedConfiguration);

      expect(
        screen.getByText(
          `The cloud compute profile for ${appTypeToString[appType]} beta is non-configurable.`
        )
      ).toBeInTheDocument();
    });

    it('should not allow machine type configuration when the app is running', async () => {
      const user = userEvent.setup();
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      const { container } = await component(appType, {
        userApps: [{ ...listAppsResponse(), status: AppStatus.RUNNING }],
      });

      const cpuDropdown = container.querySelector(
        `${appTypeToString[appType]}-cpu`
      );
      expect(cpuDropdown).not.toBeInTheDocument();

      // matches the default configuration text output: 4 CPUS, 15GB RAM, 50GB disk
      const fixedConfiguration = screen.getByText(/CPUS/);
      await user.hover(fixedConfiguration);

      expect(
        screen.getByText(
          /Cannot configure the compute profile of an active environment/
        )
      ).toBeInTheDocument();
    });

    it('should not allow machine type configuration when the app is deleting', async () => {
      const user = userEvent.setup();
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      const { container } = await component(appType, {
        userApps: [{ ...listAppsResponse(), status: AppStatus.DELETING }],
      });

      const cpuDropdown = container.querySelector(
        `${appTypeToString[appType]}-cpu`
      );
      expect(cpuDropdown).not.toBeInTheDocument();

      // matches the default configuration text output: 4 CPUS, 15GB RAM, 50GB disk
      const fixedConfiguration = screen.getByText(/CPUS/);
      await user.hover(fixedConfiguration);

      expect(
        screen.getByText(
          /Cannot configure the compute profile of an environment which is being deleted/
        )
      ).toBeInTheDocument();
    });

    it('should not allow machine type configuration when another app has a different machine type', async () => {
      const user = userEvent.setup();
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableGKEAppMachineTypeChoice: true,
        },
      });

      const thisAppConfig: UserAppEnvironment = {
        ...listAppsResponse(),
        status: AppStatus.DELETED,
        kubernetesRuntimeConfig: {
          ...listAppsResponse().kubernetesRuntimeConfig,
          machineType: 'n1-standard-1',
        },
      };
      const otherAppConfig: UserAppEnvironment = {
        ...listAppsResponse(),
        appType: otherAppType[appType],
        status: AppStatus.RUNNING,
        kubernetesRuntimeConfig: {
          ...listAppsResponse().kubernetesRuntimeConfig,
          machineType: 'n1-standard-2',
        },
      };

      // sanity check: different machine type
      expect(thisAppConfig.kubernetesRuntimeConfig.machineType).not.toEqual(
        otherAppConfig.kubernetesRuntimeConfig.machineType
      );

      const { container } = await component(appType, {
        userApps: [thisAppConfig, otherAppConfig],
      });

      const cpuDropdown = container.querySelector(
        `${appTypeToString[appType]}-cpu`
      );
      expect(cpuDropdown).not.toBeInTheDocument();

      // matches the default configuration text output: 4 CPUS, 15GB RAM, 50GB disk
      const fixedConfiguration = screen.getByText(/CPUS/);
      await user.hover(fixedConfiguration);

      expect(
        screen.getByText(
          /environments already exist in the workspace with differing compute profiles/
        )
      ).toBeInTheDocument();
    });
  });
});
