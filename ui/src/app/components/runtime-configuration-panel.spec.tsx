import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router';
import { parseInt } from 'lodash';

import {
  Disk,
  DisksApi,
  DiskType,
  GpuConfig,
  Runtime,
  RuntimeApi,
  RuntimeStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AnalysisConfig, toAnalysisConfig } from 'app/utils/analysis-config';
import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  findMachineByName,
  Machine,
  MIN_DISK_SIZE_GB,
} from 'app/utils/machines';
import { currentWorkspaceStore } from 'app/utils/navigation';
import * as runtimeHooks from 'app/utils/runtime-hooks';
import { runtimePresets } from 'app/utils/runtime-presets';
import { diskTypeLabels } from 'app/utils/runtime-utils';
import {
  cdrVersionStore,
  compoundRuntimeOpStore,
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectDropdown,
  getDropdownOption,
  getDropdownSelection,
} from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  defaultDataprocConfig,
  defaultDataProcRuntime,
  defaultGceConfig,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
  defaultRuntime,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import {
  createOrCustomize,
  getErrorsAndWarnings,
  RuntimeConfigurationPanel,
  RuntimeConfigurationPanelProps,
} from './runtime-configuration-panel';
import { PanelContent } from './runtime-configuration-panel/utils';

const setup = () => {
  serverConfigStore.set({ config: defaultServerConfig });
};

describe(createOrCustomize.name, () => {
  beforeEach(() => {
    setup();
  });
  it('returns Customize when a pendingRuntime exists', () => {
    expect(
      createOrCustomize({
        pendingRuntime: defaultRuntime(),
        currentRuntime: undefined,
        runtimeStatus: undefined,
      })
    ).toEqual(PanelContent.Customize);
  });

  test.each([
    ['null', null, undefined],
    ['undefined', undefined, undefined],
    ['UNKNOWN', defaultRuntime(), RuntimeStatus.UNKNOWN],
  ])(
    'it returns Create for a %s runtime',
    (desc, currentRuntime, runtimeStatus) => {
      expect(
        createOrCustomize({
          pendingRuntime: undefined,
          currentRuntime,
          runtimeStatus,
        })
      ).toEqual(PanelContent.Create);
    }
  );
});

