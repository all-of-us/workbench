import '@testing-library/jest-dom';

import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  DataprocConfig,
  Disk,
  DiskType,
  GpuConfig,
  ListRuntimeResponse,
  PersistentDiskRequest,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
} from 'generated/fetch';

import { render, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  AnalysisConfig,
  AnalysisDiff,
  AnalysisDiffState,
  applyUpdate,
  canUseExistingDisk,
  compareGpu,
  diffsToUpdateMessaging,
  DiskConfig,
  findMostSevereDiffState,
  fromAnalysisConfig,
  getCreator,
  isVisible,
  maybeWithPersistentDisk,
  rebootUpdate,
  recreateEnvAndPDUpdate,
  recreateEnvUpdate,
  toAnalysisConfig,
  withAnalysisConfigDefaults,
} from 'app/utils/runtime-utils';
import {
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { stubDisk } from 'testing/stubs/disks-api-stub';
import {
  defaultDataprocConfig,
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
  defaultRuntime,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';

import {
  allMachineTypes,
  ComputeType,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
} from './machines';
import { runtimePresets } from './runtime-presets';
import {
  allMachineTypes,
  ComputeType,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
} from './machines';
import { useCustomRuntime } from './runtime-hooks';
import { runtimePresets } from './runtime-presets';

describe('runtime-utils', () => {
  const workspaceNamespace = 'test';

  const TestRuntime = ({ id }) => {
    const [{ currentRuntime }] = useCustomRuntime(
      workspaceNamespace,
      runtimeDiskStore.get().gcePersistentDisk
    );
    const { runtimeName = '' } = currentRuntime || {};
    return <div id={id}>{runtimeName}</div>;
  };

  const TestComponent = () => {
    return (
      <div>
        <TestRuntime id='1' />
        <TestRuntime id='2' />
      </div>
    );
  };

  let runtimeApiStub: RuntimeApiStub;

  beforeEach(() => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
    serverConfigStore.set({ config: { ...defaultServerConfig } });

    // For a component using the runtime store to function properly, there must
    // be an active workspace context provided - in the real application this is
    // configured by a central component. This line simulates what would
    // normally happen in WorkspaceWrapper.
    runtimeStore.set({
      workspaceNamespace,
      runtime: undefined,
      runtimeLoaded: true,
    });
    runtimeDiskStore.set({
      workspaceNamespace,
      gcePersistentDisk: undefined,
    });
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with a value', async () => {
    const { container } = render(<TestComponent />);

    // Runtime initialization is in progress at this point.
    const runtime = (id) => container.querySelector(`[id="${id}"]`);
    expect(runtime('1')).toHaveTextContent('');
    expect(runtime('2')).toHaveTextContent('');

    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('Runtime Name');
      expect(runtime('2')).toHaveTextContent('Runtime Name');
    });
  });

  it('should update when runtime store updates', async () => {
    const { container } = render(<TestComponent />);

    const runtime = (id) => container.querySelector(`[id="${id}"]`);

    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('Runtime Name');
      expect(runtime('2')).toHaveTextContent('Runtime Name');
    });

    act(() =>
      runtimeStore.set({
        ...runtimeStore.get(),
        runtime: {
          ...runtimeApiStub.runtime,
          runtimeName: 'foo',
        },
      })
    );
    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('foo');
      expect(runtime('2')).toHaveTextContent('foo');
    });
  });
});

describe(findMostSevereDiffState.name, () => {
  test.each([
    [[], undefined],
    [[AnalysisDiffState.NEEDS_DELETE], AnalysisDiffState.NEEDS_DELETE],
    [
      [AnalysisDiffState.NEEDS_DELETE, undefined],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NEEDS_DELETE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      ],
      AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    ],
    [
      [
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    ],
    [[AnalysisDiffState.NO_CHANGE], AnalysisDiffState.NO_CHANGE],
  ])('findMostSevereDiffState(%s) = %s', (diffStates, want) => {
    expect(findMostSevereDiffState(diffStates)).toEqual(want);
  });
});

describe(getCreator.name, () => {
  test.each([
    ['a runtime without a creator label', defaultRuntime(), undefined],
    [
      'a runtime with a creator label',
      { ...defaultRuntime(), labels: { creator: 'scientist@aou' } },
      'scientist@aou',
    ],
    ['a non-runtime object', {}, undefined],
    ['undefined', undefined, undefined],
    ['null', null, undefined],
  ])(
    'getCreator should have the expected result for %s',
    (
      desc: string,
      runtimeResponse: ListRuntimeResponse,
      expected: string | undefined
    ) => expect(getCreator(runtimeResponse)).toEqual(expected)
  );
});

