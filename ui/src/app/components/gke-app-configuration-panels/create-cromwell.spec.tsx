import '@testing-library/jest-dom';

import * as React from 'react';

import { DisksApi, WorkspaceAccessLevel } from 'generated/fetch';
import { AppsApi } from 'generated/fetch/api';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { CreateCromwell } from './create-cromwell';
import { CommonCreateGkeAppProps } from './create-gke-app';

// tests for behavior specific to Cromwell.  For behavior common to all GKE Apps, see create-gke-app.spec
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
    onClickDeleteGkeApp: jest.fn(),
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

  it('should display a cost of $0.40 per hour when running and $0.20 per hour when paused', async () => {
    await component();
    expect(screen.queryByLabelText('cost while running')).toHaveTextContent(
      '$0.40 per hour'
    );
    expect(screen.queryByLabelText('cost while paused')).toHaveTextContent(
      '$0.20 per hour'
    );
  });
});
