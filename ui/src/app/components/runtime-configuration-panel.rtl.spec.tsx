import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router';

import {
  DisksApi,
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
  debugAll,
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectDropdown,
  getDropdownOption,
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

import { RadioButton } from './inputs';
import { WarningMessage } from './messages';
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

  const setCurrentRuntime = (runtime: Runtime) => {
    runtimeApiStub.runtime = runtime;
    runtimeStore.set({ ...runtimeStore.get(), runtime });
  };

  // TODO
  // const setCurrentDisk = (disk: Disk) => {
  //   disksApiStub.disk = disk;
  //   runtimeDiskStore.set({
  //     ...runtimeDiskStore.get(),
  //     gcePersistentDisk: disk,
  //   });
  // };

  const clickExpectedButton = (name: string) => {
    const button = screen.getByRole('button', { name });
    expect(button).toBeInTheDocument();
    expectButtonElementEnabled(button);
    button.click();
  };

  const pickAndClick = (
    container: HTMLElement,
    user: UserEvent,
    dropDownId: string,
    optionText: string
  ): void => {
    const option = getDropdownOption(container, dropDownId, optionText);
    user.click(option);
  };

  const pickMainCpu = (
    container: HTMLElement,
    user: UserEvent,
    option: number
  ): void => pickAndClick(container, user, 'runtime-cpu', option.toString());

  const pickComputeType = (
    container: HTMLElement,
    user: UserEvent,
    computeType: ComputeType
  ): void =>
    pickAndClick(container, user, 'runtime-compute', computeType.toString());

  // TODO
  const expectMainCpuOption = (container: HTMLElement) =>
    expectDropdown(container, 'runtime-cpu');
  // const pickMainRam = (
  //   container: HTMLElement,
  //   user: UserEvent,
  //   option: number
  // ): void => pickAndClick(container, user, 'runtime-ram', option.toString());

  const confirmDeleteText =
    'You’re about to delete your cloud analysis environment.';
  const expectConfirmDeletePanel = () =>
    expect(screen.queryByText(confirmDeleteText)).not.toBeNull();

  const getDetachableDiskRadio = () =>
    screen.getByRole('radio', {
      name: 'detachableDisk',
    });

  const getStandardDiskRadio = () =>
    screen.getByRole('radio', {
      name: 'standardDisk',
    });

  const component = (
    propOverrides?: Partial<RuntimeConfigurationPanelProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(
      <MemoryRouter>
        {' '}
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
    serverConfigStore.set({ config: { ...defaultServerConfig } });
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
    clearCompoundRuntimeOperations();
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
      const { container } = component();

      // now in Customize mode

      const button = screen.getByRole('button', { name: 'Customize' });
      expect(button).toBeInTheDocument();
      expectButtonElementEnabled(button);

      expectMainCpuOption(container);
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

    pickMainCpu(container, user, 8);
    clickExpectedButton('Try Again');

    await waitFor(() =>
      // Kicks off a deletion to first clear the error status runtime.
      expect(runtimeApiStub.runtime.status).toEqual('Deleting')
    );
  });

  // TODO
  // it('should allow creation with GCE with PD config', async () => {
  //   const user = userEvent.setup();
  //
  //   setCurrentRuntime(null);
  //
  //   const { container } = component();
  //   clickExpectedButton('Customize');
  //
  //   await waitFor(() => expectMainCpuOption(container));
  //
  //   pickMainCpu(container, user, 8);
  //   pickMainRam(container, user, 52);
  //   pickDetachableDiskSize(container, user, MIN_DISK_SIZE_GB + 10);
  //
  //   clickExpectedButton('Create');
  //
  //   await waitFor(() => {
  //     expect(runtimeApiStub.runtime.status).toEqual('Creating');
  //     expect(runtimeApiStub.runtime.configurationType).toEqual(
  //       RuntimeConfigurationType.USER_OVERRIDE
  //     );
  //     expect(runtimeApiStub.runtime.gceWithPdConfig).toEqual({
  //       machineType: 'n1-highmem-8',
  //       gpuConfig: null,
  //       persistentDisk: {
  //         diskType: 'pd-standard',
  //         labels: {},
  //         name: 'stub-disk',
  //         size: MIN_DISK_SIZE_GB + 10,
  //       },
  //     });
  //     expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  //   });
  // });

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

    const detachablePdButton = getDetachableDiskRadio();
    expect(detachablePdButton).toBeInTheDocument();
    expect(detachablePdButton).not.toBeDisabled();

    const standardDiskButton = getStandardDiskRadio();
    expect(standardDiskButton).toBeInTheDocument();
    expect(standardDiskButton).toBeDisabled();
  });

  it('should require standard disk / prevent detachable PD use for Dataproc', async () => {
    setCurrentRuntime(defaultDataProcRuntime());

    component();

    const detachablePdButton = getDetachableDiskRadio();
    expect(detachablePdButton).toBeInTheDocument();
    expect(detachablePdButton).toBeDisabled();

    const standardDiskButton = getStandardDiskRadio();
    expect(standardDiskButton).toBeInTheDocument();
    expect(standardDiskButton).not.toBeDisabled();
  });

  it('should allow Dataproc -> PD transition', async () => {
    const user = userEvent.setup();

    setCurrentRuntime(defaultDataProcRuntime());

    const { container } = component();

    // confirm Dataproc by observing that PD is disabled
    expect(getDetachableDiskRadio()).toBeDisabled();

    pickComputeType(container, user, ComputeType.Standard);

    await waitFor(() => {
      // confirm GCE by observing that PD is enabled
      expect(getDetachableDiskRadio()).not.toBeDisabled();
    });

    clickExpectedButton('Next');
    clickExpectedButton('Update');

    runtimeApiStub.runtime.status = RuntimeStatus.DELETED;

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
      expect(disksApiStub.disk).toBeTruthy();
    });
  });

  // TODO
  // it('should allow disk deletion when detaching', async () => {
  //   const user = userEvent.setup();
  //
  //   setCurrentRuntime(defaultGceRuntimeWithPd());
  //   setCurrentDisk(stubDisk());
  //
  //   const { container } = component();
  //
  //   pickComputeType(container, user, ComputeType.Dataproc);
  //
  //   await waitFor(() =>
  //     // confirm Dataproc by observing that PD is disabled
  //     expect(getDetachableDiskRadio()).toBeDisabled()
  //   );
  //
  //   clickExpectedButton('Next');
  //
  //   expect(
  //     screen.queryByText(/will be unused after you apply this update/)
  //   ).toBeInTheDocument();
  //
  //   const deleteRadio = screen.getByRole('radio', {
  //     name: 'Delete persistent disk',
  //   });
  //   deleteRadio.click();
  //
  //   clickExpectedButton('Next');
  //   clickExpectedButton('Update');
  //
  //   runtimeApiStub.runtime.status = RuntimeStatus.DELETED;
  //
  //   await waitFor(() => {
  //     expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
  //     expect(disksApiStub.disk).toBeNull();
  //   });
  // });

  //   it('should allow skipping disk deletion when detaching', async () => {
  //     setCurrentRuntime(defaultGceRuntimeWithPd());
  // //    setCurrentDisk(existingDisk());
  //
  //     const wrapper = component();
  //     pickComputeType(wrapper, ComputeType.Dataproc);
  //
  //     await mustClickButton(wrapper, 'Next');
  //
  //     expect(wrapper.text()).toContain(
  //       'will be unused after you apply this update'
  //     );
  //
  //     // Default option should be NOT to delete.
  //     await mustClickButton(wrapper, 'Next');
  //     await mustClickButton(wrapper, 'Update');
  //     runtimeApiStub.runtime.status = RuntimeStatus.DELETED;
  //
  //     await waitForFakeTimersAndUpdate(wrapper, /* maxRetries*/ 10);
  //
  //     expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
  //     expect(disksApiStub.disk?.name).toEqual(disk.name);
  //   });

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
      screen.queryByText(/only support reattachable persistent disks/)
    ).toBeInTheDocument();

    const detachablePdButton = getDetachableDiskRadio();
    expect(detachablePdButton).toBeInTheDocument();
    expect(detachablePdButton).toBeEnabled();
    expect(detachablePdButton).toHaveProperty('checked');

    const standardDiskButton = getStandardDiskRadio();
    expect(standardDiskButton).toBeInTheDocument();
    expect(standardDiskButton).toBeDisabled();
    expect(standardDiskButton).toHaveProperty('disabled');
  });
});
