import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { ComputeType } from 'app/utils/machines';
import { runtimePresets } from 'app/utils/runtime-presets';
import { PanelContent, toAnalysisConfig } from 'app/utils/runtime-utils';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  defaultDataProcRuntime,
  defaultGceRuntimeWithPd,
} from 'testing/stubs/runtime-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';

import { CreatePanel, CreatePanelProps } from './create-panel';

const defaultAnalysisConfig = toAnalysisConfig(
  defaultGceRuntimeWithPd(),
  stubDisk()
);

const setPanelContent = jest.fn();
const setRuntimeStatus = jest.fn();

const defaultProps: CreatePanelProps = {
  profile: ProfileStubVariables.PROFILE_STUB,
  workspace: buildWorkspaceStub(),
  analysisConfig: defaultAnalysisConfig,
  creatorFreeCreditsRemaining: 0,
  status: RuntimeStatus.RUNNING,
  setPanelContent,
  setRuntimeStatus,
};

describe(CreatePanel.name, () => {
  const component = async (propOverrides?: Partial<CreatePanelProps>) =>
    render(<CreatePanel {...{ ...defaultProps, ...propOverrides }} />);

  beforeEach(async () => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  test.each([
    [
      ComputeType.Dataproc,
      toAnalysisConfig(defaultDataProcRuntime(), undefined),
      `Recommended Environment for ${runtimePresets.hailAnalysis.displayName}`,
    ],
    [
      ComputeType.Standard,
      toAnalysisConfig(defaultGceRuntimeWithPd(), stubDisk()),
      `Recommended Environment for ${runtimePresets.generalAnalysis.displayName}`,
    ],
  ])(
    'should recommend an appropriate preset for the %s computeType',
    async (computeType, analysisConfig, expectedText) => {
      await component({ analysisConfig });
      expect(screen.queryByText(expectedText)).toBeInTheDocument();
    }
  );

  it('should allow customization', async () => {
    await component();

    const customizeButton = screen.queryByRole('button', { name: 'Customize' });
    expect(customizeButton).toBeInTheDocument();
    customizeButton.click();
    await waitFor(() =>
      expect(setPanelContent).toHaveBeenCalledWith(PanelContent.Customize)
    );
  });
});