describe(maybeWithPersistentDisk.name, () => {
  it('returns the existing runtime when dataproc', () => {
    const runtime = defaultDataProcRuntime();
    const disk = {
      ...stubDisk(),
      name: 'a non default value',
    };
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing runtime when a PD is already attached', () => {
    const runtime = defaultGceRuntimeWithPd();
    const disk = {
      ...stubDisk(),
      name: 'a non default value',
    };
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing GCE (no PD) runtime when a the disk is null', () => {
    const runtime = defaultGceRuntime();
    const disk = null;
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('returns the existing GCE (no PD) runtime when a the disk is undefined', () => {
    const runtime = defaultGceRuntime();
    const disk = undefined;
    const newRuntime = maybeWithPersistentDisk(runtime, disk);
    expect(newRuntime).toEqual(runtime);
  });

  it('adds a persistent disk to a GCE (no PD) runtime', () => {
    const runtime = defaultGceRuntime();
    const disk = stubDisk();

    const newRuntime = maybeWithPersistentDisk(runtime, disk);

    expect(newRuntime.gceWithPdConfig).toBeTruthy();
    expect(newRuntime.gceConfig).toBeFalsy();
    expect(newRuntime.dataprocConfig).toBeFalsy();

    // fields copied from the original runtime
    expect(newRuntime.runtimeName).toEqual(runtime.runtimeName);
    expect(newRuntime.googleProject).toEqual(runtime.googleProject);
    expect(newRuntime.status).toEqual(runtime.status);
    expect(newRuntime.createdDate).toEqual(runtime.createdDate);
    expect(newRuntime.toolDockerImage).toEqual(runtime.toolDockerImage);
    expect(newRuntime.configurationType).toEqual(runtime.configurationType);

    // fields copied from the disk
    expect(newRuntime.gceWithPdConfig.persistentDisk.size).toEqual(disk.size);
    expect(newRuntime.gceWithPdConfig.persistentDisk.name).toEqual(disk.name);
    expect(newRuntime.gceWithPdConfig.persistentDisk.diskType).toEqual(
      disk.diskType
    );
  });
});

// diff state combination logic is covered by the findMostSevereDiffState tests
// so here we can use single-element lists as inputs
describe(diffsToUpdateMessaging.name, () => {
  const baseDiff: AnalysisDiff = {
    desc: undefined, // these are not used by this function
    previous: undefined,
    new: undefined,
    diff: undefined, // tests will override
  };

  // diff = [NO_CHANGE, CAN_UPDATE_IN_PLACE] x diskDiff = [all states]
  test.each([
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.NO_CHANGE],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.CAN_UPDATE_IN_PLACE],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.CAN_UPDATE_WITH_REBOOT],
    [AnalysisDiffState.NO_CHANGE, AnalysisDiffState.NEEDS_DELETE],
    [AnalysisDiffState.CAN_UPDATE_IN_PLACE, AnalysisDiffState.NO_CHANGE],
    [
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    ],
    [
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    ],
    [AnalysisDiffState.CAN_UPDATE_IN_PLACE, AnalysisDiffState.NEEDS_DELETE],
  ])(
    'it returns an applyUpdate when the environment diff is %s and the diskDiff is %',
    (diff, diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff,
            diskDiff,
          },
        ])
      ).toEqual(applyUpdate)
  );

  test.each([
    AnalysisDiffState.NO_CHANGE,
    AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    AnalysisDiffState.NEEDS_DELETE,
  ])(
    'it returns a rebootUpdate when the environment diff is CAN_UPDATE_WITH_REBOOT and the diskDiff is %s',
    (diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff: AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
            diskDiff,
          },
        ])
      ).toEqual(rebootUpdate)
  );

  test.each([
    AnalysisDiffState.NO_CHANGE,
    AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
  ])(
    'it returns a recreateEnvUpdate when the environment diff is NEEDS_DELETE and the diskDiff is %s',
    (diskDiff) =>
      expect(
        diffsToUpdateMessaging([
          {
            ...baseDiff,
            diff: AnalysisDiffState.NEEDS_DELETE,
            diskDiff,
          },
        ])
      ).toEqual(recreateEnvUpdate)
  );

  it('returns a recreateEnvAndPDUpdate when both the environment diff and disk diff are NEEDS_DELETE', () =>
    expect(
      diffsToUpdateMessaging([
        {
          ...baseDiff,
          diff: AnalysisDiffState.NEEDS_DELETE,
          diskDiff: AnalysisDiffState.NEEDS_DELETE,
        },
      ])
    ).toEqual(recreateEnvAndPDUpdate));
});

