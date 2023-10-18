import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { toAnalysisConfig } from 'app/utils/runtime-utils';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';

import { CreatePanel, CreatePanelProps } from './create-panel';

const defaultRuntime = defaultGceRuntimeWithPd();
const defaultDisk = stubDisk();
const defaultProps: CreatePanelProps = {
  profile: ProfileStubVariables.PROFILE_STUB,
  workspace: buildWorkspaceStub(),
  analysisConfig: toAnalysisConfig(defaultRuntime, defaultDisk),
  creatorFreeCreditsRemaining: 0,
  status: RuntimeStatus.RUNNING,
  setPanelContent: jest.fn(),
  setRuntimeStatus: jest.fn(),
};

describe(CreatePanel.name, () => {
  const component = async (propOverrides?: Partial<CreatePanelProps>) =>
    render(<CreatePanel {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('should render', async () => {
    await component();
    expect(
      screen.queryByText(/Recommended Environment for/)
    ).toBeInTheDocument();
  });
});
