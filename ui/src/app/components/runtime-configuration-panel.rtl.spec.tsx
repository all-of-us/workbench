import '@testing-library/jest-dom';

import {
  DisksApi,
  GpuConfig,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AnalysisConfig, toAnalysisConfig } from 'app/utils/analysis-config';
import {
  DATAPROC_MIN_DISK_SIZE_GB,
  findMachineByName,
  Machine,
  MIN_DISK_SIZE_GB,
} from 'app/utils/machines';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { runtimePresets } from 'app/utils/runtime-presets';
import {
  cdrVersionStore,
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  defaultDataProcRuntime,
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

  const component = (
    propOverrides?: Partial<RuntimeConfigurationPanelProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(<RuntimeConfigurationPanel {...allProps} />);
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

  it('should show loading spinner while loading', async () => {
    // simulate not done loading
    runtimeStore.set({ ...runtimeStore.get(), runtimeLoaded: false });

    const { container } = component();
    expect(container).toBeInTheDocument();

    await waitFor(() => {
      // spinner label
      expect(screen.queryByLabelText('Please Wait')).toBeInTheDocument();
    });

    runtimeStore.set({ ...runtimeStore.get(), runtimeLoaded: true });

    await waitFor(() => {
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument();
    });
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
});
