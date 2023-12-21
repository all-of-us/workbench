import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  BillingStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { UIAppType } from 'app/components/apps-panel/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  clearCompoundRuntimeOperations,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import {
  ConfigurationPanel,
  ConfigurationPanelProps,
} from './configuration-panel';

describe('ConfigurationPanel', () => {
  let defaultProps: ConfigurationPanelProps;
  let workspacesApiStub: WorkspacesApiStub;
  let onClose: () => void;

  const component = async (propOverrides?: object) =>
    render(<ConfigurationPanel {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    onClose = jest.fn();
    defaultProps = {
      onClose,
      appType: UIAppType.JUPYTER,
    };

    serverConfigStore.set({ config: defaultServerConfig });
    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('should render disabled panel when creator billing disabled', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingStatus: BillingStatus.INACTIVE,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    await component();

    const disabledPanel = screen.queryByText(
      'Cloud services are disabled for this workspace.'
    );
    expect(disabledPanel).not.toBeNull();
  });

  it('should not render disabled panel when creator billing is enabled', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingStatus: BillingStatus.ACTIVE,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    await component();
    const disabledPanel = screen.queryByText(
      'Cloud services are disabled for this workspace.'
    );
    expect(disabledPanel).toBeNull();
  });
});
