import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  BillingStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { clearCompoundRuntimeOperations } from 'app/utils/stores';

import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
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

  const component = async (propOverrides?: object) => {
    const allProps = { ...defaultProps, ...propOverrides };
    const c = mountWithRouter(<ConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
  };

  beforeEach(async () => {
    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    onClose = jest.fn();
    defaultProps = {
      onClose,
      appType: UIAppType.JUPYTER,
    };

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
    const wrapper = await component();

    const disabledPanel = wrapper.find({
      'data-test-id': 'environment-disabled-panel',
    });
    expect(disabledPanel.exists()).toBeTruthy();
  });

  it('should not render disabled panel when creator billing is enabled', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingStatus: BillingStatus.ACTIVE,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    const wrapper = await component();

    const disabledPanel = wrapper.find({
      'data-test-id': 'environment-disabled-panel',
    });
    expect(disabledPanel.exists()).toBeFalsy();
  });
});
