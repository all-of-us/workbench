import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppType,
  DisksApi,
  UserAppEnvironment,
  WorkspaceAccessLevel,
} from 'generated/fetch';
import { AppsApi } from 'generated/fetch/api';

import { render, screen, waitFor } from '@testing-library/react';
import {
  defaultCromwellConfig,
  defaultRStudioConfig,
  defaultSASConfig,
} from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { appTypeToString } from 'app/utils/user-apps-utils';

import defaultServerConfig from 'testing/default-server-config';
import { expectButtonElementEnabled } from 'testing/react-test-helpers';
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
  app: undefined,
  disk: undefined,
  onClickDeleteGkeApp: jest.fn(),
  onClickDeleteUnattachedPersistentDisk: jest.fn(),
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
    [AppType.CROMWELL, defaultCromwellConfig, createListAppsCromwellResponse],
    [AppType.RSTUDIO, defaultRStudioConfig, createListAppsRStudioResponse],
    [AppType.SAS, defaultSASConfig, createListAppsSASResponse],
  ])(
    '%s',
    (
      appType: AppType,
      appConfig: UserAppEnvironment,
      listAppsResponse: () => UserAppEnvironment
    ) => {
      const startButtonText = `${appTypeToString[appType]} cloud environment create button`;

      it('Should create an app and close the panel when the create button is clicked', async () => {
        await component(appType, {
          app: undefined,
          disk: undefined,
        });

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
            appConfig
          );
          expect(onClose).toHaveBeenCalledTimes(1);
        });
      });

      it('should use the existing PD when creating', async () => {
        const disk = stubDisk();
        await component(appType, {
          app: undefined,
          disk,
        });

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
          app: listAppsResponse(),
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
          app: undefined,
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

        await component(appType, {
          app: createListAppsSASResponse(),
          disk,
        });

        const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
        expect(deleteButton).toBeNull();
      });

      it('should not render a Delete Persistent Disk link when no disk is present', async () => {
        await component(appType, {
          app: undefined,
          disk: undefined,
        });

        const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
        expect(deleteButton).toBeNull();
      });
    }
  );
});
