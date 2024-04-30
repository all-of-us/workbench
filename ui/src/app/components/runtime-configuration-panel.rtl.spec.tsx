import '@testing-library/jest-dom';

import { act } from 'react-dom/test-utils';
import { MemoryRouter } from 'react-router';
import { parseInt } from 'lodash';

import {
  Disk,
  DisksApi,
  DiskType,
  GpuConfig,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
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
import { runtimePresets } from 'app/utils/runtime-presets';
import {
  cdrVersionStore,
  clearCompoundRuntimeOperations,
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
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
  deriveCurrentRuntime,
  getErrorsAndWarnings,
  RuntimeConfigurationPanel,
  RuntimeConfigurationPanelProps,
} from './runtime-configuration-panel';
import { PanelContent } from './runtime-configuration-panel/utils';

describe(deriveCurrentRuntime.name, () => {
  it('returns an undefined runtime if the inputs are undefined', () => {
    const expected = undefined;

    expect(
      deriveCurrentRuntime({
        crFromCustomRuntimeHook: undefined,
        gcePersistentDisk: undefined,
      })
    ).toEqual(expected);
  });

  describe.each([
    ['GCE', defaultGceRuntime()],
    ['GCE with PD', defaultGceRuntimeWithPd()],
    ['DataProc', defaultDataProcRuntime()],
  ])('%s', (desc, runtime) => {
    it(`returns a ${desc} runtime from the hook if it is not DELETED`, () => {
      // sanity check
      expect(runtime.status).not.toEqual(RuntimeStatus.DELETED);
      expect(
        deriveCurrentRuntime({
          crFromCustomRuntimeHook: runtime,
          gcePersistentDisk: undefined,
        })
      ).toEqual(runtime);
    });

    it(`returns a ${desc} runtime from the hook if it is DELETED and config type USER_OVERRIDE`, () => {
      const testRuntime = {
        ...runtime,
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.USER_OVERRIDE,
      };

      expect(
        deriveCurrentRuntime({
          crFromCustomRuntimeHook: testRuntime,
          gcePersistentDisk: undefined,
        })
      ).toEqual(testRuntime);
    });
  });

  it(
    'converts a GCE runtime from the hook to the GCE-with-PD preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is no PD',
    () => {
      const runtime = {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const expectedGceWithPdConfig = {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: undefined, // cleared by applyPresetOverride()
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts a GCE runtime from the hook to the GCE-with-PD preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is a PD',
    () => {
      const runtime = {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const disk = { ...stubDisk(), name: 'whatever' };

      const expectedGceWithPdConfig = {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: disk.name,
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: disk,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts the GCE-with-PD runtime from the hook to the preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is a PD',
    () => {
      const runtimeDiskName = 'something';
      const pdName = 'something else';

      const runtime = {
        ...defaultGceRuntimeWithPd(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
        gceWithPdConfig: {
          ...defaultGceRuntimeWithPd().gceWithPdConfig,
          persistentDisk: {
            ...defaultGceRuntimeWithPd().gceWithPdConfig.persistentDisk,
            name: runtimeDiskName,
          },
        },
      };

      const disk = { ...stubDisk(), name: pdName };

      const expectedGceWithPdConfig = {
        ...runtime.gceWithPdConfig,
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: runtimeDiskName, // keeps original disk, does NOT attach a different one
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: disk,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it(
    'converts the GCE-with-PD runtime from the hook to the preset if: ' +
      'it is DELETED and config type GENERAL_ANALYSIS, ' +
      'and there is no PD',
    () => {
      const runtime = {
        ...defaultGceRuntimeWithPd(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      };

      const expectedGceWithPdConfig = {
        ...runtime.gceWithPdConfig,
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: runtime.gceWithPdConfig.persistentDisk.name,
        },
      };

      const currentRuntime = deriveCurrentRuntime({
        crFromCustomRuntimeHook: runtime,
        gcePersistentDisk: undefined,
      });

      expect(currentRuntime.gceConfig).toBeFalsy();
      expect(currentRuntime.gceWithPdConfig).toEqual(expectedGceWithPdConfig);
      expect(currentRuntime.dataprocConfig).toBeFalsy();
    }
  );

  it('converts the Dataproc runtime from the hook to the preset if it is DELETED and config type HAIL_GENOMIC_ANALYSIS', () => {
    const runtime = {
      ...defaultDataProcRuntime(),
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
    };

    const currentRuntime = deriveCurrentRuntime({
      crFromCustomRuntimeHook: runtime,
      gcePersistentDisk: undefined,
    });

    expect(currentRuntime.gceConfig).toBeFalsy();
    expect(currentRuntime.gceWithPdConfig).toBeFalsy();
    expect(currentRuntime.dataprocConfig).toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );
  });
});

describe(createOrCustomize.name, () => {
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
    [
      'DELETED GCE GENERAL_ANALYSIS', // not GCE with PD
      {
        ...defaultGceRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      },
      RuntimeStatus.DELETED, // not used here, but let's be consistent
    ],
    [
      'DELETED HAIL_GENOMIC_ANALYSIS',
      {
        ...defaultDataProcRuntime(),
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      },
      RuntimeStatus.DELETED, // not used here, but let's be consistent
    ],
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
    serverConfigStore.set({ config: defaultServerConfig });
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
    const creatorFreeCreditsRemaining =
      serverConfigStore.get().config.defaultFreeCreditsDollarLimit + 1;

    // want a running cost over $25/hr

    const machine: Machine = findMachineByName('n1-highmem-96'); // most expensive, at about $5.70
    const gpuConfig: GpuConfig = { gpuType: 'nvidia-tesla-v100', numOfGpus: 8 }; // nearly $20 by itself
    const analysisConfig = { ...defaultGceAc, machine, gpuConfig };

    const expectedWarning =
      'Your runtime is expensive. Are you sure you wish to proceed?';

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig, creatorFreeCreditsRemaining }
    );

    expect(errorMessageContent).toEqual([]);
    expect(warningMessageContent).toHaveLength(1);
    expect(warningMessageContent[0].props.children).toEqual(expectedWarning);
  });

  it('should show a cost error when the user does not have enough remaining initial credits', () => {
    const usingInitialCredits = true;
    const creatorFreeCreditsRemaining =
      serverConfigStore.get().config.defaultFreeCreditsDollarLimit - 1;

    // want a running cost over $25/hr

    const machine: Machine = findMachineByName('n1-highmem-96'); // most expensive, at about $5.70
    const gpuConfig: GpuConfig = { gpuType: 'nvidia-tesla-v100', numOfGpus: 8 }; // nearly $20 by itself
    const analysisConfig = { ...defaultGceAc, machine, gpuConfig };

    const expectedError =
      'Your runtime is too expensive. To proceed using free credits, reduce your running costs below'; // $cost

    const { errorMessageContent, warningMessageContent } = getErrorsAndWarnings(
      { usingInitialCredits, analysisConfig, creatorFreeCreditsRemaining }
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

describe(RuntimeConfigurationPanel.name, () => {
  let runtimeApiStub: RuntimeApiStub;
  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;

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

  const existingDisk = (): Disk => {
    return {
      size: 1000,
      diskType: DiskType.STANDARD,
      name: 'my-existing-disk',
      blockSize: 1,
      gceRuntime: true,
    };
  };

  const clickExpectedButton = (name: string) => {
    const button = screen.getByRole('button', { name });
    expect(button).toBeInTheDocument();
    expectButtonElementEnabled(button);
    button.click();
  };

  const pickDropdownOptionAndClick = async (
    container: HTMLElement,
    user: UserEvent,
    dropDownId: string,
    optionText: string
  ): Promise<void> => {
    const option = getDropdownOption(container, dropDownId, optionText);
    await user.click(option);
  };

  const pickSpinButtonSize = async (
    user: UserEvent,
    name: string,
    value: number
  ): Promise<void> => {
    const spinButton = screen.getByRole('spinbutton', { name });
    expect(spinButton).toBeInTheDocument();
    await user.clear(spinButton);
    await user.type(spinButton, value.toString());
  };

  const pickMainCpu = (
    container: HTMLElement,
    user: UserEvent,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'runtime-cpu',
      option.toString()
    );

  const getMainCpu = (container: HTMLElement): string =>
    getDropdownSelection(container, 'runtime-cpu');

  const getMainRam = (container: HTMLElement): string =>
    getDropdownSelection(container, 'runtime-ram');

  const pickComputeType = (
    container: HTMLElement,
    user: UserEvent,
    computeType: ComputeType
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'runtime-compute',
      computeType.toString()
    );

  const pickMainRam = (
    container: HTMLElement,
    user: UserEvent,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'runtime-ram',
      option.toString()
    );

  const pickWorkerCpu = (
    container: HTMLElement,
    user: UserEvent,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'worker-cpu',
      option.toString()
    );

  const pickWorkerRam = (
    container: HTMLElement,
    user: UserEvent,
    option: number
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'worker-ram',
      option.toString()
    );

  const pickDetachableDiskSize = async (
    user: UserEvent,
    size: number
  ): Promise<void> => pickSpinButtonSize(user, 'detachable-disk', size);

  const pickStandardDiskSize = async (
    user: UserEvent,
    size: number
  ): Promise<void> => pickSpinButtonSize(user, 'standard-disk', size);

  const pickWorkerDiskSize = async (
    user: UserEvent,
    size: number
  ): Promise<void> => pickSpinButtonSize(user, 'worker-disk', size);

  const pickNumWorkers = async (user: UserEvent, size: number): Promise<void> =>
    pickSpinButtonSize(user, 'num-workers', size);

  const pickNumPreemptibleWorkers = async (
    user: UserEvent,
    size: number
  ): Promise<void> => pickSpinButtonSize(user, 'num-preemptible', size);

  const pickPresets = (
    container: HTMLElement,
    user: UserEvent,
    option: string
  ): Promise<void> =>
    pickDropdownOptionAndClick(
      container,
      user,
      'runtime-presets-menu',
      option.toString()
    );

  const spinDiskElement = (diskName: string): HTMLElement =>
    screen.getByRole('spinbutton', {
      name: diskName,
    });

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

  beforeEach(async () => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName:
        'billingAccounts/' + defaultServerConfig.freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      googleProject: runtimeApiStub.runtime.googleProject,
    });

    runtimeStore.set({
      runtime: runtimeApiStub.runtime,
      workspaceNamespace: workspaceStubs[0].namespace,
      runtimeLoaded: true,
    });

    runtimeDiskStore.set({
      workspaceNamespace: workspaceStubs[0].namespace,
      gcePersistentDisk: null,
    });
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
  });

  it('should show loading spinner while loading', async () => {
    // simulate not done loading
    runtimeStore.set({ ...runtimeStore.get(), runtimeLoaded: false });

    const { container } = component();
    expect(container).toBeInTheDocument();

    await waitFor(() =>
      // spinner label
      expect(screen.queryByLabelText('Please Wait')).toBeInTheDocument()
    );

    runtimeStore.set({ ...runtimeStore.get(), runtimeLoaded: true });

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

    component();

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceWithPdConfig.machineType).toEqual(
        'n1-standard-4'
      );
      expect(runtimeApiStub.runtime.gceConfig).toBeUndefined();
      expect(runtimeApiStub.runtime.dataprocConfig).toBeUndefined();
    });
  });

  it('should show customize after create', async () => {
    setCurrentRuntime(null);

    component();

    clickExpectedButton('Create');

    // creation closes the panel. re-render with the new runtime state
    await waitFor(() => {
      component();

      // now in Customize mode

      const button = screen.getByRole('button', { name: 'Customize' });
      expect(button).toBeInTheDocument();
      expectButtonElementEnabled(button);
    });
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is GeneralAnalysis', async () => {
    // In the case where the user's latest runtime is a preset (GeneralAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: 1000,
      },
    });

    component();

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceConfig).toBeUndefined();
      expect(runtimeApiStub.runtime.gceWithPdConfig.machineType).toBe(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .machineType
      );
      expect(runtimeApiStub.runtime.gceWithPdConfig.persistentDisk.size).toBe(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .persistentDisk.size
      );
    });
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is HailGenomicsAnalysis', async () => {
    // In the case where the user's latest runtime is a preset (HailGenomicsAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime

    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: {
        ...defaultDataprocConfig(),
        masterMachineType: 'n1-standard-16',
        masterDiskSize: 999,
        workerDiskSize: 444,
        numberOfWorkers: 5,
      },
    });

    component();

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      const {
        masterMachineType,
        masterDiskSize,
        workerDiskSize,
        numberOfWorkers,
      } = runtimeApiStub.runtime.dataprocConfig;

      expect(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
      ).toMatchObject({
        masterMachineType,
        masterDiskSize,
        workerDiskSize,
        numberOfWorkers,
      });
    });
  });

  it('should allow creation when runtime has error status', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.ERROR,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });

    component();

    clickExpectedButton('Try Again');
    await waitFor(() =>
      // Kicks off a deletion to first clear the error status runtime.
      expect(runtimeApiStub.runtime.status).toEqual('Deleting')
    );
  });

  it('should allow creation from error with an update', async () => {
    const user = userEvent.setup();

    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.ERROR,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });

    const { container } = component();

    await pickMainCpu(container, user, 8);
    clickExpectedButton('Try Again');

    await waitFor(() =>
      // Kicks off a deletion to first clear the error status runtime.
      expect(runtimeApiStub.runtime.status).toEqual('Deleting')
    );
  });

  it('should allow creation with GCE with PD config', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(null);

    const { container } = component();
    clickExpectedButton('Customize');

    await pickMainCpu(container, user, 8);
    await pickMainRam(container, user, 52);
    await pickDetachableDiskSize(user, MIN_DISK_SIZE_GB + 10);

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.USER_OVERRIDE
      );
      expect(runtimeApiStub.runtime.gceWithPdConfig).toEqual({
        machineType: 'n1-highmem-8',
        gpuConfig: null,
        persistentDisk: {
          diskType: 'pd-standard',
          labels: {},
          name: 'stub-disk',
          size: MIN_DISK_SIZE_GB + 10,
        },
      });
      expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
    });
  });

  it('should allow creation with Dataproc config', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(null);

    const { container } = component();
    clickExpectedButton('Customize');

    // master settings
    await pickMainCpu(container, user, 2);
    await pickMainRam(container, user, 7.5);
    await pickComputeType(container, user, ComputeType.Dataproc);
    await pickStandardDiskSize(user, DATAPROC_MIN_DISK_SIZE_GB + 10);

    // worker settings
    await pickWorkerCpu(container, user, 8);
    await pickWorkerRam(container, user, 30);
    await pickWorkerDiskSize(user, 300);
    await pickNumWorkers(user, 10);
    await pickNumPreemptibleWorkers(user, 20);

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.USER_OVERRIDE
      );
      expect(runtimeApiStub.runtime.dataprocConfig).toEqual({
        masterMachineType: 'n1-standard-2',
        masterDiskSize: DATAPROC_MIN_DISK_SIZE_GB + 10,
        workerMachineType: 'n1-standard-8',
        workerDiskSize: 300,
        numberOfWorkers: 10,
        numberOfPreemptibleWorkers: 20,
      });
      expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
    });
  });

  it('should disable the Next button if there are no changes and runtime is running', async () => {
    setCurrentRuntime(defaultGceRuntimeWithPd());
    component();
    const button = screen.getByRole('button', { name: 'Next' });
    expect(button).toBeInTheDocument();
    expectButtonElementDisabled(button);
  });

  it('should show create button if runtime is deleted', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.DELETED,
    });
    component();
    const button = screen.getByRole('button', { name: 'Create' });
    expect(button).toBeInTheDocument();
    expectButtonElementEnabled(button);
  });

  it('should allow runtime deletion', async () => {
    component({});
    clickExpectedButton('Delete Environment');

    // confirm that the correct panel is visible
    await waitFor(() => expectConfirmDeletePanel());

    clickExpectedButton('Delete');

    await waitFor(() => {
      // Runtime should be deleting, and panel should have closed.
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.DELETING);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('should allow cancelling runtime deletion', async () => {
    component({});
    clickExpectedButton('Delete Environment');

    // confirm that the correct panel is visible
    await waitFor(() => expectConfirmDeletePanel());

    clickExpectedButton('Cancel');

    await waitFor(() => {
      // Runtime should still be active, and confirm page should no longer be visible.
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.RUNNING);
      expect(onClose).not.toHaveBeenCalled();
      expect(screen.queryByText(confirmDeleteText)).toBeNull();
    });
  });

  it('should require PD (prevent standard disk) for GCE', async () => {
    setCurrentRuntime(defaultGceRuntimeWithPd());

    component();

    expect(
      screen.queryByText('Reattachable persistent disk')
    ).toBeInTheDocument();
    expect(screen.queryByText('Standard disk')).not.toBeInTheDocument();
  });

  it('should require standard disk / prevent detachable PD use for Dataproc', async () => {
    setCurrentRuntime(defaultDataProcRuntime());

    component();

    expect(screen.queryByText('Standard disk')).toBeInTheDocument();
    expect(
      screen.queryByText('Reattachable persistent disk')
    ).not.toBeInTheDocument();
  });

  it('should allow Dataproc -> PD transition', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(defaultDataProcRuntime());

    const { container } = component();

    // confirm Dataproc by observing that Standard disk is required
    expect(screen.queryByText('Standard disk')).toBeInTheDocument();

    await pickComputeType(container, user, ComputeType.Standard);

    await waitFor(() => {
      // confirm GCE by observing that PD is required
      expect(
        screen.queryByText('Reattachable persistent disk')
      ).toBeInTheDocument();
    });

    clickExpectedButton('Next');
    clickExpectedButton('Update');

    // after deletion happens, confirm the new runtime state
    runtimeApiStub.runtime.status = RuntimeStatus.DELETED;
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
      expect(disksApiStub.disk).toBeTruthy();
    });
  });

  it('should render Spark console links for a running cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
      gceWithPdConfig: null,
    });

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
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
      gceWithPdConfig: null,
    });

    component();
    const manageButton = screen.getByRole('button', {
      name: 'Manage and monitor Spark console',
    });
    expect(manageButton).toBeInTheDocument();
    expectButtonElementDisabled(manageButton);
    manageButton.click();
  });

  it('Should disable standard storage option for existing GCE runtime and have reattachable selected', async () => {
    // set GCE Runtime without PD as current runtime
    setCurrentRuntime(defaultGceRuntime());

    component();
    expect(
      screen.queryByText('Reattachable persistent disk')
    ).toBeInTheDocument();
    expect(screen.queryByText('Standard disk')).not.toBeInTheDocument();
  });

  it('should allow configuration via GCE preset', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(null);

    const { container } = component();

    const CustomizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await CustomizeButton.click();

    // Ensure set the form to something non-standard to start
    await pickMainCpu(container, user, 8);
    await pickComputeType(container, user, ComputeType.Dataproc);
    await pickStandardDiskSize(user, MIN_DISK_SIZE_GB + 10);

    // GPU
    await pickPresets(
      container,
      user,
      runtimePresets.generalAnalysis.displayName
    );

    clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.GENERAL_ANALYSIS
      );
      expect(runtimeApiStub.runtime.gceWithPdConfig.persistentDisk).toEqual({
        diskType: 'pd-standard',
        labels: {},
        name: 'stub-disk',
        size: MIN_DISK_SIZE_GB,
      });
      expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
    });
  });

  it('should allow configuration via dataproc preset', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(null);

    const { container } = component();

    const CustomizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await CustomizeButton.click();

    await pickPresets(container, user, runtimePresets.hailAnalysis.displayName);

    clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS
      );
      expect(runtimeApiStub.runtime.dataprocConfig).toEqual(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
      );
      expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
    });
  });

  it(
    'should set runtime preset values in customize panel instead of getRuntime values ' +
      'if configurationType is GeneralAnalysis',
    async () => {
      const customMachineType = 'n1-standard-16';
      const customDiskSize = 1000;
      setCurrentRuntime({
        ...runtimeApiStub.runtime,
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
        gceConfig: {
          ...defaultGceConfig(),
          machineType: customMachineType,
          diskSize: customDiskSize,
        },
        dataprocConfig: null,
      });

      // show that the preset values do not match the existing runtime

      const { machineType, persistentDisk } =
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig;

      expect(customMachineType).not.toEqual(machineType);
      expect(customDiskSize).not.toEqual(persistentDisk.size);
      const { container } = component();
      // Try :Check there is no spinner
      expect(getMainCpu(container)).toEqual(
        findMachineByName(machineType).cpu.toString()
      );
      expect(getMainRam(container)).toEqual(
        findMachineByName(machineType).memory.toString()
      );
      const detachableDiskElement = spinDiskElement(/detachable\-disk/i);
      await waitFor(() => {
        expect(detachableDiskElement).toHaveValue('120');
      });
    }
  );

  it(
    'should set runtime preset values in customize panel instead of getRuntime values ' +
      'if configurationType is HailGenomicsAnalysis',
    async () => {
      const customMasterMachineType = 'n1-standard-16';
      const customMasterDiskSize = 999;
      const customWorkerDiskSize = 444;
      const customNumberOfWorkers = 5;
      setCurrentRuntime({
        ...runtimeApiStub.runtime,
        status: RuntimeStatus.DELETED,
        configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
        gceConfig: null,
        gceWithPdConfig: null,
        dataprocConfig: {
          ...defaultDataprocConfig(),
          masterMachineType: customMasterMachineType,
          masterDiskSize: customMasterDiskSize,
          workerDiskSize: customWorkerDiskSize,
          numberOfWorkers: customNumberOfWorkers,
        },
      });

      // show that the preset values do not match the existing runtime

      const {
        masterMachineType,
        masterDiskSize,
        workerDiskSize,
        numberOfWorkers,
      } = runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig;

      expect(customMasterMachineType).not.toEqual(masterMachineType);
      expect(customMasterDiskSize).not.toEqual(masterDiskSize);
      expect(customWorkerDiskSize).not.toEqual(workerDiskSize);
      expect(customNumberOfWorkers).not.toEqual(numberOfWorkers);

      const { container } = component();

      clickExpectedButton('Customize');
      expect(getMainCpu(container)).toEqual(
        findMachineByName(masterMachineType).cpu.toString()
      );
      expect(getMainRam(container)).toEqual(
        findMachineByName(masterMachineType).memory.toString()
      );

      const masterDiskValue =
        spinDiskElement(/standard\-disk/i).getAttribute('value');
      const workerDiskValue =
        spinDiskElement(/worker\-disk/i).getAttribute('value');
      const numOfWorkersValue =
        spinDiskElement(/num\-workers/i).getAttribute('value');
      expect(masterDiskValue).toEqual(masterDiskSize.toString());
      expect(workerDiskValue).toEqual(workerDiskSize.toString());
      expect(numOfWorkersValue).toEqual(numberOfWorkers.toString());
    }
  );

  it('should reattach to an existing disk by default, for deleted VMs', async () => {
    const disk = existingDisk();
    setCurrentDisk(disk);
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.DELETED,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
      },
      dataprocConfig: null,
    });

    await component();
    const detachableDiskElement = spinDiskElement(/detachable\-disk/i);
    await waitFor(() => {
      const numberFormatter = new Intl.NumberFormat('en-US');
      expect(detachableDiskElement).toHaveValue(
        numberFormatter.format(disk.size)
      );
    });
  });
  it('should allow configuration via dataproc preset from modified form', async () => {
    const user = userEvent.setup();
    setCurrentRuntime(null);

    const { container } = await component();

    clickExpectedButton('Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(container, user, 2);
    await pickMainRam(container, user, 7.5);
    await pickDetachableDiskSize(user, DATAPROC_MIN_DISK_SIZE_GB);
    await pickComputeType(container, user, ComputeType.Dataproc);

    await pickWorkerCpu(container, user, 8);
    await pickWorkerRam(container, user, 30);
    await pickWorkerDiskSize(user, 300);
    await pickNumWorkers(user, 10);
    await pickNumPreemptibleWorkers(user, 20);
    await pickPresets(container, user, runtimePresets.hailAnalysis.displayName);

    clickExpectedButton('Create');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS
      );
      expect(runtimeApiStub.runtime.dataprocConfig).toEqual(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
      );
      expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
    });
  });

  it('should tag as user override after preset modification', async () => {
    const user = userEvent.setup();
    setCurrentRuntime(null);

    const { container } = await component();

    clickExpectedButton('Customize');

    // Take the preset but make a solitary modification.
    await pickPresets(container, user, runtimePresets.hailAnalysis.displayName);
    await pickNumPreemptibleWorkers(user, 20);

    clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.USER_OVERRIDE
      );
    });
  });

  it('should tag as preset if configuration matches', async () => {
    const user = userEvent.setup();
    setCurrentRuntime(null);

    const { container } = await component();
    clickExpectedButton('Customize');

    // Take the preset, make a change, then revert.
    await pickPresets(
      container,
      user,
      runtimePresets.generalAnalysis.displayName
    );
    await pickComputeType(container, user, ComputeType.Dataproc);
    await pickWorkerCpu(container, user, 2);
    await pickComputeType(container, user, ComputeType.Standard);
    await pickDetachableDiskSize(user, MIN_DISK_SIZE_GB);
    clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.GENERAL_ANALYSIS
      );
    });
  });
  it('should restrict memory options by cpu', async () => {
    const user = userEvent.setup();
    const { container } = await component();

    await pickMainCpu(container, user, 8);
    const dropdown: HTMLElement = expectDropdown(container, 'runtime-ram');
    dropdown.click();

    const runtimeRamOptions = Array.from(
      container.querySelectorAll(`#${'runtime-ram'} .p-dropdown-item`)
    ).map((option) => option.textContent);

    expect(runtimeRamOptions).toEqual(['7.2', '30', '52']);
  });

  it('should respect divergent sets of valid machine types across compute types', async () => {
    const user = userEvent.setup();
    const { container } = await component();

    await pickMainCpu(container, user, 1);
    await pickMainRam(container, user, 3.75);

    await pickComputeType(container, user, ComputeType.Dataproc);

    // n1-standard-1 is illegal for Dataproc, so it should restore the default.
    expect(getMainCpu(container)).toBe('4');
    expect(getMainRam(container)).toBe('15');
  });

  it('should carry over valid main machine type across compute types', async () => {
    const user = userEvent.setup();
    const { container } = await component();

    await pickMainCpu(container, user, 2);
    await pickMainRam(container, user, 7.5);

    await pickComputeType(container, user, ComputeType.Dataproc);

    // n1-standard-2 is legal for Dataproc, so it should remain.
    expect(getMainCpu(container)).toBe('2');
    expect(getMainRam(container)).toBe('7.5');
  });
  it('should warn user about re-creation if there are updates that require one - increase disk size', async () => {
    const user = userEvent.setup();

    await component();

    const detachableDiskElement = screen.getByRole('spinbutton', {
      name: /detachable\-disk/i,
    });
    const detachableDisk = detachableDiskElement.getAttribute('value');
    await pickDetachableDiskSize(user, parseInt(detachableDisk) + 10);
    clickExpectedButton('Next');
    // After https://precisionmedicineinitiative.atlassian.net/browse/RW-9167 Re-attachable persistent disk is default
    // Increase disk size for RPD does not show any error message
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /any in\-memory state and local file modifications will be erased\. data stored in workspace buckets is never affected by changes to your cloud environment\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about re-boot if there are updates that require one - increase disk size', async () => {
    const user = userEvent.setup();
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    });
    await component();
    const masterDiskSize = screen
      .getByRole('spinbutton', {
        name: /standard\-disk/i,
      })
      .getAttribute('value');

    await pickStandardDiskSize(user, parseInt(masterDiskSize) + 10);
    clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require a reboot of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /any in\-memory state will be erased, but local file modifications will be preserved\. data stored in workspace buckets is never affected by changes to your cloud environment\./i
      )
    ).toBeInTheDocument();
  });

  it('should not warn user for updates where not needed - number of workers', async () => {
    const user = userEvent.setup();
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    });

    await component();
    const numWorkers = screen
      .getByRole('spinbutton', {
        name: /num\-workers/i,
      })
      .getAttribute('value');
    await pickNumWorkers(user, parseInt(numWorkers) + 2);
    clickExpectedButton('Next');
    expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
    expect(screen.queryByText('These changes')).not.toBeInTheDocument();
  });
});