describe(getErrorsAndWarnings.name, () => {
  beforeEach(() => {
    setup();
  });
  it('should show no errors or warnings by default', () => {
    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      {
        usingInitialCredits: true,
        analysisConfig: toAnalysisConfig(defaultRuntime(), stubDisk()),
      }
    );

    expect(errorMessageContent).toEqual([]);
    expect(warningMessageContent).toEqual([]);
  });

  const minDiskGce = MIN_DISK_SIZE_GB;
  const minDiskDataProc = DATAPROC_MIN_DISK_SIZE_GB;
  const maxDiskInitialCredits = 4000;
  const maxDiskBYOBilling = 64000;
  const defaultGceAc = toAnalysisConfig(defaultGceRuntime(), undefined);
  const defaultDataProcAc = toAnalysisConfig(
    defaultDataProcRuntime(),
    undefined
  );

  it.each([
    [
      'too-small GCE standard disk when using initial credits',
      'Disk size',
      true,
      {
        ...defaultGceAc,
        diskConfig: { ...defaultGceAc.diskConfig, size: minDiskGce - 1 },
      },
    ],
    [
      'too-small GCE standard disk when not using initial credits',
      'Disk size',
      false,
      {
        ...defaultGceAc,
        diskConfig: { ...defaultGceAc.diskConfig, size: minDiskGce - 1 },
      },
    ],
    [
      'too-large GCE standard disk when using initial credits',
      'Disk size',
      true,
      {
        ...defaultGceAc,
        diskConfig: {
          ...defaultGceAc.diskConfig,
          size: maxDiskInitialCredits + 1,
        },
      },
    ],
    [
      'too-large GCE standard disk when not using initial credits',
      'Disk size',
      false,
      {
        ...defaultGceAc,
        diskConfig: { ...defaultGceAc.diskConfig, size: maxDiskBYOBilling + 1 },
      },
    ],
    [
      'too-small DataProc master disk when using initial credits',
      'Master disk size',
      true,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          masterDiskSize: minDiskDataProc - 1,
        },
      },
    ],
    [
      'too-small DataProc master disk when using BYOBilling credits',
      'Master disk size',
      false,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          masterDiskSize: minDiskDataProc - 1,
        },
      },
    ],
    [
      'too-large DataProc master disk when using initial credits',
      'Master disk size',
      true,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          masterDiskSize: maxDiskInitialCredits + 1,
        },
      },
    ],
    [
      'too-large DataProc master disk when using BYOBilling credits',
      'Master disk size',
      false,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          masterDiskSize: maxDiskBYOBilling + 1,
        },
      },
    ],
    [
      'too-small DataProc worker disk when using initial credits',
      'Worker disk size',
      true,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          workerDiskSize: minDiskDataProc - 1,
        },
      },
    ],
    [
      'too-small DataProc worker disk when using BYOBilling credits',
      'Worker disk size',
      false,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          workerDiskSize: minDiskDataProc - 1,
        },
      },
    ],
    [
      'too-large DataProc worker disk when using initial credits',
      'Worker disk size',
      true,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          workerDiskSize: maxDiskInitialCredits + 1,
        },
      },
    ],
    [
      'too-large DataProc worker disk when using BYOBilling credits',
      'Worker disk size',
      false,
      {
        ...defaultDataProcAc,
        dataprocConfig: {
          ...defaultDataProcAc.dataprocConfig,
          workerDiskSize: maxDiskBYOBilling + 1,
        },
      },
    ],
  ])(
    'it should show a disk error for a %s',
    (
      desc,
      diskSizeDesc: string,
      usingInitialCredits: boolean,
      analysisConfig: AnalysisConfig
    ) => {
      const minDisk = analysisConfig.dataprocConfig
        ? minDiskDataProc
        : minDiskGce;
      const maxDisk = usingInitialCredits
        ? maxDiskInitialCredits
        : maxDiskBYOBilling;
      const expectedError = `${diskSizeDesc} must be between ${minDisk} and ${maxDisk} GB`;

      const { errorMessageContent, warningMessageContent } =
        getErrorsAndWarnings({ usingInitialCredits, analysisConfig });

      expect(warningMessageContent).toEqual([]);
      expect(errorMessageContent).toHaveLength(1);
      expect(errorMessageContent[0].props.children).toEqual(expectedError);
    }
  );

  it('should show an error for fewer than 2 dataproc workers', () => {
    const usingInitialCredits = true; // not relevant here
    const analysisConfig = {
      ...defaultDataProcAc,
      dataprocConfig: {
        ...defaultDataProcAc.dataprocConfig,
        numberOfWorkers: 1,
      },
    };
    const expectedError = 'Dataproc requires at least 2 worker nodes';

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig }
    );

    expect(warningMessageContent).toEqual([]);
    expect(errorMessageContent).toHaveLength(1);
    expect(errorMessageContent[0].props.children).toEqual(expectedError);
  });

  it('should show a cost warning when the user has enough remaining initial credits', () => {
    const usingInitialCredits = true;
    const creatorInitialCreditsRemaining =
      serverConfigStore.get().config.defaultInitialCreditsDollarLimit + 1;

    // want a running cost over $25/hr

    const machine: Machine = findMachineByName('n1-highmem-96'); // most expensive, at about $5.70
    const gpuConfig: GpuConfig = { gpuType: 'nvidia-tesla-v100', numOfGpus: 8 }; // nearly $20 by itself
    const analysisConfig = { ...defaultGceAc, machine, gpuConfig };

    const expectedWarning =
      'Your runtime is expensive. Are you sure you wish to proceed?';

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig, creatorInitialCreditsRemaining }
    );

    expect(errorMessageContent).toEqual([]);
    expect(warningMessageContent).toHaveLength(1);
    expect(warningMessageContent[0].props.children).toEqual(expectedWarning);
  });

  it('should show a cost error when the user does not have enough remaining initial credits', () => {
    const usingInitialCredits = true;
    const creatorInitialCreditsRemaining =
      serverConfigStore.get().config.defaultInitialCreditsDollarLimit - 1;

    // want a running cost over $25/hr

    const machine: Machine = findMachineByName('n1-highmem-96'); // most expensive, at about $5.70
    const gpuConfig: GpuConfig = { gpuType: 'nvidia-tesla-v100', numOfGpus: 8 }; // nearly $20 by itself
    const analysisConfig = { ...defaultGceAc, machine, gpuConfig };

    const expectedError =
      'Your runtime is too expensive. To proceed using free credits, reduce your running costs below'; // $cost

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig, creatorInitialCreditsRemaining }
    );

    expect(warningMessageContent).toEqual([]);
    expect(errorMessageContent).toHaveLength(1);
    expect(errorMessageContent[0].props.children).toContain(expectedError);
  });

  it('should have a higher warning threshold when using BYOBilling', () => {
    const usingInitialCredits = false;

    // over the initial-credits threshold of $25/hr but less than $150/hr

    const machine: Machine = findMachineByName('n1-highmem-96'); // most expensive, at about $5.70
    const gpuConfig: GpuConfig = { gpuType: 'nvidia-tesla-v100', numOfGpus: 8 }; // nearly $20 by itself
    const analysisConfig = { ...defaultGceAc, machine, gpuConfig };

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig }
    );

    expect(warningMessageContent).toEqual([]);
    expect(errorMessageContent).toEqual([]);
  });

  it('should return a cost warning when passing the higher BYOBilling threshold', () => {
    const usingInitialCredits = false;

    // over $150/hr

    const analysisConfig = {
      ...defaultDataProcAc,
      dataprocConfig: {
        ...defaultDataProcAc.dataprocConfig,
        workerMachineType: 'n1-highmem-96', // most expensive, at about $5.70
        numberOfWorkers: 30, // so 30 x $5.70 for the workers alone
      },
    };

    const expectedWarning =
      'Your runtime is expensive. Are you sure you wish to proceed?';

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig }
    );

    expect(errorMessageContent).toEqual([]);
    expect(warningMessageContent).toHaveLength(1);
    expect(warningMessageContent[0].props.children).toEqual(expectedWarning);
  });
});

