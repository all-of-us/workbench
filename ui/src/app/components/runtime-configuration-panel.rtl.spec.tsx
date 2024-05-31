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

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import {
  disksApi,
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';
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
import { diskTypeLabels } from 'app/utils/runtime-utils';
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
  let user: UserEvent;

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

  const detachableDiskRuntime = (): Runtime => {
    const { size, diskType, name } = existingDisk();
    return {
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
      gceWithPdConfig: {
        machineType: 'n1-standard-16',
        persistentDisk: {
          size,
          diskType,
          name,
          labels: {},
        },
        gpuConfig: null,
      },
      gceConfig: null,
      dataprocConfig: null,
    };
  };

  type DetachableDiskCase = [
    string,
    ((container: HTMLElement) => Promise<void>)[],
    {
      wantUpdateDisk?: boolean;
      wantDeleteDisk?: boolean;
      wantDeleteRuntime?: boolean;
    }
  ];

  const clickExpectedButton = (name: string) => {
    const button = screen.getByRole('button', { name });
    expect(button).toBeInTheDocument();
    expectButtonElementEnabled(button);
    button.click();
  };

  const pickDropdownOptionAndClick = async (
    container: HTMLElement,
    dropDownId: string,
    optionText: string
  ): Promise<void> => {
    const option = getDropdownOption(container, dropDownId, optionText);
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

  const pickSsdType = (container: HTMLElement): Promise<void> =>
    pickDetachableType(container, DiskType.SSD);

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

  const changeMainCpu_To8 = async (container: HTMLElement) =>
    pickMainCpu(container, 8);

  const clickEnableGpu = async (container) => {
    console.log(container);
    const enableGpu = screen.getByRole('checkbox', {
      name: /enable gpus/i,
    });
    expect(enableGpu).toBeInTheDocument();
    expect(enableGpu).not.toBeChecked();
    fireEvent.click(enableGpu);
    expect(enableGpu).toBeChecked();
  };

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

  const decrementDetachableDiskSize = async (container): Promise<void> => {
    console.log(container);
    const diskValueAsInt =
      parseInt(getDetachableDiskValue().replace(/,/g, '')) - 1;
    await pickDetachableDiskSize(diskValueAsInt);
  };

  const incrementDetachableDiskSize = async (container): Promise<void> => {
    console.log(container);
    const diskValueAsInt =
      parseInt(getDetachableDiskValue().replace(/,/g, '')) + 1;
    await pickDetachableDiskSize(diskValueAsInt);
  };

  const getDeletePDRadio = () =>
    screen.queryByRole('radio', {
      name: 'delete-pd',
    });

  const togglePDRadioButton = () => {
    getDeletePDRadio().click();
  };

  const clickButtonIfVisible = (name) => {
    const button = screen.queryByRole('button', { name });
    if (button) {
      button.click();
    }
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

  const waitForFakeTimersAndUpdate = async (maxRetries = 10) => {
    const createRuntimeSpy = jest.spyOn(runtimeApi(), 'createRuntime');

    for (let i = 0; i < maxRetries; i++) {
      if (runtimeApiStub.runtime.status === RuntimeStatus.DELETED) {
        // Wait for the updates to complete
        await act(async () => {
          await waitFor(
            async () => {
              console.log(runtimeApiStub.runtime.status);
              await new Promise((r) => setTimeout(r, 2000));
              expect(createRuntimeSpy).toHaveBeenCalledTimes(1);
              return;
            },
            { timeout: 10000 }
          );
        });
      }
    }
  };

  async function runDetachableDiskCase(
    container,
    [
      _,
      setters,
      {
        wantUpdateDisk = false,
        wantDeleteDisk = false,
        wantDeleteRuntime = false,
      },
    ]: DetachableDiskCase,
    existingDiskName: string
  ) {
    const deleteDiskSpy = jest.spyOn(disksApi(), 'deleteDisk');
    const updateDiskSpy = jest.spyOn(disksApi(), 'updateDisk');

    for (const f of setters) {
      await f(container);
    }

    clickButtonIfVisible('Next');
    clickButtonIfVisible('Update');
    clickButtonIfVisible('Create');
    const deleteRadio = screen.queryAllByTestId('delete-unattached-pd-radio');
    if (deleteRadio && deleteRadio.length > 0) {
      expect(deleteRadio[0]).not.toBeChecked();
      fireEvent.click(deleteRadio[0]);
      expect(deleteRadio[0]).toBeChecked();
      clickExpectedButton('Delete');
    }

    await act(async () => {
      await waitFor(() => {
        expect(updateDiskSpy).toHaveBeenCalledTimes(wantUpdateDisk ? 1 : 0);
        expect(deleteDiskSpy).toHaveBeenCalledTimes(wantDeleteDisk ? 1 : 0);
      });
    });

    if (wantDeleteRuntime) {
      await act(async () => {
        await waitFor(() => {
          expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.DELETING);
          runtimeApiStub.runtime.status = RuntimeStatus.DELETED;
        });
      });
      expect(runtimeApiStub.runtime.status).toBe(RuntimeStatus.DELETED);
      // Dropdown adds a hacky setTimeout(.., 1), which causes exceptions here, hence the retries.
      await waitForFakeTimersAndUpdate(/* maxRetries*/ 20);
    }

    await waitFor(() => {
      if (wantDeleteDisk) {
        expect(disksApiStub.disk.name).not.toEqual(existingDiskName);
      } else {
        expect(disksApiStub.disk.name).toEqual(existingDiskName);
      }
    });
  }

  beforeEach(async () => {
    user = userEvent.setup();
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
    // Some test runtime pooling were interfering with other tests using fake timers helped stopping that
    jest.clearAllTimers();
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

    await pickMainCpu(container, 8);
    clickExpectedButton('Try Again');

    await waitFor(() =>
      // Kicks off a deletion to first clear the error status runtime.
      expect(runtimeApiStub.runtime.status).toEqual('Deleting')
    );
  });

  it('should allow creation with GCE with PD config', async () => {
    setCurrentRuntime(null);

    const { container } = component();
    clickExpectedButton('Customize');

    await pickMainCpu(container, 8);
    await pickMainRam(container, 52);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB + 10);

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
    setCurrentRuntime(null);

    const { container } = component();
    clickExpectedButton('Customize');

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
    setCurrentRuntime(defaultDataProcRuntime());

    const { container } = component();

    // confirm Dataproc by observing that Standard disk is required
    expect(screen.queryByText('Standard disk')).toBeInTheDocument();

    await pickComputeType(container, ComputeType.Standard);

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
    setCurrentRuntime(null);

    const { container } = component();

    const CustomizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await CustomizeButton.click();

    // Ensure set the form to something non-standard to start
    await pickMainCpu(container, 8);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(MIN_DISK_SIZE_GB + 10);

    // GPU
    await pickPresets(container, runtimePresets.generalAnalysis.displayName);

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
    setCurrentRuntime(null);

    const { container } = component();

    const CustomizeButton = screen.getByRole('button', {
      name: 'Customize',
    });

    await CustomizeButton.click();

    await pickPresets(container, runtimePresets.hailAnalysis.displayName);

    clickExpectedButton('Create');
    await waitFor(
      () => {
        expect(runtimeApiStub.runtime.status).toEqual('Creating');
        expect(runtimeApiStub.runtime.configurationType).toEqual(
          RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS
        );
        expect(runtimeApiStub.runtime.dataprocConfig).toEqual(
          runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
        );
        expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
      },
      { interval: 750 }
    );
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
      expect(getDetachableDiskValue()).toEqual('120');
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

      expect(getMasterDiskValue()).toEqual(masterDiskSize.toString());
      expect(getWorkerDiskValue()).toEqual(workerDiskSize.toString());
      expect(getNumOfWorkersValue()).toEqual(numberOfWorkers.toString());
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
    const numberFormatter = new Intl.NumberFormat('en-US');
    expect(getDetachableDiskValue()).toEqual(numberFormatter.format(disk.size));
  });

  it.skip('should allow configuration via dataproc preset from modified form', async () => {
    setCurrentRuntime(null);

    const { container } = await component();

    clickExpectedButton('Customize');

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
    await pickPresets(container, runtimePresets.hailAnalysis.displayName);

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

  it.skip('should tag as user override after preset modification', async () => {
    setCurrentRuntime(null);

    const { container } = await component();

    clickExpectedButton('Customize');

    // Take the preset but make a solitary modification.
    await pickPresets(container, runtimePresets.hailAnalysis.displayName);
    await pickNumPreemptibleWorkers(20);

    clickExpectedButton('Create');
    await act(async () => {
      await waitFor(
        () => {
          expect(runtimeApiStub.runtime.status).toEqual('Creating');
          expect(runtimeApiStub.runtime.configurationType).toEqual(
            RuntimeConfigurationType.USER_OVERRIDE
          );
        },
        { interval: 750 }
      );
    });
  });

  it('should tag as preset if configuration matches', async () => {
    setCurrentRuntime(null);

    const { container } = await component();
    clickExpectedButton('Customize');

    // Take the preset, make a change, then revert.
    await pickPresets(container, runtimePresets.generalAnalysis.displayName);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickWorkerCpu(container, 2);
    await pickComputeType(container, ComputeType.Standard);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
    clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.configurationType).toEqual(
        RuntimeConfigurationType.GENERAL_ANALYSIS
      );
    });
  });

  it('should restrict memory options by cpu', async () => {
    const { container } = await component();

    await pickMainCpu(container, 8);
    const dropdown: HTMLElement = expectDropdown(container, 'runtime-ram');
    dropdown.click();

    const runtimeRamOptions = Array.from(
      container.querySelectorAll(`#${'runtime-ram'} .p-dropdown-item`)
    ).map((option) => option.textContent);

    expect(runtimeRamOptions).toEqual(['7.2', '30', '52']);
  });

  it('should respect divergent sets of valid machine types across compute types', async () => {
    const { container } = await component();

    await pickMainCpu(container, 1);
    await pickMainRam(container, 3.75);

    await pickComputeType(container, ComputeType.Dataproc);

    // n1-standard-1 is illegal for Dataproc, so it should restore the default.
    expect(getMainCpu(container)).toBe('4');
    expect(getMainRam(container)).toBe('15');
  });

  it('should carry over valid main machine type across compute types', async () => {
    const { container } = await component();

    await pickMainCpu(container, 2);
    await pickMainRam(container, 7.5);

    await pickComputeType(container, ComputeType.Dataproc);

    // n1-standard-2 is legal for Dataproc, so it should remain.
    expect(getMainCpu(container)).toBe('2');
    expect(getMainRam(container)).toBe('7.5');
  });

  it('should warn user about re-creation if there are updates that require one - increase disk size', async () => {
    await component();

    const detachableDiskElement = spinDiskElement('detachable-disk');
    const detachableDisk = detachableDiskElement.getAttribute('value');
    await pickDetachableDiskSize(parseInt(detachableDisk) + 10);
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
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    });
    await component();
    const masterDiskSize = screen
      .getByRole('spinbutton', {
        name: /standard\-disk/i,
      })
      .getAttribute('value');

    await pickStandardDiskSize(parseInt(masterDiskSize) + 10);
    clickExpectedButton('Next');
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
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
    });

    await component();
    const numWorkers = screen
      .getByRole('spinbutton', {
        name: /num\-workers/i,
      })
      .getAttribute('value');
    await pickNumWorkers(parseInt(numWorkers) + 2);
    clickExpectedButton('Next');
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

    await component();
    const numWorkers = screen
      .getByRole('spinbutton', {
        name: /num\-workers/i,
      })
      .getAttribute('value');

    await pickNumWorkers(parseInt(numWorkers) + 2);
    clickExpectedButton('Next');
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

    const { container } = await component();

    const mainCpuSize = parseInt(getMainCpu(container)) + 4;
    await pickMainCpu(container, mainCpuSize);
    clickExpectedButton('Next');
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

    const { container } = await component();

    expect(getMainRam(container)).toEqual('15');
    // 15 GB -> 26 GB
    await pickMainRam(container, 26);
    clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require a reboot of your cloud environment to take effect\./i
      )
    ).toBeInTheDocument();
  });

  it('should warn user about re-creation if there are updates that require one - CPU', async () => {
    const { container } = await component();
    await pickMainCpu(container, parseInt(getMainCpu(container)) + 4);
    clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeTruthy();
  });

  it('should warn user about re-creation if there are updates that require one - Memory', async () => {
    const { container } = await component();

    expect(getMainRam(container)).toEqual('15');
    // 15 GB -> 26 GB
    await pickMainRam(container, 26);
    clickExpectedButton('Next');

    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async () => {
    const { container } = await component();

    await pickComputeType(container, ComputeType.Dataproc);
    clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your cloud environment to take effect\./i
      )
    ).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    await component();
    const diskValueAsInt = parseInt(getDetachableDiskValue().replace(/,/g, ''));
    const newDiskValue = diskValueAsInt - 10;
    await pickDetachableDiskSize(newDiskValue);
    clickExpectedButton('Next');
    expect(
      screen.getByText(
        /these changes require deletion and re\-creation of your persistent disk and cloud environment to take effect\./i
      )
    ).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const { container } = await component();

    expect(getWorkerCpu(container)).toEqual('4');
    // 4 -> 8
    await pickWorkerCpu(container, 8);
    clickExpectedButton('Next');

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

    const { container } = await component();

    expect(getWorkerRam(container)).toEqual('15');
    // 15 -> 26
    await pickWorkerRam(container, 26);
    clickExpectedButton('Next');

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
    await component();

    await pickWorkerDiskSize(parseInt(getWorkerDiskValue()) + 10);
    clickExpectedButton('Next');

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

    const { container } = await component();

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB + 10);
    await pickMainCpu(container, 8);
    await pickMainRam(container, 30);
    await pickWorkerCpu(container, 16);
    await pickWorkerRam(container, 60);
    await pickNumPreemptibleWorkers(3);
    await pickNumWorkers(5);
    await pickWorkerDiskSize(DATAPROC_MIN_DISK_SIZE_GB);

    clickExpectedButton('Next');
    clickExpectedButton('Cancel');

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

  it('should disable Next button if Runtime is in between states', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      status: RuntimeStatus.CREATING,
    });

    await component();

    const nextButton = screen.getByRole('button', { name: 'Next' });
    expectButtonElementDisabled(nextButton);
  });

  it('should send an updateRuntime API call if runtime changes do not require a delete', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.RUNNING,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
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

    await component();
    const updateSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    const deleteSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');

    await pickStandardDiskSize(
      parseInt(getMasterDiskValue().replace(/,/g, '')) + 20
    );

    clickExpectedButton('Next');

    clickExpectedButton('Update');
    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalled();
      expect(deleteSpy).toHaveBeenCalledTimes(0);
    });
  });

  it('should send an updateDisk API call if disk changes do not require a delete', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    setCurrentDisk(existingDisk());
    await component();

    const updateSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    const deleteSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');

    await pickDetachableDiskSize(1010);

    clickExpectedButton('Next');

    clickExpectedButton('Update');
    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalled();
      expect(deleteSpy).toHaveBeenCalledTimes(0);
    });
  });

  it('should send a delete call if an update requires delete', async () => {
    const { container } = await component();

    await pickComputeType(container, ComputeType.Dataproc);

    clickExpectedButton('Next');
    clickExpectedButton('Update');

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Deleting');
    });
  });

  it('should add additional options when the compute type changes', async () => {
    const { container } = await component();

    await pickComputeType(container, ComputeType.Dataproc);

    expect(getNumOfWorkersValue()).toBeTruthy();
    expect(getNumOfPreemptibleWorkersValue()).toBeTruthy();
    expect(getWorkerCpu(container)).toBeTruthy();
    expect(getWorkerRam(container)).toBeTruthy();
    expect(getWorkerDiskValue()).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async () => {
    const { container } = await component();

    expect(screen.getByText('Cost when running')).toBeTruthy();
    expect(screen.getByText('Cost when paused')).toBeTruthy();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    expect(getRunningCost()).toEqual('$0.20 per hour');
    expect(getPausedCost()).toEqual('< $0.01 per hour');

    // Change the machine to n1-standard-8 and bump the storage to 300GB.
    await pickMainCpu(container, 8);
    await pickMainRam(container, 30);
    await pickDetachableDiskSize(300);
    expect(getRunningCost()).toEqual('$0.40 per hour');
    expect(getPausedCost()).toEqual('$0.02 per hour');

    await pickPresets(container, runtimePresets.generalAnalysis.displayName);
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
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
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

    const { container } = await component();

    // with Master disk size: 1000
    expect(screen.getByText('Cost when running')).toBeTruthy();
    expect(screen.getByText('Cost when paused')).toBeTruthy();

    expect(getRunningCost()).toEqual('$0.77 per hour');
    expect(getPausedCost()).toEqual('$0.07 per hour');

    // Change the Master disk size or master size to 150
    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);

    expect(screen.getByText('Cost when running')).toBeTruthy();
    expect(screen.getByText('Cost when paused')).toBeTruthy();

    expect(getRunningCost()).toEqual('$0.73 per hour');
    expect(getPausedCost()).toEqual('$0.02 per hour');
    // Switch to n1-highmem-4, double disk size.
    await pickMainRam(container, 26);
    await pickStandardDiskSize(2000);
    expect(getRunningCost()).toEqual('$0.87 per hour');
    expect(getPausedCost()).toEqual('$0.13 per hour');
  });

  it('should prevent runtime update when disk size is invalid', async () => {
    const { container } = await component();

    const getNextButton = () => screen.getByRole('button', { name: 'Next' });

    await pickDetachableDiskSize(49);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
    await pickComputeType(container, ComputeType.Dataproc);
    await pickWorkerDiskSize(49);
    expectButtonElementDisabled(getNextButton());

    await pickWorkerDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    expectButtonElementEnabled(getNextButton());
  });

  it('should prevent runtime update when PD disk size is invalid', async () => {
    const { container } = await component();
    const getNextButton = () => screen.getByRole('button', { name: 'Next' });

    await pickDetachableType(container, DiskType.STANDARD);
    await pickDetachableDiskSize(49);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableType(container, DiskType.SSD);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableDiskSize(4900);
    expectButtonElementDisabled(getNextButton());

    await pickDetachableType(container, DiskType.STANDARD);
    expectButtonElementDisabled(getNextButton());
  });

  it('should prevent runtime creation when disk size is invalid', async () => {
    setCurrentRuntime(null);

    const getCreateButton = () =>
      screen.getByRole('button', { name: 'Create' });

    const { container } = await component();
    clickExpectedButton('Customize');

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
      configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
      gceConfig: defaultGceConfig(),
      dataprocConfig: null,
    });

    const { container } = await component();
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
    const { container } = await component();
    await clickExpectedButton('Customize');
    await pickComputeType(container, ComputeType.Standard);
    await pickMainCpu(container, 8);
    await pickDetachableDiskSize(MIN_DISK_SIZE_GB);
    await clickExpectedButton('Create');
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig).toEqual(null);
    });
  });

  it('should allow creating gcePD with GPU', async () => {
    setCurrentRuntime(null);
    const { container } = await component();
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
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceConfig).toBeUndefined();
      expect(
        runtimeApiStub.runtime.gceWithPdConfig.persistentDisk.name
      ).toEqual('stub-disk');
      expect(
        runtimeApiStub.runtime.gceWithPdConfig.gpuConfig.numOfGpus
      ).toEqual(2);
    });
  });

  it('should allow disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    setCurrentDisk(existingDisk());

    const { container } = await component();
    await pickComputeType(container, ComputeType.Dataproc);

    clickExpectedButton('Next');
    expect(
      screen.getByText(
        /your environment currently has a reattachable disk, which will be unused after you apply this update\. /i
      )
    ).toBeTruthy();

    togglePDRadioButton();

    clickExpectedButton('Next');
    clickExpectedButton('Update');
    await waitFor(async () => {
      runtimeApiStub.runtime.status = RuntimeStatus.DELETED;
      expect(runtimeApiStub.runtime.gceWithPdConfig).not.toBeNull();
    });
    await waitFor(() => {}, { interval: 1000 });
    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
      expect(runtimeApiStub.runtime.gceWithPdConfig).toBeUndefined();
      // expect(runtimeApiStub.runtime.gceWithPdConfig).toBeNull();
    });
  });

  it('should allow skipping disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    const disk = existingDisk();
    setCurrentDisk(disk);

    const { container } = await component();
    await pickComputeType(container, ComputeType.Dataproc);

    clickExpectedButton('Next');

    expect(
      screen.getByText(
        /your environment currently has a reattachable disk, which will be unused after you apply this update\./i
      )
    ).toBeTruthy();

    // Default option should be NOT to delete.
    clickExpectedButton('Next');
    clickExpectedButton('Update');
    runtimeApiStub.runtime.status = RuntimeStatus.DELETED;

    await waitFor(() => {
      expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.CREATING);
      expect(disksApiStub.disk?.name).toEqual(disk.name);
    });
  });

  it('should prevent runtime creation when running cost is too high for free tier', async () => {
    setCurrentRuntime(null);
    const { container } = await component();
    clickExpectedButton('Customize');
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
    const { container } = await component();
    clickExpectedButton('Customize');

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

  it('should allow runtime creation when running cost is too high for user provided billing', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.OWNER,
      billingAccountName: 'user provided billing',
    });

    setCurrentRuntime(null);
    const { container } = await component();
    clickExpectedButton('Customize');

    await pickComputeType(container, ComputeType.Dataproc);
    await pickStandardDiskSize(DATAPROC_MIN_DISK_SIZE_GB);
    // This should make the cost about $50 per hour.
    await pickNumWorkers(20000);
    const createButton = screen.getByRole('button', { name: 'Create' });
    expectButtonElementEnabled(createButton);
  });

  it('should prevent runtime creation when running cost is too high for paid tier', async () => {
    setCurrentRuntime(null);
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    const { container } = await component();

    clickExpectedButton('Customize');

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
      screen.findByText(
        'Your runtime is expensive. Are you sure you wish to proceed?'
      )
    ).toBeTruthy();

    await pickNumWorkers(2);
    createButton = screen.getByRole('button', { name: 'Create' });
    // We don't want to disable for user provided billing. Just put a warning.
    expectButtonElementEnabled(createButton);
  });

  it('should disable worker count updates for stopped dataproc cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.STOPPED,
      configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    await component();

    const workerCountInput: HTMLInputElement = spinDiskElement('num-workers');
    expect(workerCountInput.disabled).toBeTruthy();

    const preemptibleCountInput: HTMLInputElement =
      spinDiskElement('num-preemptible');
    expect(preemptibleCountInput.disabled).toBeTruthy();
  });

  jest.setTimeout(50000);
  test.skip.each([
    [
      'disk type',
      [pickSsdType],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    [
      'disk decrease',
      [decrementDetachableDiskSize],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    // [
    //   'in-place + disk type',
    //   [changeMainCpu_To8, pickSsdType],
    //   { wantDeleteDisk: true, wantDeleteRuntime: true },
    // ],
    // [
    //   'in-place + disk decrease',
    //   [changeMainCpu_To8, decrementDetachableDiskSize],
    //   { wantDeleteDisk: true, wantDeleteRuntime: true },
    // ],
    ['recreate', [clickEnableGpu], { wantDeleteRuntime: true }],
    // [
    //   'recreate + disk type',
    //   [clickEnableGpu, pickSsdType],
    //   { wantDeleteDisk: true, wantDeleteRuntime: true },
    // ],
    // [
    //   'recreate + disk decrease',
    //   [clickEnableGpu, decrementDetachableDiskSize],
    //   { wantDeleteDisk: true, wantDeleteRuntime: true },
    // ],
  ] as DetachableDiskCase[])(
    'should allow runtime to recreate to attached PD: %s',
    async (desc: string, setters, expectations) => {
      setCurrentRuntime(detachableDiskRuntime());

      const disk = existingDisk();
      setCurrentDisk(disk);

      const { container } = await component();
      await runDetachableDiskCase(
        container,
        [desc, setters, expectations],
        disk.name
      );
    }
  );

  test.each([
    ['disk increase', [incrementDetachableDiskSize]],
    ['in-place', [changeMainCpu_To8]],
    [
      'in-place + disk increase',
      [changeMainCpu_To8, incrementDetachableDiskSize],
    ],
    ['recreate + disk increase', [clickEnableGpu, incrementDetachableDiskSize]],
  ])(
    'should allow runtime updates to attached PD: %s',
    async (desc: string, setters) => {
      setCurrentRuntime(detachableDiskRuntime());

      const disk = existingDisk();
      setCurrentDisk(disk);

      const { container } = await component();
      const updateRuntimeSpy = jest.spyOn(runtimeApi(), 'updateRuntime');

      for (const action of setters) {
        await action(container);
      }

      clickButtonIfVisible('Next');
      clickButtonIfVisible('Update');
      await waitFor(() => {
        expect(updateRuntimeSpy).toHaveBeenCalledTimes(1);
        expect(disksApiStub.disk.name).toEqual(disk.name);
      });
    }
  );

  test.each([
    ['disk type', [pickSsdType], { wantDeleteDisk: true }],
    ['disk decrease', [decrementDetachableDiskSize], { wantDeleteDisk: true }],
    ['disk increase', [incrementDetachableDiskSize], { wantUpdateDisk: true }],
  ] as DetachableDiskCase[])(
    'should allow runtime creates with existing disk: %s',
    async (desc, setters, expectations) => {
      setCurrentRuntime(null);

      const disk = existingDisk();
      setCurrentDisk(disk);

      const { container } = await component();
      clickExpectedButton('Customize');

      runDetachableDiskCase(
        container,
        [desc, setters, expectations],
        disk.name
      );
    }
  );
});