// only testing the 'diff' field because the others are straightforward
describe(compareGpu.name, () => {
  const baseAnalysisConfig: AnalysisConfig = {
    computeType: undefined, // none of these are used - only gpuConfig
    machine: undefined,
    diskConfig: undefined,
    detachedDisk: undefined,
    dataprocConfig: undefined,
    autopauseThreshold: undefined,
    gpuConfig: undefined, // will be overridden
  };
  const baseGpuConfig: GpuConfig = {
    gpuType: 'the standard one',
    numOfGpus: 1,
  };

  it('returns a NO_CHANGE diff when neither the old or new config specifies GPUs', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: undefined },
        { ...baseAnalysisConfig, gpuConfig: undefined }
      )?.diff
    ).toEqual(AnalysisDiffState.NO_CHANGE));

  it('returns a NO_CHANGE diff when configs are identical', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig },
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig }
      )?.diff
    ).toEqual(AnalysisDiffState.NO_CHANGE));

  it('returns a NEEDS_DELETE diff when adding a gpuConfig', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: undefined },
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when removing a gpuConfig', () =>
    expect(
      compareGpu(
        { ...baseAnalysisConfig, gpuConfig: baseGpuConfig },
        { ...baseAnalysisConfig, gpuConfig: undefined }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU type differs', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: { ...baseGpuConfig, gpuType: 'red' },
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: { ...baseGpuConfig, gpuType: 'blue' },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU count increases', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: baseGpuConfig,
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: {
            ...baseGpuConfig,
            numOfGpus: baseGpuConfig.numOfGpus + 1,
          },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));

  it('returns a NEEDS_DELETE diff when GPU count decreases', () =>
    expect(
      compareGpu(
        {
          ...baseAnalysisConfig,
          gpuConfig: baseGpuConfig,
        },
        {
          ...baseAnalysisConfig,
          gpuConfig: {
            ...baseGpuConfig,
            numOfGpus: baseGpuConfig.numOfGpus - 1,
          },
        }
      )?.diff
    ).toEqual(AnalysisDiffState.NEEDS_DELETE));
});

const defaultDiskSize = 100;
const defaultDiskType = DiskType.STANDARD;
const defaultDiskName = 'my disk';
const dataprocConfig = defaultDataprocConfig();

const defaultAnalysisConfig: AnalysisConfig = {
  computeType: ComputeType.Standard,
  machine: DEFAULT_MACHINE_TYPE,
  diskConfig: {
    size: defaultDiskSize,
    existingDiskName: defaultDiskName,
    detachableType: defaultDiskType,
    detachable: true,
  },
  detachedDisk: {
    size: defaultDiskSize,
    name: defaultDiskName,
    diskType: defaultDiskType,
    gceRuntime: true,
    blockSize: 1,
  },
  dataprocConfig,
  autopauseThreshold: 100,
  gpuConfig: {
    gpuType: 'the normal kind',
    numOfGpus: 1,
  },
  numNodes: dataprocConfig.numberOfWorkers + 1, // +1 for the master node
};

