import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus, WorkspaceAccessLevel } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { allMachineTypes, ComputeType } from 'app/utils/machines';
import { PanelContent, toAnalysisConfig } from 'app/utils/runtime-utils';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';

import { CustomizePanel, CustomizePanelProps } from './customize-panel';

class MockGpuConfigSelector extends React.Component {
  render() {
    return <div>Mock GpuConfigSelector</div>;
  }
}
jest.mock(
  'app/components/runtime-configuration-panel/gpu-config-selector',
  () => {
    return {
      GpuConfigSelector: () => <MockGpuConfigSelector />,
    };
  }
);

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

  it('displays runtime errors if they exist', async () => {
    const currentRuntime = {
      ...defaultGceRuntimeWithPd(),
      errors: [{ errorMessage: 'the runtime failed' }],
    };
    await component({ currentRuntime });
    expect(
      screen.queryByText(/An error was encountered with your cloud environment/)
    ).toBeInTheDocument();
  });

  // for some reason I was unable to get this to work as a test.each()

  it('does not display runtime errors if the error object is undefined', async () => {
    const currentRuntime = {
      ...defaultGceRuntimeWithPd(),
      errors: undefined,
    };
    await component({ currentRuntime });
    expect(
      screen.queryByText(/An error was encountered with your cloud environment/)
    ).not.toBeInTheDocument();
  });

  it('does not display runtime errors if the error object is null', async () => {
    const currentRuntime = {
      ...defaultGceRuntimeWithPd(),
      errors: null,
    };
    await component({ currentRuntime });
    expect(
      screen.queryByText(/An error was encountered with your cloud environment/)
    ).not.toBeInTheDocument();
  });

  it('does not display runtime errors if the error object is empty', async () => {
    const currentRuntime = {
      ...defaultGceRuntimeWithPd(),
      errors: [],
    };
    await component({ currentRuntime });
    expect(
      screen.queryByText(/An error was encountered with your cloud environment/)
    ).not.toBeInTheDocument();
  });

  it('renders a GpuConfigSelector for ComputeType.Standard', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard, // already the case, but make it explicit
    };
    await component({ analysisConfig });
    expect(screen.queryByText(/Mock GpuConfigSelector/)).toBeInTheDocument();
  });

  it('does not render a GpuConfigSelector for ComputeType.Dataproc', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    await component({ analysisConfig });
    expect(
      screen.queryByText(/Mock GpuConfigSelector/)
    ).not.toBeInTheDocument();
  });

  it('should allow access to the Spark console for ComputeType.Dataproc', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    const existingAnalysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    // TODO why do we need both analysisConfig and existingAnalysisConfig here?
    await component({ analysisConfig, existingAnalysisConfig });
    const sparkButton = screen.queryByRole('button', {
      name: 'Manage and monitor Spark console',
    });
    await waitFor(() => expect(sparkButton).toBeInTheDocument());
    screen.debug();
    sparkButton.click();
    await waitFor(() =>
      expect(setPanelContent).toHaveBeenCalledWith(PanelContent.SparkConsole)
    );
  });

  it('should not allow access to the Spark console for ComputeType.Standard', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard, // already the case, but make it explicit
    };
    const existingAnalysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard, // already the case, but make it explicit
    };
    await component({ analysisConfig, existingAnalysisConfig });
    const sparkButton = screen.queryByRole('button', {
      name: 'Manage and monitor Spark console',
    });
    expect(sparkButton).not.toBeInTheDocument();
  });
});