describe('RuntimeConfigurationPanel', () => {
  let runtimeApiStub: RuntimeApiStub;
  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;
  let user: UserEvent;

  beforeEach(() => {
    setup();
  });

  const onClose = jest.fn();
  const defaultProps: RuntimeConfigurationPanelProps = {
    onClose,
    profileState: {
      profile: ProfileStubVariables.PROFILE_STUB,
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    },
  };
  const runtimeDiskStoreStub = {
    workspaceNamespace: workspaceStubs[0].namespace,
    gcePersistentDisk: null,
    gcePersistentDiskLoaded: true,
  };
  runtimeDiskStore.set(runtimeDiskStoreStub);

  const setCurrentRuntime = (runtime: Runtime) => {
    runtimeApiStub.runtime = runtime;
    runtimeStore.set({ ...runtimeStore.get(), runtime });
  };

  const setCurrentDisk = (d: Disk) => {
    disksApiStub.disk = d;
    runtimeDiskStoreStub.gcePersistentDisk = d;
    runtimeDiskStore.set(runtimeDiskStoreStub);
  };

  const existingDisk = (): Disk => ({
    ...stubDisk(),
    size: 1000,
    diskType: DiskType.STANDARD,
    name: 'my-existing-disk',
    gceRuntime: true,
    zone: 'us-central1-a',
  });

  const detachableDiskRuntime = (): Runtime => {
    const { size, diskType, name } = existingDisk();
    return {
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      gceWithPdConfig: {
        machineType: 'n1-standard-16',
        persistentDisk: {
          size,
          diskType,
          name,
          labels: {},
        },
        gpuConfig: null,
        zone: 'us-central1-a',
      },
      gceConfig: null,
      dataprocConfig: null,
    };
  };

  const clickExpectedButton = async (name: string) => {
    const button = screen.getByRole('button', { name });
    expect(button).toBeInTheDocument();
    expectButtonElementEnabled(button);
    await user.click(button);
  };

  const pickDropdownOptionAndClick = async (
    container: HTMLElement,
    dropDownId: string,
    optionText: string
  ): Promise<void> => {
    const option = await getDropdownOption(container, dropDownId, optionText);
    await user.click(option);
  };

  const pickSpinButtonSize = async (
    name: string,
    value: number
  ): Promise<void> => {
    const spinButton = screen.getByRole('spinbutton', { name });
    expect(spinButton).toBeInTheDocument();
    await user.clear(spinButton);
    await user.type(spinButton, value.toString());
  };

  const pickMainCpu = (container: HTMLElement, option: number): Promise<void> =>
    pickDropdownOptionAndClick(container, 'runtime-cpu', option.toString());

  const pickDetachableType = (
    container: HTMLElement,
    diskType: DiskType
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      'disk-type',
      diskTypeLabels[diskType]
    );

  const getMainCpu = (container: HTMLElement): string =>
    getDropdownSelection(container, 'runtime-cpu');

  const getMainRam = (container: HTMLElement): string =>
    getDropdownSelection(container, 'runtime-ram');

  const getWorkerCpu = (container: HTMLElement): string =>
    getDropdownSelection(container, 'worker-cpu');

  const getWorkerRam = (container: HTMLElement): string =>
    getDropdownSelection(container, 'worker-ram');

  const pickComputeType = (
    container: HTMLElement,
    computeType: ComputeType
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      'runtime-compute',
      computeType.toString()
    );

  const pickMainRam = (container: HTMLElement, option: number): Promise<void> =>
    pickDropdownOptionAndClick(container, 'runtime-ram', option.toString());

  const pickWorkerCpu = (
    container: HTMLElement,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(container, 'worker-cpu', option.toString());

  const pickWorkerRam = (
    container: HTMLElement,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(container, 'worker-ram', option.toString());

  const pickDetachableDiskSize = async (size: number): Promise<void> =>
    pickSpinButtonSize('detachable-disk', size);

  const pickStandardDiskSize = async (size: number): Promise<void> =>
    pickSpinButtonSize('standard-disk', size);

  const pickWorkerDiskSize = async (size: number): Promise<void> =>
    pickSpinButtonSize('worker-disk', size);

  const pickNumWorkers = async (size: number): Promise<void> =>
    pickSpinButtonSize('num-workers', size);

  const pickNumPreemptibleWorkers = async (size: number): Promise<void> =>
    pickSpinButtonSize('num-preemptible', size);

  const pickPresets = (container: HTMLElement, option: string): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      'runtime-presets-menu',
      option.toString()
    );

  const pickGpuType = (container: HTMLElement, option: string): Promise<void> =>
    pickDropdownOptionAndClick(container, 'gpu-type', option);

  const pickGpuNum = (container: HTMLElement, option: number): Promise<void> =>
    pickDropdownOptionAndClick(container, 'gpu-num', option.toString());

  const spinDiskElement = (diskName: string): HTMLInputElement =>
    screen.getByRole('spinbutton', {
      name: diskName,
    });

  const getMasterDiskValue = () =>
    spinDiskElement('standard-disk').getAttribute('value');

  const getDetachableDiskValue = () =>
    spinDiskElement('detachable-disk').getAttribute('value');

  const getWorkerDiskValue = () =>
    spinDiskElement('worker-disk').getAttribute('value');

  const getNumOfWorkersValue = () =>
    spinDiskElement('num-workers').getAttribute('value');

  const getNumOfPreemptibleWorkersValue = () =>
    spinDiskElement('num-preemptible').getAttribute('value');

  const getDeletePDRadio = () =>
    screen.getByRole('radio', {
      name: /delete persistent disk/i,
    });

  const togglePDRadioButton = async () => {
    await user.click(getDeletePDRadio());
  };

  const confirmDeleteText =
    'You’re about to delete your cloud analysis environment.';

  const expectConfirmDeletePanel = () =>
    expect(screen.queryByText(confirmDeleteText)).not.toBeNull();

  const component = (
    propOverrides?: Partial<RuntimeConfigurationPanelProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(
      <MemoryRouter>
        <RuntimeConfigurationPanel {...allProps} />
      </MemoryRouter>
    );
  };

  const getRunningCost = () =>
    screen.getByLabelText('cost while running').textContent;

  const getPausedCost = () =>
    screen.getByLabelText('cost while paused').textContent;

  let mockSetRuntimeRequest;
  const firstCall = 0;
  const firstParameter = 0;

  beforeEach(async () => {
    user = userEvent.setup();
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
    const workspace = workspaceStubs[0];
    const oneMinute = 60 * 1000;

    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    currentWorkspaceStore.next({
      ...workspace,
      initialCredits: {
        ...workspace.initialCredits,
        expirationEpochMillis: new Date().getTime() + 2 * oneMinute,
      },
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName:
        'billingAccounts/' + defaultServerConfig.initialCreditsBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      googleProject: runtimeApiStub.runtime.googleProject,
    });

    runtimeStore.set({
      runtime: runtimeApiStub.runtime,
      workspaceNamespace: workspace.namespace,
      runtimeLoaded: true,
    });

    runtimeDiskStore.set({
      workspaceNamespace: workspace.namespace,
      gcePersistentDisk: null,
      gcePersistentDiskLoaded: true,
    });

    jest
      .spyOn(runtimeHooks, 'useRuntimeAndDiskStores')
      .mockImplementation(() => ({
        runtimeLoaded: true,
        gcePersistentDiskLoaded: true,
        runtime: runtimeStore.get().runtime,
        gcePersistentDisk: runtimeDiskStore.get().gcePersistentDisk,
        isLoaded: true,
      }));

    mockSetRuntimeRequest = jest.fn();
  });

  afterEach(async () => {
    jest.clearAllMocks();
  });

  const mockUseCustomRuntime = () => {
    jest
      .spyOn(runtimeHooks, 'useCustomRuntime')
      .mockImplementation((currentWorkspaceNamespace: string) => {
        const runtimeOps = useStore(compoundRuntimeOpStore);
        const { pendingRuntime = null } =
          runtimeOps[currentWorkspaceNamespace] || {};
        const { runtime } = useStore(runtimeStore);
        return [
          { currentRuntime: runtime, pendingRuntime: pendingRuntime },
          mockSetRuntimeRequest,
        ];
      });
  };

  it('should show loading spinner while loading', async () => {
    // Override the mock to simulate loading state
    jest
      .spyOn(runtimeHooks, 'useRuntimeAndDiskStores')
      .mockImplementation(() => ({
        runtimeLoaded: false,
        gcePersistentDiskLoaded: false,
        runtime: null,
        gcePersistentDisk: null,
        isLoaded: false,
      }));

    const { container, rerender } = component();
    expect(container).toBeInTheDocument();

    await waitFor(() =>
      // spinner label
      expect(screen.queryByLabelText('Please Wait')).toBeInTheDocument()
    );

    // Now mock it as loaded
    jest
      .spyOn(runtimeHooks, 'useRuntimeAndDiskStores')
      .mockImplementation(() => ({
        runtimeLoaded: true,
        gcePersistentDiskLoaded: true,
        runtime: runtimeStore.get().runtime,
        gcePersistentDisk: runtimeDiskStore.get().gcePersistentDisk,
        isLoaded: true,
      }));

    // Use rerender instead of creating a new component
    rerender(
      <MemoryRouter>
        <RuntimeConfigurationPanel {...defaultProps} />
      </MemoryRouter>
    );

    await waitFor(() =>
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
    );
  });

  it('renders the chosen panel when specified in initialPanelContent', () => {
    const { container } = component({
      initialPanelContent: PanelContent.Create,
    });
    expect(container).toBeInTheDocument();

    expect(
      screen.getByText(
        /Your analysis environment consists of an application and compute resources./
      )
    ).toBeInTheDocument();
  });

  it('should allow creation with defaults when no runtime exists', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    component();

    await clickExpectedButton('Create');

    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceWithPdConfig.machineType
    ).toEqual('n1-standard-4');

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceConfig
    ).toBeUndefined();

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toBeUndefined();
  });

  it('should close panel after creating a runtime', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    component();

    await clickExpectedButton('Create');

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should show customize panel when opened with a running runtime', async () => {
    setCurrentRuntime(defaultGceRuntimeWithPd());
    mockUseCustomRuntime();
    component();

    const button = screen.getByRole('button', { name: /delete environment/i });
    expectButtonElementEnabled(button);
  });

  it('should allow creation when runtime has error status', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.ERROR,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });
    mockUseCustomRuntime();
    component();

    await clickExpectedButton('Try Again');
    expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
  });

  it('should allow creation from error with an update', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.ERROR,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });

    mockUseCustomRuntime();
    const { container } = component();

    await pickMainCpu(container, 8);
    await clickExpectedButton('Try Again');

    expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
  });

  it('should allow creation with GCE with PD config', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');

    await pickMainCpu(container, 8);
    await pickMainRam(container, 52);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB + 10);

    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceWithPdConfig
    ).toEqual({
      machineType: 'n1-highmem-8',
      gpuConfig: null,
      persistentDisk: {
        diskType: DiskType.STANDARD,
        labels: {},
        name: null,
        size: MIN_DISK_SIZE_GB + 10,
      },
      zone: serverConfigStore.get().config.defaultGceVmZone,
    });
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');

    // master settings
    await pickMainCpu(container, 2);
    await pickMainRam(container, 7.5);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB + 10);

    // worker settings
    await pickWorkerCpu(container, 8);
    await pickWorkerRam(container, 30);
    await pickWorkerDiskSize(300);
    await pickNumWorkers(10);
    await pickNumPreemptibleWorkers(20);

    await clickExpectedButton('Create');

    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalled();
    });

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toEqual({
      masterMachineType: 'n1-standard-2',
      masterDiskSize: DATAPROC_MIN_DISK_SIZE_GB + 10,
      workerMachineType: 'n1-standard-8',
      workerDiskSize: 300,
      numberOfWorkers: 10,
      numberOfPreemptibleWorkers: 20,
    });
  });

  it('should disable the Next button if there are no changes and runtime is running', async () => {
    setCurrentRuntime(defaultGceRuntimeWithPd());
    mockUseCustomRuntime();
    component();
    const button = screen.getByRole('button', { name: 'Next' });
    expect(button).toBeInTheDocument();
    expectButtonElementDisabled(button);
  });

  it('should allow runtime deletion', async () => {
    mockUseCustomRuntime();
    component({});
    await clickExpectedButton('Delete Environment');

    // confirm that the correct panel is visible
    await waitFor(() => expectConfirmDeletePanel());
  });

  it('should allow cancelling runtime deletion', async () => {
    mockUseCustomRuntime();
    component({});
    await clickExpectedButton('Delete Environment');

    // confirm that the correct panel is visible
    await waitFor(() => expectConfirmDeletePanel());

    await clickExpectedButton('Cancel');

    await waitFor(() => {
      expect(screen.queryByText(confirmDeleteText)).not.toBeInTheDocument();
    });

    expect(mockSetRuntimeRequest).not.toHaveBeenCalled();
  });

  it('should require PD (prevent standard disk) for GCE', async () => {
    setCurrentRuntime(defaultGceRuntimeWithPd());
    mockUseCustomRuntime();
    component();

    expect(
      screen.queryByText('Reattachable persistent disk')
    ).toBeInTheDocument();
    expect(screen.queryByText('Standard disk')).not.toBeInTheDocument();
  });

  it('should require standard disk / prevent detachable PD use for Dataproc', async () => {
    setCurrentRuntime(defaultDataProcRuntime());
    mockUseCustomRuntime();
    component();

    expect(screen.queryByText('Standard disk')).toBeInTheDocument();
    expect(
      screen.queryByText('Reattachable persistent disk')
    ).not.toBeInTheDocument();
  });

  it('should render Spark console links for a running cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
      gceWithPdConfig: null,
    });
    mockUseCustomRuntime();
    component();
    const manageButton = screen.getByRole('button', {
      name: 'Manage and monitor Spark console',
    });
    expect(manageButton).toBeInTheDocument();
    expectButtonElementEnabled(manageButton);
    manageButton.click();

    await waitFor(() => screen.getByText('MapReduce History Server'));
  });

  it('should disable the Spark console for a non-running cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.STOPPED,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
      gceWithPdConfig: null,
    });
    mockUseCustomRuntime();
    component();
    const manageButton = screen.getByRole('button', {
      name: 'Manage and monitor Spark console',
    });
    expect(manageButton).toBeInTheDocument();
    expectButtonElementDisabled(manageButton);
  });

  it('Should disable standard storage option for existing GCE runtime and have reattachable selected', async () => {
    // set GCE Runtime without PD as current runtime
    setCurrentRuntime(defaultGceRuntime());
    mockUseCustomRuntime();
    component();
    expect(
      screen.queryByText('Reattachable persistent disk')
    ).toBeInTheDocument();
    expect(screen.queryByText('Standard disk')).not.toBeInTheDocument();
  });

  it('should allow configuration via GCE preset', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();

    const CustomizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await user.click(CustomizeButton);

    // Ensure set the form to something non-standard to start
    await pickMainCpu(container, 8);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(MIN_DISK_SIZE_GB + 10);

    // GPU
    await pickPresets(container, runtimePresets().generalAnalysis.displayName);

    clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceWithPdConfig.persistentDisk
    ).toEqual({
      diskType: DiskType.STANDARD,
      labels: {},
      name: null,
      size: MIN_DISK_SIZE_GB,
    });
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toBeFalsy();
  });

  it('should allow configuration via dataproc preset', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();

    const { container } = component();

    const customizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await user.click(customizeButton);

    await pickPresets(container, runtimePresets().hailAnalysis.displayName);

    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toEqual(runtimePresets().hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceConfig
    ).toBeFalsy();
  });

  it('should reattach to an existing disk by default, for deleted VMs', async () => {
    const disk = existingDisk();
    setCurrentDisk(disk);
    setCurrentRuntime(undefined);
    mockUseCustomRuntime();
    component();
    expect(screen.getByText(/1000 gb disk/i)).toBeInTheDocument();
  });

  it('should allow configuration via dataproc preset from modified form', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(container, 2);
    await pickMainRam(container, 7.5);
    await pickDetachableDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    await pickComputeType(container, ComputeType.Dataproc);

    await pickWorkerCpu(container, 8);
    await pickWorkerRam(container, 30);
    await pickWorkerDiskSize(300);
    await pickNumWorkers(10);
    await pickNumPreemptibleWorkers(20);
    await pickPresets(container, runtimePresets().hailAnalysis.displayName);
    await clickExpectedButton('Create');

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .dataprocConfig
    ).toEqual(runtimePresets().hailAnalysis.runtimeTemplate.dataprocConfig);

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceConfig
    ).toBeFalsy();
  });

  it('should tag as user override after preset modification', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();

    const { container } = component();

    await clickExpectedButton('Customize');

    // Take the preset but make a solitary modification.
    await pickPresets(container, runtimePresets().hailAnalysis.displayName);
    await pickNumPreemptibleWorkers(20);

    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });
  });

  it('should tag as preset if configuration matches', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();

    const { container } = component();
    await clickExpectedButton('Customize');

    // Take the preset, make a change, then revert.
    await pickPresets(container, runtimePresets().generalAnalysis.displayName);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickWorkerCpu(container, 2);
    await pickComputeType(container, ComputeType.Standard);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });
  });

  it('should restrict memory options by cpu', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    await screen.findByText('Cloud compute profile');

    await pickMainCpu(container, 8);
    const dropdown: HTMLElement = expectDropdown(container, 'runtime-ram');
    await user.click(dropdown);

    const runtimeRamOptions = Array.from(
      container.querySelectorAll(`#${'runtime-ram'} .p-dropdown-item`)
    ).map((option) => option.textContent);

    expect(runtimeRamOptions).toEqual(['7.2', '30', '52']);
  });

  it('should respect divergent sets of valid machine types across compute types', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    await pickMainCpu(container, 1);
    await pickMainRam(container, 3.75);

    await pickComputeType(container, ComputeType.Dataproc);

    // n1-standard-1 is illegal for Dataproc, so it should restore the default.
    expect(getMainCpu(container)).toBe('4');
    expect(getMainRam(container)).toBe('15');
  });

  it('should carry over valid main machine type across compute types', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    await pickMainCpu(container, 2);
    await pickMainRam(container, 7.5);

    await pickComputeType(container, ComputeType.Dataproc);

    // n1-standard-2 is legal for Dataproc, so it should remain.
    expect(getMainCpu(container)).toBe('2');
    expect(getMainRam(container)).toBe('7.5');
  });

  it('should warn user about re-creation if there are updates that require one - increase disk size', async () => {
    mockUseCustomRuntime();
    component();

    const detachableDiskElement = spinDiskElement('detachable-disk');
    const detachableDisk = detachableDiskElement.getAttribute('value');
    await pickDetachableDiskSize(parseInt(detachableDisk) + 10);
    await clickExpectedButton('Next');
    // After https://precisionmedicineinitiative.atlassian.net/browse/RW-9167 Re-attachable persistent disk is default
    // Increase disk size for RPD does not show any error message
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /any in\-memory state and local file modifications will be erased\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about re-boot if there are updates that require one - increase disk size', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    component();
    const masterDiskSize = screen
      .getByRole('spinbutton', {
        name: /standard\-disk/i,
      })
      .getAttribute('value');

    await pickStandardDiskSize(parseInt(masterDiskSize) + 10);
    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require a reboot of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /any in\-memory state will be erased, but local file modifications will be preserved\. /i
      )
    ).toBeInTheDocument();
  });

  it('should not warn user for updates where not needed - number of workers', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    component();
    const numWorkers = screen
      .getByRole('spinbutton', {
        name: /num\-workers/i,
      })
      .getAttribute('value');
    await pickNumWorkers(parseInt(numWorkers) + 2);
    await clickExpectedButton('Next');
    expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
    expect(screen.queryByText('These changes')).not.toBeInTheDocument();
  });

  it('should not warn user for updates where not needed - number of preemptibles', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    component();
    const numWorkers = screen
      .getByRole('spinbutton', {
        name: /num\-workers/i,
      })
      .getAttribute('value');

    await pickNumWorkers(parseInt(numWorkers) + 2);
    await clickExpectedButton('Next');
    expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
    expect(screen.queryByText('These changes')).not.toBeInTheDocument();
  });

  it('should warn user about reboot if there are updates that require one - CPU', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    const { container } = component();

    const mainCpuSize = parseInt(getMainCpu(container)) + 4;
    await pickMainCpu(container, mainCpuSize);
    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require a reboot of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about reboot if there are updates that require one - Memory', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    const { container } = component();

    expect(getMainRam(container)).toEqual('15');
    // 15 GB -> 26 GB
    await pickMainRam(container, 26);
    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require a reboot of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about re-creation if there are updates that require one - CPU', async () => {
    mockUseCustomRuntime();
    const { container } = component();
    await pickMainCpu(container, parseInt(getMainCpu(container)) + 4);
    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about re-creation if there are updates that require one - Memory', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    expect(getMainRam(container)).toEqual('15');
    // 15 GB -> 26 GB
    await pickMainRam(container, 26);
    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async () => {
    // The diffs that generate the error message that we are looking for does not seem to come from the runtime(s) in here. There is some sort of function that generates it.
    mockUseCustomRuntime();
    const { container } = component();

    await pickComputeType(container, ComputeType.Dataproc);
    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    mockUseCustomRuntime();
    component();
    const diskValueAsInt = parseInt(getDetachableDiskValue().replace(/,/g, ''));
    const newDiskValue = diskValueAsInt - 10;
    await pickDetachableDiskSize(newDiskValue);
    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your persistent disk and cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    const { container } = component();

    expect(getWorkerCpu(container)).toEqual('4');
    // 4 -> 8
    await pickWorkerCpu(container, 8);
    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn the user about deletion if there are updates that require one - Worker RAM', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    const { container } = component();

    expect(getWorkerRam(container)).toEqual('15');
    // 15 -> 26
    await pickWorkerRam(container, 26);
    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn the user about deletion if there are updates that require one - Worker Disk', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    component();

    await pickWorkerDiskSize(parseInt(getWorkerDiskValue()) + 10);
    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should retain original inputs when hitting cancel from the Confirm panel', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });
    mockUseCustomRuntime();
    const { container } = component();

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB + 10);
    await pickMainCpu(container, 8);
    await pickMainRam(container, 30);
    await pickWorkerCpu(container, 16);
    await pickWorkerRam(container, 60);
    await pickNumPreemptibleWorkers(3);
    await pickNumWorkers(5);
    await pickWorkerDiskSize(DATAPROC_MIN_DISK_SIZE_GB);

    await clickExpectedButton('Next');
    await clickExpectedButton('Cancel');

    expect(getMasterDiskValue()).toBe(
      (DATAPROC_MIN_DISK_SIZE_GB + 10).toString()
    );
    expect(getMainCpu(container)).toBe('8');
    expect(getMainRam(container)).toBe('30');
    expect(getWorkerCpu(container)).toBe('16');
    expect(getWorkerRam(container)).toBe('60');
    expect(getNumOfPreemptibleWorkersValue()).toBe('3');
    expect(getNumOfWorkersValue()).toBe('5');
    expect(getWorkerDiskValue()).toBe(DATAPROC_MIN_DISK_SIZE_GB.toString());
  });

  it('should disable Next button if Runtime is in Creating State', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      status: RuntimeStatus.CREATING,
    });
    mockUseCustomRuntime();
    component();

    const nextButton = screen.getByRole('button', { name: 'Next' });
    expectButtonElementDisabled(nextButton);
  });

  it('should add additional options when the compute type changes', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    await pickComputeType(container, ComputeType.Dataproc);

    expect(getNumOfWorkersValue()).toBeTruthy();
    expect(getNumOfPreemptibleWorkersValue()).toBeTruthy();
    expect(getWorkerCpu(container)).toBeTruthy();
    expect(getWorkerRam(container)).toBeTruthy();
    expect(getWorkerDiskValue()).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    expect(screen.getByText('Cost when running')).toBeInTheDocument();
    expect(screen.getByText('Cost when paused')).toBeInTheDocument();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    expect(getRunningCost()).toEqual('$0.20 per hour');
    expect(getPausedCost()).toEqual('< $0.01 per hour');

    // Change the machine to n1-standard-8 and bump the storage to 300GB.
    await pickMainCpu(container, 8);
    await pickMainRam(container, 30);
    await pickDetachableDiskSize(300);
    expect(getRunningCost()).toEqual('$0.40 per hour');
    expect(getPausedCost()).toEqual('$0.02 per hour');

    await pickPresets(container, runtimePresets().generalAnalysis.displayName);
    expect(getRunningCost()).toEqual('$0.20 per hour');
    expect(getPausedCost()).toEqual('< $0.01 per hour');

    await pickComputeType(container, ComputeType.Dataproc);
    expect(getRunningCost()).toEqual('$0.73 per hour');
    expect(getPausedCost()).toEqual('$0.02 per hour');

    // Bump up all the worker values to increase the price on everything.
    await pickNumWorkers(4);
    await pickNumPreemptibleWorkers(4);
    await pickWorkerCpu(container, 8);
    await pickWorkerRam(container, 30);
    await pickWorkerDiskSize(300);
    expect(getRunningCost()).toEqual('$2.88 per hour');
    expect(getPausedCost()).toEqual('$0.14 per hour');
  });

  it('should update the cost estimator when master machine changes', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 1000,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
      },
    });
    mockUseCustomRuntime();
    const { container } = component();

    // with Master disk size: 1000
    expect(screen.getByText('Cost when running')).toBeInTheDocument();
    expect(screen.getByText('Cost when paused')).toBeInTheDocument();

    expect(getRunningCost()).toEqual('$0.77 per hour');
    expect(getPausedCost()).toEqual('$0.07 per hour');

    // Change the Master disk size or master size to 150
    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);

    expect(screen.getByText('Cost when running')).toBeInTheDocument();
    expect(screen.getByText('Cost when paused')).toBeInTheDocument();

    expect(getRunningCost()).toEqual('$0.73 per hour');
    expect(getPausedCost()).toEqual('$0.02 per hour');
    // Switch to n1-highmem-4, double disk size.
    await pickMainRam(container, 26);
    await pickStandardDiskSize(2000);
    expect(getRunningCost()).toEqual('$0.87 per hour');
    expect(getPausedCost()).toEqual('$0.13 per hour');
  });

  it('should prevent runtime update when disk size is invalid', async () => {
    mockUseCustomRuntime();
    const { container } = component();

    const getNextButton = () => screen.getByRole('button', { name: 'Next' });

    await pickComputeType(container, ComputeType.Dataproc);
    await pickWorkerDiskSize(49);
    expectButtonElementDisabled(getNextButton());

    await pickWorkerDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    expectButtonElementEnabled(getNextButton());
  });

  it('should prevent runtime update when disk size of Standard disk type is invalid', async () => {
    mockUseCustomRuntime();
    component();
    const getNextButton = () => screen.getByRole('button', { name: 'Next' });

    await pickDetachableDiskSize(49);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
  });

  it('should prevent runtime update when PD disk size is invalid', async () => {
    mockUseCustomRuntime();
    const { container } = component();
    const getNextButton = () => screen.getByRole('button', { name: 'Next' });

    await pickDetachableType(container, DiskType.SSD);
    await pickDetachableDiskSize(49);

    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableType(container, DiskType.STANDARD);
    expectButtonElementDisabled(getNextButton());
  });

  it('should prevent runtime creation when disk size is invalid', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();

    const getCreateButton = () =>
      screen.getByRole('button', { name: 'Create' });

    const { container } = component();
    await clickExpectedButton('Customize');

    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(49);
    expectButtonElementDisabled(getCreateButton());

    await pickStandardDiskSize(4900);
    expectButtonElementDisabled(getCreateButton());

    await pickStandardDiskSize(MIN_DISK_SIZE_GB);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickWorkerDiskSize(49);
    expectButtonElementDisabled(getCreateButton());

    await pickWorkerDiskSize(4900);
    expectButtonElementDisabled(getCreateButton());

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    expectButtonElementEnabled(getCreateButton());
  });

  it('should allow worker configuration for stopped GCE runtime', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.STOPPED,
      gceConfig: defaultGceConfig(),
      dataprocConfig: null,
    });
    mockUseCustomRuntime();
    const { container } = component();
    await pickComputeType(container, ComputeType.Dataproc);

    const workerCountInput = spinDiskElement('num-workers');
    expect(workerCountInput.attributes.getNamedItem('disabled')).toBeNull();
    const preemptibleCountInput = spinDiskElement('num-preemptible');
    expect(
      preemptibleCountInput.attributes.getNamedItem('disabled')
    ).toBeNull();
  });

  it('should allow creating gce without GPU', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');
    await pickComputeType(container, ComputeType.Standard);
    await pickMainCpu(container, 8);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });

    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceWithPdConfig.gpuConfig
    ).toEqual(null);
  });

  it('should allow creating gcePD with GPU', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');
    await pickComputeType(container, ComputeType.Standard);

    const enableGpu: HTMLInputElement = screen.getByRole('checkbox', {
      name: /enable gpus/i,
    });

    expect(enableGpu.checked).toEqual(false);
    enableGpu.click();
    expect(enableGpu.checked).toEqual(true);
    await pickGpuType(container, 'NVIDIA Tesla T4');
    await pickGpuNum(container, 2);
    await pickMainCpu(container, 8);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);

    await clickExpectedButton('Create');
    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceConfig
    ).toBeUndefined();
    expect(
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter].runtime
        .gceWithPdConfig.gpuConfig.numOfGpus
    ).toEqual(2);
  });

  it('should allow disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    setCurrentDisk(existingDisk());
    mockUseCustomRuntime();
    const { container } = component();
    await pickComputeType(container, ComputeType.Dataproc);

    await clickExpectedButton('Next');
    expect(
      screen.getByText(
        /your environment currently has a reattachable disk, which will be unused after you apply this update\. /i
      )
    ).toBeInTheDocument();

    await togglePDRadioButton();

    await clickExpectedButton('Next');
    await clickExpectedButton('Update');

    await waitFor(async () => {
      expect(mockSetRuntimeRequest).toHaveBeenCalledTimes(1);
    });
    const pdConfig =
      mockSetRuntimeRequest.mock.calls[firstCall][firstParameter]
        .gceWithPdConfig;
    expect(pdConfig).toBeUndefined();
  });

  it('should allow skipping disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    const disk = existingDisk();
    setCurrentDisk(disk);
    mockUseCustomRuntime();

    const { container } = component();
    await pickComputeType(container, ComputeType.Dataproc);

    await clickExpectedButton('Next');

    expect(
      screen.getByText(
        /your environment currently has a reattachable disk, which will be unused after you apply this update\./i
      )
    ).toBeInTheDocument();
  });

  it('should prevent runtime creation when running cost is too high for initial credits', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');
    const createButton = screen.getByRole('button', { name: 'Create' });

    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(150);
    // This should make the cost about $50 per hour.
    await pickNumWorkers(200);
    expectButtonElementDisabled(createButton);

    await pickNumWorkers(2);
    expectButtonElementEnabled(createButton);
  });

  it('should prevent runtime creation when worker count is invalid', async () => {
    setCurrentRuntime(null);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');

    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    const createButton = screen.getByRole('button', { name: 'Create' });
    await pickNumWorkers(0);
    expectButtonElementDisabled(createButton);

    await pickNumWorkers(1);
    expectButtonElementDisabled(createButton);

    await pickNumWorkers(2);
    expectButtonElementEnabled(createButton);
  });

  it('should prevent runtime creation when running cost is too high for paid tier', async () => {
    setCurrentRuntime(null);
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    mockUseCustomRuntime();
    const { container } = component();

    await clickExpectedButton('Customize');

    await pickComputeType(container, ComputeType.Dataproc);

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    // This should make the cost about $140 per hour.
    await pickNumWorkers(600);
    let createButton = screen.getByRole('button', { name: 'Create' });
    expectButtonElementEnabled(createButton);

    // This should make the cost around $160 per hour.
    await pickNumWorkers(700);
    createButton = screen.getByRole('button', { name: 'Create' });
    // We don't want to disable for user provided billing. Just put a warning.
    expectButtonElementEnabled(createButton);
    expect(
      screen.getByText(
        'Your runtime is expensive. Are you sure you wish to proceed?'
      )
    ).toBeInTheDocument();

    // Picking the number of workers within range should remove the banner
    await pickNumWorkers(2);
    createButton = screen.getByRole('button', { name: 'Create' });
    expect(
      screen.queryByText(
        'Your runtime is expensive. Are you sure you wish to proceed?'
      )
    ).toBeNull();
    expectButtonElementEnabled(createButton);
  });

  it('should disable worker count updates for stopped dataproc cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.STOPPED,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    mockUseCustomRuntime();
    component();

    const workerCountInput: HTMLInputElement = spinDiskElement('num-workers');
    expect(workerCountInput.disabled).toBeTruthy();

    const preemptibleCountInput: HTMLInputElement =
      spinDiskElement('num-preemptible');
    expect(preemptibleCountInput.disabled).toBeTruthy();
  });

  it('should require disk deletion when attempting to create a runtime with a different disk type', async () => {
    const deleteDiskSpy = jest.spyOn(disksApiStub, 'deleteDisk');
    setCurrentRuntime(null);
    const disk = existingDisk();
    setCurrentDisk(disk);
    mockUseCustomRuntime();
    const { container } = component();
    await clickExpectedButton('Customize');
    await pickDetachableType(container, DiskType.SSD);
    await clickExpectedButton('Next');
    screen.getByRole('heading', {
      name: /environment creation requires deleting your unattached disk/i,
    });
    await togglePDRadioButton();
    await clickExpectedButton('Delete');

    await waitFor(() => {
      expect(deleteDiskSpy).toHaveBeenCalled();
    });

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should not delete disk when attempting to create a runtime with a larger disk', async () => {
    const deleteDiskSpy = jest.spyOn(disksApiStub, 'deleteDisk');
    setCurrentRuntime(null);
    const disk = existingDisk();
    setCurrentDisk(disk);
    mockUseCustomRuntime();
    component();
    await clickExpectedButton('Customize');
    await pickDetachableDiskSize(disk.size + 10);
    await clickExpectedButton('Create');

    expect(deleteDiskSpy).not.toHaveBeenCalled();

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should require disk deletion when attempting to create a runtime with a smaller disk', async () => {
    const deleteDiskSpy = jest.spyOn(disksApiStub, 'deleteDisk');
    setCurrentRuntime(null);
    const disk = existingDisk();
    setCurrentDisk(disk);
    mockUseCustomRuntime();
    component();
    await clickExpectedButton('Customize');
    await pickDetachableDiskSize(disk.size - 10);
    await clickExpectedButton('Next');

    screen.getByRole('heading', {
      name: /environment creation requires deleting your unattached disk/i,
    });
    await togglePDRadioButton();
    await clickExpectedButton('Delete');

    await waitFor(() => {
      expect(deleteDiskSpy).toHaveBeenCalled();
    });

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
