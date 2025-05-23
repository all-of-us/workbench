import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus, WorkspaceAccessLevel } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  toAnalysisConfig,
  withAnalysisConfigDefaults,
} from 'app/utils/analysis-config';
import { AutopauseMinuteThresholds, ComputeType } from 'app/utils/machines';
import { runtimePresets } from 'app/utils/runtime-presets';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectDropdownDisabled,
  getDropdownOption,
} from 'testing/react-test-helpers';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { defaultGceRuntimeWithPd } from 'testing/stubs/runtime-api-stub';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';
import { ALL_RUNTIME_STATUSES, minus } from 'testing/utils';

import { CustomizePanel, CustomizePanelProps } from './customize-panel';
import { PanelContent } from './utils';

jest.mock(
  'app/components/runtime-configuration-panel/gpu-config-selector',
  () => {
    return {
      GpuConfigSelector: () => <div>Mock GpuConfigSelector</div>,
    };
  }
);
jest.mock(
  'app/components/runtime-configuration-panel/dataproc-config-selector',
  () => {
    return {
      DataProcConfigSelector: () => <div>Mock DataProcConfigSelector</div>,
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
  creatorInitialCreditsRemaining: 0,
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

  test.each([[undefined], [null], [[]]])(
    'it does not display runtime errors if the errors object is %s',
    async (errors) => {
      const currentRuntime = {
        ...defaultGceRuntimeWithPd(),
        errors,
      };
      await component({ currentRuntime });
      expect(
        screen.queryByText(
          /An error was encountered with your cloud environment/
        )
      ).not.toBeInTheDocument();
    }
  );

  it('allows changing the ComputeType from Standard to Dataproc', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard, // already the case, but make it explicit
    };
    const user = userEvent.setup();
    const { container } = await component({
      analysisConfig,
      allowDataproc: true,
    });

    const dataprocOption = await getDropdownOption(
      container,
      'runtime-compute',
      ComputeType.Dataproc,
      2
    );
    user.click(dataprocOption);
    await waitFor(() => {
      expect(setAnalysisConfig).toHaveBeenCalledWith(
        withAnalysisConfigDefaults(
          { ...analysisConfig, computeType: ComputeType.Dataproc },
          stubDisk()
        )
      );
    });
  });

  it('allows changing the ComputeType from Dataproc to Standard when no PD exists', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    const user = userEvent.setup();
    const gcePersistentDisk = undefined;

    const { container } = await component({
      analysisConfig,
      allowDataproc: true,
      gcePersistentDisk,
    });

    const standardOption = await getDropdownOption(
      container,
      'runtime-compute',
      ComputeType.Standard,
      2
    );
    user.click(standardOption);
    await waitFor(() => {
      expect(setAnalysisConfig).toHaveBeenCalledWith(
        withAnalysisConfigDefaults(
          { ...analysisConfig, computeType: ComputeType.Standard },
          gcePersistentDisk
        )
      );
    });
  });

  it('allows changing the ComputeType from Dataproc to Standard when a PD exists', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    const user = userEvent.setup();

    const name = 'existing-disk-name';
    const size = 234;
    const gcePersistentDisk = { ...stubDisk(), name, size };

    const { container } = await component({
      analysisConfig,
      allowDataproc: true,
      gcePersistentDisk,
    });

    const standardOption = await getDropdownOption(
      container,
      'runtime-compute',
      ComputeType.Standard,
      2
    );
    user.click(standardOption);
    await waitFor(() => {
      expect(setAnalysisConfig).toHaveBeenCalledWith(
        withAnalysisConfigDefaults(
          {
            ...analysisConfig,
            computeType: ComputeType.Standard,
            diskConfig: {
              ...analysisConfig.diskConfig,
              size: gcePersistentDisk.size,
              existingDiskName: gcePersistentDisk.name,
            },
          },
          gcePersistentDisk
        )
      );
    });
  });

  test.each([
    [
      'computeType is Standard and allowDataproc is false',
      {
        analysisConfig: {
          ...defaultAnalysisConfig,
          computeType: ComputeType.Standard, // already the case, but make it explicit
        },
        allowDataproc: false,
      },
    ],
    [
      'computeType is Dataproc and allowDataproc is false',
      {
        analysisConfig: {
          ...defaultAnalysisConfig,
          computeType: ComputeType.Dataproc,
        },
        allowDataproc: false,
      },
    ],
    [
      'computeType is Standard and runtimeStatus is not RUNNING or STOPPED',
      {
        analysisConfig: {
          ...defaultAnalysisConfig,
          computeType: ComputeType.Standard, // already the case, but make it explicit
        },
        allowDataproc: true,
        runtimeStatus: RuntimeStatus.CREATING,
      },
    ],
    [
      'computeType is Dataproc and runtimeStatus is not RUNNING or STOPPED',
      {
        analysisConfig: {
          ...defaultAnalysisConfig,
          computeType: ComputeType.Dataproc,
        },
        allowDataproc: true,
        runtimeStatus: RuntimeStatus.CREATING,
      },
    ],
  ])(
    'does not allow changing the ComputeType when %s',
    async (_, propOverrides: Partial<CustomizePanelProps>) => {
      const { container } = await component(propOverrides);
      expectDropdownDisabled(container, 'runtime-compute');
    }
  );

  it('allows changing autopause values', async () => {
    const user = userEvent.setup();

    // arbitrary - can be anything but the first (default) value
    const [secondValue, secondLabel] = Array.from(AutopauseMinuteThresholds)[1];

    const { container } = await component();

    const secondOptionOf4 = await getDropdownOption(
      container,
      'runtime-autopause',
      secondLabel,
      AutopauseMinuteThresholds.size
    );
    user.click(secondOptionOf4);
    await waitFor(() => {
      expect(setAnalysisConfig).toHaveBeenCalledWith(
        withAnalysisConfigDefaults(
          { ...defaultAnalysisConfig, autopauseThreshold: secondValue },
          stubDisk()
        )
      );
    });
  });

  it('does not allow changing the autopause when runtimeStatus is not RUNNING or STOPPED', async () => {
    const { container } = await component({
      runtimeStatus: RuntimeStatus.CREATING,
    });
    expectDropdownDisabled(container, 'runtime-autopause');
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

  it('renders a DataProcConfigSelector for ComputeType.Dataproc', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
    };
    await component({ analysisConfig });
    expect(
      screen.queryByText(/Mock DataProcConfigSelector/)
    ).toBeInTheDocument();
  });

  it('does not render a DataProcConfigSelector for ComputeType.Standard', async () => {
    const analysisConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard, // already the case, but make it explicit
    };
    await component({ analysisConfig });
    expect(
      screen.queryByText(/Mock DataProcConfigSelector/)
    ).not.toBeInTheDocument();
  });

  it('shows an updateMessaging.warn message if it exists and the runtime does', async () => {
    const warn = 'You Have Been Warned!';
    const updateMessaging = {
      applyAction: 'not relevant for this test',
      warn,
    };
    await component({ runtimeExists: true, updateMessaging });
    expect(screen.queryByText(warn)).toBeInTheDocument();
  });

  it('does not show an updateMessaging.warn message if the runtime does not exist', async () => {
    const warn = 'You Have Been Warned AGAIN';
    const updateMessaging = {
      applyAction: 'not relevant for this test',
      warn,
    };
    await component({ runtimeExists: false, updateMessaging });
    expect(screen.queryByText(warn)).not.toBeInTheDocument();
  });

  it('shows errorMessageContent messages if they exist', async () => {
    const errorMessageContent = [
      <div>Error Number One</div>,
      <div>Error Number Two</div>,
    ];
    await component({ errorMessageContent });
    expect(screen.queryByText(/Error Number One/)).toBeInTheDocument();
    expect(screen.queryByText(/Error Number Two/)).toBeInTheDocument();
  });

  it('shows warningMessageContent messages if they exist', async () => {
    const warningMessageContent = [
      <div>Warning Number One</div>,
      <div>Warning Number Two</div>,
    ];
    await component({ warningMessageContent });
    expect(screen.queryByText(/Warning Number One/)).toBeInTheDocument();
    expect(screen.queryByText(/Warning Number Two/)).toBeInTheDocument();
  });

  // many subcomponents are enabled/disabled by the same logic.  PresetSelector can act as a stand-in here for:
  // MachineSelector, GpuConfigSelector, DataProcConfigSelector, the autopause Dropdown, DiskSelector, and
  // Delete Environment in CustomizePanelFooter.
  // The runtime-compute Dropdown has similar logic, but only when allowDataproc is also true
  it('enables the PresetSelector when no runtimeExists, regardless of runtimeStatus', async () => {
    await component({
      runtimeExists: false,
      runtimeStatus: undefined,
    });
    const dropdown = screen.queryByLabelText('Recommended environments');
    expect(dropdown).toBeInTheDocument();
    dropdown.click();
    await waitFor(() => {
      expect(
        screen.queryByLabelText(runtimePresets().hailAnalysis.displayName)
      ).toBeInTheDocument();
      expect(
        screen.queryByLabelText(runtimePresets().generalAnalysis.displayName)
      ).toBeInTheDocument();
    });
  });

  const enabledStatuses = [RuntimeStatus.RUNNING, RuntimeStatus.STOPPED];
  test.each(enabledStatuses)(
    'it enables the PresetSelector for a %s runtime when runtimeExists',
    async (runtimeStatus) => {
      await component({
        runtimeExists: true,
        runtimeStatus,
      });
      const dropdown = screen.queryByLabelText('Recommended environments');
      expect(dropdown).toBeInTheDocument();
      dropdown.click();
      await waitFor(() => {
        expect(
          screen.queryByLabelText(runtimePresets().hailAnalysis.displayName)
        ).toBeInTheDocument();
        expect(
          screen.queryByLabelText(runtimePresets().generalAnalysis.displayName)
        ).toBeInTheDocument();
      });
    }
  );

  test.each(minus(ALL_RUNTIME_STATUSES, enabledStatuses))(
    'it disables the PresetSelector for a %s runtime when runtimeExists',
    async (runtimeStatus) => {
      await component({
        runtimeExists: true,
        runtimeStatus,
      });
      const dropdown = screen.queryByLabelText('Recommended environments');
      expect(dropdown).toBeInTheDocument();
      dropdown.click();
      await waitFor(() => {
        expect(
          screen.queryByLabelText(runtimePresets().hailAnalysis.displayName)
        ).not.toBeInTheDocument();
        expect(
          screen.queryByLabelText(runtimePresets().generalAnalysis.displayName)
        ).not.toBeInTheDocument();
      });
    }
  );
});
