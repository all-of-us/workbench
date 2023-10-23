import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus, WorkspaceAccessLevel } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { allMachineTypes } from 'app/utils/machines';
import { toAnalysisConfig } from 'app/utils/runtime-utils';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';

import { CustomizePanel, CustomizePanelProps } from './customize-panel';

const defaultAnalysisConfig = toAnalysisConfig(
  defaultGceRuntimeWithPd(),
  stubDisk()
);

const onClose = jest.fn();
const requestAnalysisConfig = jest.fn();
const setAnalysisConfig = jest.fn();
const setPanelContent = jest.fn();
const setRuntimeStatusRequest = jest.fn();

const defaultProps: CustomizePanelProps = {
  allowDataproc: true,
  analysisConfig: defaultAnalysisConfig,
  attachedPdExists: true,
  creatorFreeCreditsRemaining: 0,
  currentRuntime: defaultGceRuntimeWithPd(),
  environmentChanged: false,
  errorMessageContent: [],
  existingAnalysisConfig: defaultAnalysisConfig,
  gcePersistentDisk: stubDisk(),
  onClose,
  profile: ProfileStubVariables.PROFILE_STUB,
  requestAnalysisConfig,
  runtimeCanBeCreated: true,
  runtimeCanBeUpdated: true,
  runtimeExists: true,
  runtimeStatus: RuntimeStatus.RUNNING,
  setAnalysisConfig,
  setPanelContent,
  setRuntimeStatusRequest,
  updateMessaging: {
    applyAction: 'APPLY',
  },
  validMainMachineTypes: allMachineTypes,
  warningMessageContent: [],
  workspaceData: {
    ...buildWorkspaceStub(),
    accessLevel: WorkspaceAccessLevel.OWNER,
  },
};

describe(CustomizePanel.name, () => {
  const component = async (propOverrides?: Partial<CustomizePanelProps>) =>
    render(<CustomizePanel {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('should render', async () => {
    await component();
    expect(screen.queryByText(/Cloud compute profile/)).toBeInTheDocument();
  });
});
