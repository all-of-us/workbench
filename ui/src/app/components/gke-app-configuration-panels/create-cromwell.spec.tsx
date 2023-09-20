import '@testing-library/jest-dom';

import * as React from 'react';

import { DisksApi, WorkspaceAccessLevel } from 'generated/fetch';
import { AppsApi } from 'generated/fetch/api';

import { render, screen, waitFor } from '@testing-library/react';
import { defaultCromwellConfig } from 'app/components/apps-panel/utils';
import { appsApi, registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { expectButtonElementEnabled } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';

import { CreateCromwell } from './create-cromwell';
import { CommonCreateGkeAppProps } from './create-gke-app';

describe(CreateCromwell.name, () => {
  const onClose = jest.fn();
  const freeTierBillingAccountId = 'freetier';

  const defaultProps: CommonCreateGkeAppProps = {
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
    onClickDeleteUnattachedPersistentDisk: jest.fn(),
  };

  let disksApiStub: DisksApiStub;

  const component = async (propOverrides?: Partial<CommonCreateGkeAppProps>) =>
    render(<CreateCromwell {...{ ...defaultProps, ...propOverrides }} />);

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

  it('start button should create cromwell and close panel', async () => {
    await component({
      app: undefined,
      disk: undefined,
    });

    const spyCreateApp = jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

    const startButton = screen.getByLabelText(
      'Cromwell cloud environment create button'
    );
    expectButtonElementEnabled(startButton);
    startButton.click();

    await waitFor(() => {
      expect(spyCreateApp).toHaveBeenCalledTimes(1);
      expect(spyCreateApp).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        defaultCromwellConfig
      );
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  it('should use the existing PD when creating', async () => {
    const disk = stubDisk();
    await component({
      app: undefined,
      disk,
    });

    const spyCreateApp = jest
      .spyOn(appsApi(), 'createApp')
      .mockImplementation((): Promise<any> => Promise.resolve());

    const startButton = screen.getByLabelText(
      'Cromwell cloud environment create button'
    );
    expectButtonElementEnabled(startButton);
    startButton.click();

    await waitFor(() => {
      expect(spyCreateApp).toHaveBeenCalledTimes(1);
      expect(spyCreateApp.mock.calls[0][1].persistentDiskRequest).toEqual(disk);
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  it('should display a cost of $0.40 per hour when running and $0.20 per hour when paused', async () => {
    await component();
    expect(screen.queryByLabelText('cost while running')).toHaveTextContent(
      '$0.40 per hour'
    );
    expect(screen.queryByLabelText('cost while paused')).toHaveTextContent(
      '$0.20 per hour'
    );
  });

  it('should render a DeletePersistentDiskButton when a disk is present but no app', async () => {
    const disk = stubDisk();
    const onClickDeleteUnattachedPersistentDisk = jest.fn();

    await component({
      app: undefined,
      disk,
      onClickDeleteUnattachedPersistentDisk,
    });

    const deleteButton = screen.getByLabelText('Delete Persistent Disk');
    expect(deleteButton).not.toBeNull();

    expectButtonElementEnabled(deleteButton);
    deleteButton.click();

    await waitFor(() => {
      expect(onClickDeleteUnattachedPersistentDisk).toHaveBeenCalledTimes(1);
    });
  });

  it('should not render a DeletePersistentDiskButton when an app is present', async () => {
    const disk = stubDisk();

    await component({
      app: createListAppsCromwellResponse(),
      disk,
    });

    const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
    expect(deleteButton).toBeNull();
  });

  it('should not render a DeletePersistentDiskButton no disk is present', async () => {
    await component({
      app: undefined,
      disk: undefined,
    });

    const deleteButton = screen.queryByLabelText('Delete Persistent Disk');
    expect(deleteButton).toBeNull();
  });
});