describe(fromAnalysisConfig.name, () => {
  it('should ignore numNodes', () => {
    const testConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
      numNodes: 123,
    };

    // sanity check - they're not accidentally equal
    expect(testConfig.numNodes).not.toEqual(
      testConfig.dataprocConfig.numberOfWorkers + 1
    );

    const {
      dataprocConfig: { numberOfWorkers },
    } = fromAnalysisConfig(testConfig);
    const totalNodes = numberOfWorkers + 1; // master mode
    expect(totalNodes).not.toEqual(testConfig.numNodes);
  });

  it('should ignore the detachedDisk fields', () => {
    const detachedDisk = {
      ...defaultAnalysisConfig.detachedDisk,
      size: defaultDiskSize + 1,
      name: 'my other disk',
      diskType: DiskType.SSD,
    };
    const testConfig = { ...defaultAnalysisConfig, detachedDisk };

    // sanity check: the detached disk differs in these 3 fields
    expect(testConfig.detachedDisk.size).not.toEqual(
      testConfig.diskConfig.size
    );
    expect(testConfig.detachedDisk.name).not.toEqual(
      testConfig.diskConfig.existingDiskName
    );
    expect(testConfig.detachedDisk.diskType).not.toEqual(
      testConfig.diskConfig.detachableType
    );

    const {
      gceWithPdConfig: { persistentDisk },
    } = fromAnalysisConfig(testConfig);
    expect(persistentDisk.size).not.toEqual(detachedDisk.size);
    expect(persistentDisk.name).not.toEqual(detachedDisk.name);
    expect(persistentDisk.diskType).not.toEqual(detachedDisk.diskType);
  });

  it('should return a Dataproc runtime if the computeType is Dataproc', () => {
    const oldMachineType = 'this will be overridden';
    const newMachineType = 'I will override you';
    const oldDiskSize = 123;
    const newDiskSize = 456;

    const testConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Dataproc,
      dataprocConfig: {
        ...defaultAnalysisConfig.dataprocConfig,
        masterMachineType: oldMachineType,
        masterDiskSize: oldDiskSize,
      },
      machine: { ...defaultAnalysisConfig.machine, name: newMachineType },
      diskConfig: { ...defaultAnalysisConfig.diskConfig, size: newDiskSize },
    };

    const runtime = fromAnalysisConfig(testConfig);

    // expect that only 1 of these 3 is populated, and it's Dataproc
    expect(runtime.dataprocConfig).toBeTruthy();
    expect(runtime.gceConfig).toBeFalsy();
    expect(runtime.gceWithPdConfig).toBeFalsy();

    expect(runtime.autopauseThreshold).toEqual(testConfig.autopauseThreshold);

    // expect these 2 fields to be overridden
    expect(runtime.dataprocConfig.masterMachineType).toEqual(newMachineType);
    expect(runtime.dataprocConfig.masterDiskSize).toEqual(newDiskSize);

    // expect all other fields in the dataprocConfig to be populated directly
    const originalPlusOverrides = {
      ...testConfig.dataprocConfig,
      masterMachineType: newMachineType,
      masterDiskSize: newDiskSize,
    };
    expect(runtime.dataprocConfig).toEqual(originalPlusOverrides);
  });

  it('should return a GCE with PD runtime if the computeType is Standard and the disk is detachable', () => {
    const testConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard,
      diskConfig: { ...defaultAnalysisConfig.diskConfig, detachable: true },
    };
    const expectedPD = {
      size: testConfig.diskConfig.size,
      diskType: testConfig.diskConfig.detachableType,
      labels: {},
      name: testConfig.diskConfig.existingDiskName,
    };

    const runtime = fromAnalysisConfig(testConfig);

    // expect that only 1 of these 3 is populated, and it's GCE with PD
    expect(runtime.gceWithPdConfig).toBeTruthy();
    expect(runtime.dataprocConfig).toBeFalsy();
    expect(runtime.gceConfig).toBeFalsy();

    expect(runtime.autopauseThreshold).toEqual(testConfig.autopauseThreshold);
    expect(runtime.gceWithPdConfig.machineType).toEqual(
      testConfig.machine.name
    );
    expect(runtime.gceWithPdConfig.gpuConfig).toEqual(testConfig.gpuConfig);

    expect(runtime.gceWithPdConfig.persistentDisk).toEqual(expectedPD);
  });

  it('should return a GCE (without PD) runtime if the computeType is Standard and the disk is not detachable', () => {
    const testConfig = {
      ...defaultAnalysisConfig,
      computeType: ComputeType.Standard,
      diskConfig: { ...defaultAnalysisConfig.diskConfig, detachable: false },
    };

    const runtime = fromAnalysisConfig(testConfig);

    // expect that only 1 of these 3 is populated, and it's GCE without PD
    expect(runtime.gceConfig).toBeTruthy();
    expect(runtime.gceWithPdConfig).toBeFalsy();
    expect(runtime.dataprocConfig).toBeFalsy();

    expect(runtime.autopauseThreshold).toEqual(testConfig.autopauseThreshold);
    expect(runtime.gceConfig.machineType).toEqual(testConfig.machine.name);
    expect(runtime.gceConfig.gpuConfig).toEqual(testConfig.gpuConfig);
  });

  const generalTemplate = runtimePresets.generalAnalysis.runtimeTemplate;
  const testConfigForGeneralPreset = {
    ...defaultAnalysisConfig,

    computeType: ComputeType.Standard,

    gpuConfig: generalTemplate.gceWithPdConfig.gpuConfig,

    // overridden fields for gceWithPdConfig
    diskConfig: {
      ...defaultAnalysisConfig.diskConfig,
      size: generalTemplate.gceWithPdConfig.persistentDisk.size,
      detachableType: generalTemplate.gceWithPdConfig.persistentDisk.diskType,
      existingDiskName: generalTemplate.gceWithPdConfig.persistentDisk.name,
    },
  };

  it('should populate the configuration type for the generalAnalysis preset', () => {
    const runtime = fromAnalysisConfig(testConfigForGeneralPreset);
    expect(runtime.configurationType).toEqual(
      generalTemplate.configurationType
    );
  });

  it('should populate the configuration type USER_OVERRIDE for a deviation from the generalAnalysis preset', () => {
    const testConfig = {
      ...testConfigForGeneralPreset,
      // preset gpuConfig is null
      gpuConfig: {
        gpuType: 'something',
        numOfGpus: 1,
      },
    };
    const runtime = fromAnalysisConfig(testConfig);
    expect(runtime.configurationType).toEqual(
      RuntimeConfigurationType.USER_OVERRIDE
    );
  });

  const hailTemplate = runtimePresets.hailAnalysis.runtimeTemplate;
  const testConfigForHailPreset = {
    ...defaultAnalysisConfig,

    computeType: ComputeType.Dataproc,
    dataprocConfig: hailTemplate.dataprocConfig,

    // overridden fields for dataproc
    machine: {
      ...defaultAnalysisConfig.machine,
      name: hailTemplate.dataprocConfig.masterMachineType,
    },
    diskConfig: {
      ...defaultAnalysisConfig.diskConfig,
      size: hailTemplate.dataprocConfig.masterDiskSize,
    },
  };

  it('should populate the configuration type for the hailAnalysis preset', () => {
    const runtime = fromAnalysisConfig(testConfigForHailPreset);
    expect(runtime.configurationType).toEqual(hailTemplate.configurationType);
  });

  it('should populate the configuration type USER_OVERRIDE for a deviation from the hailAnalysis preset', () => {
    const testConfig = {
      ...testConfigForHailPreset,
      dataprocConfig: {
        ...testConfigForHailPreset.dataprocConfig,
        numberOfWorkers:
          testConfigForHailPreset.dataprocConfig.numberOfWorkers + 1,
      },
    };
    const runtime = fromAnalysisConfig(testConfig);
    expect(runtime.configurationType).toEqual(
      RuntimeConfigurationType.USER_OVERRIDE
    );
  });
});

describe(toAnalysisConfig.name, () => {
  it('populates from a GCE Runtime', () => {
    const machineIndex = 3; // arbitrary
    const gpuConfig = { gpuType: 'a good one', numOfGpus: 2 };
    const testRuntime: Runtime = {
      gceConfig: {
        machineType: allMachineTypes[machineIndex].name, // needs to match one of these
        diskSize: 123,
        gpuConfig,
      },
      autopauseThreshold: 456,
    };
    const detachedDisk: Disk = stubDisk();

    const expectedDiskConfig = {
      size: testRuntime.gceConfig.diskSize,
      detachable: false,
      detachableType: null,
      existingDiskName: null,
    };

    const config = toAnalysisConfig(testRuntime, detachedDisk);

    expect(config.computeType).toEqual(ComputeType.Standard);
    expect(config.machine).toEqual(allMachineTypes[machineIndex]);
    expect(config.diskConfig).toEqual(expectedDiskConfig);
    expect(config.detachedDisk).toEqual(detachedDisk);
    expect(config.autopauseThreshold).toEqual(testRuntime.autopauseThreshold);
    expect(config.gpuConfig).toEqual(gpuConfig);
    expect(config.dataprocConfig).toBeNull();
  });

  // simplest case for 'GCE Runtime with a PD' - check all fields here
  it('populates from a GCE Runtime with a PD when there is no existing disk', () => {
    const machineIndex = 2; // arbitrary
    const gpuConfig: GpuConfig = { gpuType: 'a good one', numOfGpus: 2 };
    const persistentDisk: PersistentDiskRequest = {
      size: 123,
      diskType: DiskType.SSD,
      name: 'the disk associated with the runtime',
    };
    const testRuntime: Runtime = {
      gceWithPdConfig: {
        machineType: allMachineTypes[machineIndex].name, // needs to match one of these
        gpuConfig,
        persistentDisk,
      },
      autopauseThreshold: 456,
    };

    const expectedDiskConfig = {
      size: persistentDisk.size,
      detachable: true,
      detachableType: persistentDisk.diskType,
      existingDiskName: null, // not persistentDisk.name
    };

    const config = toAnalysisConfig(testRuntime, null);

    expect(config.computeType).toEqual(ComputeType.Standard);
    expect(config.machine).toEqual(allMachineTypes[machineIndex]);
    expect(config.autopauseThreshold).toEqual(testRuntime.autopauseThreshold);
    expect(config.gpuConfig).toEqual(gpuConfig);
    expect(config.dataprocConfig).toBeNull();
    expect(config.detachedDisk).toBeNull();
    expect(config.diskConfig).toEqual(expectedDiskConfig);
  });

  // only check fields which differ from the no-existing-disk cases
  it('populates from a GCE Runtime with a PD when an existing disk is appropriate for association', () => {
    const machineIndex = 2; // arbitrary
    const gpuConfig: GpuConfig = { gpuType: 'a good one', numOfGpus: 2 };
    const persistentDisk: PersistentDiskRequest = {
      size: 123,
      diskType: DiskType.SSD,
      name: 'the disk associated with the runtime',
    };
    const testRuntime: Runtime = {
      gceWithPdConfig: {
        machineType: allMachineTypes[machineIndex].name, // needs to match one of these
        gpuConfig,
        persistentDisk,
      },
      autopauseThreshold: 456,
    };

    const existingDisk: Disk = {
      diskType: persistentDisk.diskType,
      size: persistentDisk.size - 1, // equal or smaller will match
      name: 'my favorite disk',
      blockSize: undefined, // not important here, but required
    };

    const expectedDiskConfig = {
      size: persistentDisk.size,
      detachable: true,
      detachableType: persistentDisk.diskType,
      existingDiskName: existingDisk.name, // not persistentDisk.name
    };

    const config = toAnalysisConfig(testRuntime, existingDisk);

    expect(config.diskConfig).toEqual(expectedDiskConfig);
  });

  it('populates from a GCE Runtime with a PD when an existing disk is too big', () => {
    const machineIndex = 2; // arbitrary
    const gpuConfig: GpuConfig = { gpuType: 'a good one', numOfGpus: 2 };
    const persistentDisk: PersistentDiskRequest = {
      size: 123,
      diskType: DiskType.SSD,
      name: 'the disk associated with the runtime',
    };
    const testRuntime: Runtime = {
      gceWithPdConfig: {
        machineType: allMachineTypes[machineIndex].name, // needs to match one of these
        gpuConfig,
        persistentDisk,
      },
      autopauseThreshold: 456,
    };

    const existingDisk: Disk = {
      diskType: persistentDisk.diskType,
      size: persistentDisk.size + 1, // too big, will not match
      name: 'my favorite disk',
      blockSize: undefined, // not important here, but required
    };

    const expectedDiskConfig = {
      size: persistentDisk.size,
      detachable: true,
      detachableType: persistentDisk.diskType,
      existingDiskName: null, // not persistentDisk.name and not existingDisk.name
    };

    const config = toAnalysisConfig(testRuntime, existingDisk);

    expect(config.diskConfig).toEqual(expectedDiskConfig);
  });

  it('populates from a GCE Runtime with a PD when an existing disk is the wrong type', () => {
    const machineIndex = 2; // arbitrary
    const gpuConfig: GpuConfig = { gpuType: 'a good one', numOfGpus: 2 };
    const persistentDisk: PersistentDiskRequest = {
      size: 123,
      diskType: DiskType.SSD,
      name: 'the disk associated with the runtime',
    };
    const testRuntime: Runtime = {
      gceWithPdConfig: {
        machineType: allMachineTypes[machineIndex].name, // needs to match one of these
        gpuConfig,
        persistentDisk,
      },
      autopauseThreshold: 456,
    };

    const existingDisk: Disk = {
      diskType: DiskType.STANDARD, // does not match persistentDisk.diskType
      size: persistentDisk.size,
      name: 'my favorite disk',
      blockSize: undefined, // not important here, but required
    };

    const expectedDiskConfig = {
      size: persistentDisk.size,
      detachable: true,
      detachableType: persistentDisk.diskType,
      existingDiskName: null, // not persistentDisk.name and not existingDisk.name
    };

    const config = toAnalysisConfig(testRuntime, existingDisk);

    expect(config.diskConfig).toEqual(expectedDiskConfig);
  });

  it('populates from a Dataproc Runtime', () => {
    const machineIndex = 5; // arbitrary
    const testRuntime: Runtime = {
      dataprocConfig: {
        masterMachineType: allMachineTypes[machineIndex].name, // needs to match one of these
        masterDiskSize: 123,
      },
      autopauseThreshold: 456,
    };
    const detachedDisk: Disk = stubDisk();

    const expectedDiskConfig = {
      size: testRuntime.dataprocConfig.masterDiskSize,
      detachable: false,
      detachableType: null,
      existingDiskName: null,
    };

    const config = toAnalysisConfig(testRuntime, detachedDisk);

    expect(config.computeType).toEqual(ComputeType.Dataproc);
    expect(config.machine).toEqual(allMachineTypes[machineIndex]);
    expect(config.diskConfig).toEqual(expectedDiskConfig);
    expect(config.detachedDisk).toEqual(detachedDisk);
    expect(config.autopauseThreshold).toEqual(testRuntime.autopauseThreshold);
    expect(config.gpuConfig).toBeNull();
    expect(config.dataprocConfig).toEqual(testRuntime.dataprocConfig);
  });

  // TODO: when/why would this happen?
  it('populates from a degenerate Runtime with no GCE or Dataproc config', () => {
    const badRuntime = {};
    const detachedDisk: Disk = stubDisk();
    const expectedDiskConfig = {
      size: null,
      detachable: null,
      detachableType: null,
      existingDiskName: null,
    };

    const config = toAnalysisConfig(badRuntime, detachedDisk);

    expect(config.diskConfig).toEqual(expectedDiskConfig);
    expect(config.detachedDisk).toEqual(detachedDisk);

    expect(config.computeType).toBeNull();
    expect(config.machine).toBeNull();
    expect(config.autopauseThreshold).toBeNull();
    expect(config.dataprocConfig).toBeNull();
    expect(config.gpuConfig).toBeNull();
  });
});

describe(withAnalysisConfigDefaults.name, () => {
  describe(ComputeType.Standard, () => {
    test.each([
      [
        // fully-specified: no relevant fields are missing
        // matches the config: canUseExistingDisk() succeeds
        'with an existing fully-specified disk that matches the config',
        {
          ...defaultAnalysisConfig.detachedDisk,
          name: 'a different name',
        },
        {
          size: defaultAnalysisConfig.detachedDisk.size,
          detachable: true, // always set to true for Standard (not Dataproc) config
          detachableType: defaultAnalysisConfig.detachedDisk.diskType,
          existingDiskName: 'a different name',
        },
      ],
      [
        // fully-specified: no relevant fields are missing
        "with an existing fully-specified disk that doesn't match the config",
        {
          ...defaultAnalysisConfig.detachedDisk,
          size: defaultAnalysisConfig.detachedDisk.size + 1, // too big means it doesn't match
          name: 'a different name',
        },
        {
          size: defaultAnalysisConfig.detachedDisk.size,
          detachable: true, // always set to true for Standard (not Dataproc) config
          detachableType: defaultAnalysisConfig.detachedDisk.diskType,
          existingDiskName: null, // clears the existing name, if any
        },
      ],
      [
        'with no existing disk',
        undefined,
        {
          size: defaultAnalysisConfig.diskConfig.size, // this is only true when the value exists (missing test is below)
          detachable: true, // always set to true for Standard (not Dataproc) config
          detachableType: DiskType.STANDARD,
          existingDiskName: null,
        },
      ],
    ])(
      'it modifies a Standard config %s',
      (_desc, inputDisk: Disk, expectedDiskConfig: DiskConfig) => {
        const inputConfig = {
          ...defaultAnalysisConfig,
          diskConfig: {
            ...defaultAnalysisConfig.diskConfig,
            detachable: false, // to show that this gets overridden
          },
        };

        // sanity check
        expect(inputConfig.computeType).toEqual(ComputeType.Standard);

        const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

        expect(outConfig.computeType).toEqual(ComputeType.Standard);
        expect(outConfig.diskConfig).toEqual(expectedDiskConfig);

        // these pass through unchanged if present (later tests show behavior when missing)

        expect(outConfig.machine).toBeDefined();
        expect(outConfig.machine).toEqual(inputConfig.machine);
        expect(outConfig.dataprocConfig).toBeDefined(); // arguably this should be removed b/c incompatible with Standard
        expect(outConfig.dataprocConfig).toEqual(inputConfig.dataprocConfig);
        expect(outConfig.gpuConfig).toBeDefined();
        expect(outConfig.gpuConfig).toEqual(inputConfig.gpuConfig);
        expect(outConfig.autopauseThreshold).toBeDefined();
        expect(outConfig.autopauseThreshold).toEqual(
          inputConfig.autopauseThreshold
        );
      }
    );

    it('should treat a config with a missing compute type as Standard', () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        computeType: undefined,
      };

      const expectedDiskConfig: DiskConfig = {
        size: defaultAnalysisConfig.diskConfig.size, // this is only true when the value exists (missing test is below)
        detachable: true, // always set to true for Standard (not Dataproc) config
        detachableType: DiskType.STANDARD,
        existingDiskName: null,
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, undefined);

      expect(outConfig.computeType).toEqual(ComputeType.Standard);
      expect(outConfig.diskConfig).toEqual(expectedDiskConfig);

      // these pass through unchanged if present (later tests show behavior when missing)

      expect(outConfig.machine).toBeDefined();
      expect(outConfig.machine).toEqual(inputConfig.machine);
      expect(outConfig.dataprocConfig).toBeDefined(); // arguably this should be removed b/c incompatible with Standard
      expect(outConfig.dataprocConfig).toEqual(inputConfig.dataprocConfig);
      expect(outConfig.gpuConfig).toBeDefined();
      expect(outConfig.gpuConfig).toEqual(inputConfig.gpuConfig);
      expect(outConfig.autopauseThreshold).toBeDefined();
      expect(outConfig.autopauseThreshold).toEqual(
        inputConfig.autopauseThreshold
      );
    });

    it("should replace a missing diskConfig size with the persistent disk's when it exists", () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        diskConfig: { ...defaultAnalysisConfig.diskConfig, size: undefined },
      };
      const size = 789;
      const inputDisk: Disk = {
        size,
        diskType: defaultAnalysisConfig.detachedDisk.diskType,
        blockSize: 0,
        name: 'whatever',
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

      expect(outConfig.diskConfig.size).toEqual(size);
    });

    it('should replace a missing diskConfig size with DEFAULT_DISK_SIZE when the persistent disk is missing size', () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        diskConfig: { ...defaultAnalysisConfig.diskConfig, size: undefined },
      };
      const inputDisk: Disk = {
        size: undefined,
        diskType: defaultAnalysisConfig.detachedDisk.diskType,
        blockSize: 0,
        name: 'whatever',
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

      expect(outConfig.diskConfig.size).toEqual(DEFAULT_DISK_SIZE);
    });

    it("should replace a missing diskConfig type with the persistent disk's when it exists", () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        diskConfig: {
          ...defaultAnalysisConfig.diskConfig,
          diskType: undefined,
        },
      };
      const inputDisk: Disk = {
        size: 1000,
        diskType: defaultAnalysisConfig.detachedDisk.diskType,
        blockSize: 0,
        name: 'whatever',
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

      expect(outConfig.diskConfig.detachableType).toEqual(inputDisk.diskType);
    });

    it("should replace a missing diskConfig type with STANDARD when the persistent disk also doesn't have one", () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        diskConfig: {
          ...defaultAnalysisConfig.diskConfig,
          diskType: undefined,
        },
      };
      const inputDisk: Disk = {
        size: 1000,
        diskType: undefined,
        blockSize: 0,
        name: 'whatever',
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

      expect(outConfig.diskConfig.detachableType).toEqual(DiskType.STANDARD);
    });
  });

  describe(ComputeType.Dataproc, () => {
    it('modifies a Dataproc config', () => {
      const inputConfig = {
        ...defaultAnalysisConfig,
        computeType: ComputeType.Dataproc,
      };
      const inputDisk = stubDisk();

      const expectedDiskConfig: DiskConfig = {
        size: inputConfig.diskConfig.size,
        detachable: false,
        detachableType: null,
        existingDiskName: null,
      };

      // yes it removes 3 fields.  why?
      const expectedDataprocConfig: DataprocConfig = {
        ...inputConfig.dataprocConfig,
        masterMachineType: undefined,
        masterDiskSize: undefined,
        numberOfWorkerLocalSSDs: undefined,
      };

      const outConfig = withAnalysisConfigDefaults(inputConfig, inputDisk);

      expect(outConfig.computeType).toEqual(ComputeType.Dataproc);
      expect(outConfig.diskConfig).toEqual(expectedDiskConfig);
      expect(outConfig.dataprocConfig).toEqual(expectedDataprocConfig);
      expect(outConfig.gpuConfig).toBeNull();
      expect(outConfig.detachedDisk).toEqual(inputDisk);

      // these pass through unchanged if present (later tests show behavior when missing)

      expect(outConfig.machine).toBeDefined();
      expect(outConfig.machine).toEqual(inputConfig.machine);
      expect(outConfig.autopauseThreshold).toBeDefined();
      expect(outConfig.autopauseThreshold).toEqual(
        inputConfig.autopauseThreshold
      );
    });

    // TODO: tests for Dataproc with replacements
  });

  // TODO: tests to replace machine and autopause threshold
});

// TODO
describe(diffsToUpdateMessaging.name, () => {});
describe(compareGpu.name, () => {});
describe(fromAnalysisConfig.name, () => {});
describe(maybeWithExistingDiskName.name, () => {});
describe(withAnalysisConfigDefaults.name, () => {});
describe(toAnalysisConfig.name, () => {});
describe(isVisible.name, () => {});
describe(canUseExistingDisk.name, () => {});
