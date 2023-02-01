import * as React from 'react';
import { act } from 'react-dom/test-utils';
import * as fp from 'lodash/fp';
import { ReactWrapper } from 'enzyme';

import {
  BillingStatus,
  DisksApi,
  ProfileApi,
  RuntimeConfigurationType,
  RuntimeStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';
import { Disk, DiskType, Runtime, RuntimeApi } from 'generated/fetch/api';

import { Button, LinkButton } from 'app/components/buttons';
import { RadioButton } from 'app/components/inputs';
import { WarningMessage } from 'app/components/messages';
import { RuntimeConfigurationPanel } from 'app/components/runtime-configuration-panel';
import { ConfirmDelete } from 'app/components/runtime-configuration-panel/confirm-delete';
import { Spinner } from 'app/components/spinners';
import {
  disksApi,
  profileApi,
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';
import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  findMachineByName,
  MIN_DISK_SIZE_GB,
} from 'app/utils/machines';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { runtimePresets } from 'app/utils/runtime-presets';
import { diskTypeLabels, isActionable } from 'app/utils/runtime-utils';
import {
  cdrVersionStore,
  clearCompoundRuntimeOperations,
  diskStore,
  profileStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitForFakeTimersAndUpdate,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import {
  defaultDataprocConfig,
  defaultGceConfig,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

interface Props {
  onClose: () => void;
}

describe('RuntimeConfigurationPanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;
  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;
  let onClose: () => void;
  let enableGpu: boolean;
  let enablePersistentDisk: boolean;
  let freeTierBillingAccountId: string;

  const component = async (propOverrides?: object) => {
    const allProps = { ...props, ...propOverrides };
    const c = mountWithRouter(<RuntimeConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
  };

  const existingDisk = (): Disk => {
    return {
      size: 1000,
      diskType: DiskType.Standard,
      name: 'my-existing-disk',
      blockSize: 1,
      isGceRuntime: true,
    };
  };

  const costEstimator = (wrapper) =>
    wrapper.find('[data-test-id="cost-estimator"]');

  const runningCost = (wrapper) =>
    costEstimator(wrapper).find('[data-test-id="running-cost"]');
  const storageCost = (wrapper) =>
    costEstimator(wrapper).find('[data-test-id="storage-cost"]');

  const detachableDiskRuntime = (): Runtime => {
    const { size, diskType, name } = existingDisk();
    return {
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Running,
      configurationType: RuntimeConfigurationType.UserOverride,
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

  let runtimeStoreStub;
  let diskStoreStub;

  const setCurrentDisk = (d: Disk) => {
    disksApiStub.disk = d;
    diskStoreStub.gcePersistentDisk = d;
  };

  const setCurrentRuntime = (r: Runtime) => {
    runtimeApiStub.runtime = r;
    runtimeStoreStub.runtime = r;
  };

  beforeEach(async () => {
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: { ...defaultServerConfig } });
    enableGpu = serverConfigStore.get().config.enableGpu;
    enablePersistentDisk = serverConfigStore.get().config.enablePersistentDisk;
    freeTierBillingAccountId =
      serverConfigStore.get().config.freeTierBillingAccountId;

    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    onClose = jest.fn();
    props = {
      onClose,
    };

    cdrVersionStore.set(cdrVersionTiersResponse);

    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName: 'billingAccounts/' + freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      googleProject: runtimeApiStub.runtime.googleProject,
    });

    runtimeStoreStub = {
      runtime: runtimeApiStub.runtime,
      workspaceNamespace: workspaceStubs[0].namespace,
      runtimeLoaded: true,
    };
    runtimeStore.set(runtimeStoreStub);

    diskStoreStub = {
      workspaceNamespace: workspaceStubs[0].namespace,
      gcePersistentDisk: null,
    };
    diskStore.set(diskStoreStub);

    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  const expectEqualFields = (a, b, fieldNames) => {
    const pick = fp.flow(fp.pick(fieldNames));
    expect(pick(a)).toEqual(pick(b));
  };

  const pickDropdownOption = async (wrapper, id, label) => {
    wrapper.find(id).first().simulate('click');
    const item = wrapper
      .find(`${id} .p-dropdown-item`)
      .find({ 'aria-label': label })
      .first();
    expect(item.exists()).toBeTruthy();

    item.simulate('click');

    // In some cases, picking an option may require some waiting, e.g. for
    // rerendering of RAM options based on CPU selection.
    await waitOneTickAndUpdate(wrapper);
  };

  const getInputValue = (wrapper, id) => {
    return wrapper.find(id).first().prop('value');
  };

  const enterNumberInput = async (wrapper, id, value) => {
    // TODO: Find a way to invoke this without props.
    act(() => {
      wrapper.find(id).first().prop('onChange')({ value } as any);
    });
    await waitOneTickAndUpdate(wrapper);
  };

  function getCheckbox(wrapper, id): boolean {
    return wrapper.find(id).first().prop('checked');
  }

  async function clickCheckbox(wrapper, id) {
    const currentChecked = getCheckbox(wrapper, id);
    wrapper
      .find(id)
      .find('input')
      .first()
      .simulate('change', { target: { checked: !currentChecked } });
    await waitOneTickAndUpdate(wrapper);
  }

  const clickDeletePdRadioButton = (wrapper) =>
    wrapper
      .find({ 'data-test-id': 'delete-unattached-pd-radio' })
      .first()
      .simulate('change');

  const getMainCpu = (wrapper) => getInputValue(wrapper, '#runtime-cpu');
  const pickMainCpu = (wrapper, cpu) =>
    pickDropdownOption(wrapper, '#runtime-cpu', cpu);

  const getMainRam = (wrapper) => getInputValue(wrapper, '#runtime-ram');
  const pickMainRam = (wrapper, ram) =>
    pickDropdownOption(wrapper, '#runtime-ram', ram);

  const getMainDiskSize = (wrapper) =>
    getInputValue(
      wrapper,
      enablePersistentDisk ? '#standard-disk' : '#runtime-disk'
    );
  const pickMainDiskSize = (wrapper, diskSize) =>
    enterNumberInput(
      wrapper,
      enablePersistentDisk ? '#standard-disk' : '#runtime-disk',
      diskSize
    );

  const enableDetachable = (wrapper, detachable = true) =>
    wrapper
      .find({ name: detachable ? 'detachableDisk' : 'standardDisk' })
      .first()
      .simulate('change');

  const pickDetachableType = (wrapper, diskType: DiskType) =>
    pickDropdownOption(wrapper, '#disk-type', diskTypeLabels[diskType]);

  const getDetachableDiskSize = (wrapper) =>
    getInputValue(wrapper, '#detachable-disk');
  const pickDetachableDiskSize = (wrapper, diskSize) =>
    enterNumberInput(wrapper, '#detachable-disk', diskSize);

  const pickGpuType = (wrapper, gpuType) =>
    pickDropdownOption(wrapper, '#gpu-type', gpuType);

  const pickGpuNum = (wrapper, gpuNum) =>
    pickDropdownOption(wrapper, '#gpu-num', gpuNum);

  const clickEnableGpu = (wrapper) => clickCheckbox(wrapper, '#enable-gpu');

  const pickComputeType = (wrapper, computeType) =>
    pickDropdownOption(wrapper, '#runtime-compute', computeType);

  const getWorkerCpu = (wrapper) => getInputValue(wrapper, '#worker-cpu');
  const pickWorkerCpu = (wrapper, cpu) =>
    pickDropdownOption(wrapper, '#worker-cpu', cpu);

  const getWorkerRam = (wrapper) => getInputValue(wrapper, '#worker-ram');
  const pickWorkerRam = (wrapper, ram) =>
    pickDropdownOption(wrapper, '#worker-ram', ram);

  const getWorkerDiskSize = (wrapper) => getInputValue(wrapper, '#worker-disk');
  const pickWorkerDiskSize = (wrapper, diskSize) =>
    enterNumberInput(wrapper, '#worker-disk', diskSize);

  const getNumWorkers = (wrapper) => getInputValue(wrapper, '#num-workers');
  const pickNumWorkers = (wrapper, n) =>
    enterNumberInput(wrapper, '#num-workers', n);

  const getNumPreemptibleWorkers = (wrapper) =>
    getInputValue(wrapper, '#num-preemptible');
  const pickNumPreemptibleWorkers = (wrapper, n) =>
    enterNumberInput(wrapper, '#num-preemptible', n);

  const pickPreset = (wrapper, { displayName }) =>
    pickDropdownOption(wrapper, '#runtime-presets-menu', displayName);

  const mustClickButton = async (wrapper, label) => {
    const btn = wrapper.find(Button).find({ 'aria-label': label }).first();
    expect(btn.exists()).toBeTruthy();
    expect(btn.prop('disabled')).toBeFalsy();

    btn.simulate('click');
    await waitOneTickAndUpdate(wrapper);
  };

  const clickButtonIfVisible = async (wrapper, label) => {
    const btn = wrapper.find(Button).find({ 'aria-label': label }).first();
    if (!btn.exists()) {
      return;
    }
    expect(btn.prop('disabled')).toBeFalsy();

    btn.simulate('click');
    await waitOneTickAndUpdate(wrapper);
  };

  it('should show loading spinner while loading', async () => {
    const wrapper = await component();

    // Check before ticking - stub returns the runtime asynchronously.
    expect(!wrapper.exists('#runtime-panel'));
    expect(wrapper.exists(Spinner));

    // Now getRuntime returns.
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('#runtime-panel'));
    expect(!wrapper.exists(Spinner));
  });

  it('should show Create panel when no runtime', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    // TODO(RW-7152): This test is incorrectly depending on "default" values in runtime-panel, and
    // not general analysis. Ensure this test passes for the right reasons when fixing.
    const computeDefaults = wrapper.find('#compute-resources').first();
    expect(computeDefaults.text()).toEqual(
      '- Compute size of 4 CPUs, 15 GB memory, and a 120 GB disk'
    );
  });

  it('should allow creation when no runtime exists with defaults', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual(
      'n1-standard-4'
    );
  });

  it('should show customize after create', async () => {
    setCurrentRuntime(null);

    const wrapperBefore = await component();

    await mustClickButton(wrapperBefore, 'Create');

    const wrapperAfter = await component();
    expect(wrapperAfter.find('#runtime-cpu').exists()).toBeTruthy();
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is GeneralAnalysis', async () => {
    // In the case where the user's latest runtime is a preset (GeneralAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: 1000,
      },
      dataprocConfig: null,
    });

    const wrapper = await component();
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expectEqualFields(
      runtimeApiStub.runtime.gceConfig,
      runtimePresets.generalAnalysis.runtimeTemplate.gceConfig,
      ['machineType', 'diskSize']
    );
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is HailGenomicsAnalysis', async () => {
    // In the case where the user's latest runtime is a preset (HailGenomicsAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime

    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      gceConfig: null,
      dataprocConfig: {
        ...defaultDataprocConfig(),
        masterMachineType: 'n1-standard-16',
        masterDiskSize: 999,
        workerDiskSize: 444,
        numberOfWorkers: 5,
      },
    });

    const wrapper = await component();
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expectEqualFields(
      runtimeApiStub.runtime.dataprocConfig,
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig,
      [
        'masterMachineType',
        'masterDiskSize',
        'workerDiskSize',
        'numberOfWorkers',
      ]
    );
  });

  it('should allow creation when runtime has error status', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Error,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Try Again');

    // Kicks off a deletion to first clear the error status runtime.
    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should allow creation with update from error', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Error,
      errors: [{ errorMessage: "I'm sorry Dave, I'm afraid I can't do that" }],
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: MIN_DISK_SIZE_GB,
      },
      dataprocConfig: null,
    });

    const wrapper = await component();

    await pickMainCpu(wrapper, 8);
    await mustClickButton(wrapper, 'Try Again');

    // Kicks off a deletion to first clear the error status runtime.
    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should disable controls when runtime has a non-actionable status', async () => {
    runtimeApiStub.runtime.status = RuntimeStatus.Stopping;
    runtimeStoreStub.runtime = runtimeApiStub.runtime;

    // sanity check
    expect(isActionable(runtimeApiStub.runtime.status)).toBeFalsy();

    const wrapper = await component();

    expect(
      wrapper.find('#runtime-presets-menu').first().prop('disabled')
    ).toBeTruthy();
    expect(wrapper.find('#runtime-cpu').first().prop('disabled')).toBeTruthy();
    expect(
      wrapper
        .find(Button)
        .find({ 'aria-label': 'Next' })
        .first()
        .prop('disabled')
    ).toBeTruthy();
  });

  it('should allow creation with GCE config', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 52);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.UserOverride
    );
    expect(runtimeApiStub.runtime.gceConfig).toEqual({
      machineType: 'n1-highmem-8',
      diskSize: MIN_DISK_SIZE_GB + 10,
      gpuConfig: null,
    });
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // master settings
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB + 10);

    // worker settings
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.UserOverride
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

  it('should allow configuration via GCE preset', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Ensure set the form to something non-standard to start
    await pickMainCpu(wrapper, 8);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickPreset(wrapper, runtimePresets.generalAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.GeneralAnalysis
    );
    expect(runtimeApiStub.runtime.gceConfig).toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceConfig
    );
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.HailGenomicAnalysis
    );
    expect(runtimeApiStub.runtime.dataprocConfig).toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it(
    'should set runtime preset values in customize panel instead of getRuntime values ' +
      'if configurationType is GeneralAnalysis',
    async () => {
      setCurrentRuntime({
        ...runtimeApiStub.runtime,
        status: RuntimeStatus.Deleted,
        configurationType: RuntimeConfigurationType.GeneralAnalysis,
        gceConfig: {
          ...defaultGceConfig(),
          machineType: 'n1-standard-16',
          diskSize: 1000,
        },
        dataprocConfig: null,
      });

      const wrapper = await component();
      await mustClickButton(wrapper, 'Customize');

      expect(getMainCpu(wrapper)).toEqual(
        findMachineByName(
          runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.machineType
        ).cpu
      );
      expect(getMainRam(wrapper)).toEqual(
        findMachineByName(
          runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.machineType
        ).memory
      );
      expect(getMainDiskSize(wrapper)).toEqual(
        runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.diskSize
      );
    }
  );

  it(
    'should set runtime preset values in customize panel instead of getRuntime values ' +
      'if configurationType is HailGenomicsAnalysis',
    async () => {
      setCurrentRuntime({
        ...runtimeApiStub.runtime,
        status: RuntimeStatus.Deleted,
        configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
        gceConfig: null,
        dataprocConfig: {
          ...defaultDataprocConfig(),
          masterMachineType: 'n1-standard-16',
          masterDiskSize: 999,
          workerDiskSize: 444,
          numberOfWorkers: 5,
        },
      });

      const wrapper = await component();
      await mustClickButton(wrapper, 'Customize');

      expect(getMainCpu(wrapper)).toEqual(
        findMachineByName(
          runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
            .masterMachineType
        ).cpu
      );
      expect(getMainRam(wrapper)).toEqual(
        findMachineByName(
          runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
            .masterMachineType
        ).memory
      );
      expect(getMainDiskSize(wrapper)).toEqual(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
          .masterDiskSize
      );
      expect(getWorkerDiskSize(wrapper)).toEqual(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
          .workerDiskSize
      );
      expect(getNumWorkers(wrapper)).toEqual(
        runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
          .numberOfWorkers
      );
    }
  );

  it('should reattach to an existing disk by default, for deleted VMs', async () => {
    setCurrentDisk(existingDisk());
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
      configurationType: RuntimeConfigurationType.UserOverride,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
      },
      dataprocConfig: null,
    });

    const wrapper = await component();

    const getDetachableRadio = () =>
      wrapper.find({ name: 'detachableDisk' }).first();
    expect(getDetachableRadio().prop('checked')).toBeTruthy();
  });

  it('should allow configuration via dataproc preset from modified form', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.HailGenomicAnalysis
    );
    expect(runtimeApiStub.runtime.dataprocConfig).toEqual(
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig
    );
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should tag as user override after preset modification', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Take the preset but make a solitary modification.
    await pickPreset(wrapper, runtimePresets.hailAnalysis);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.UserOverride
    );
  });

  it('should tag as preset if configuration matches', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');

    // Take the preset, make a change, then revert.
    await pickPreset(wrapper, runtimePresets.generalAnalysis);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 2);
    await pickComputeType(wrapper, ComputeType.Standard);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.GeneralAnalysis
    );
  });

  it('should restrict memory options by cpu', async () => {
    const wrapper = await component();

    wrapper.find('div[id="runtime-cpu"]').first().simulate('click');
    wrapper
      .find('.p-dropdown-item')
      .find({ 'aria-label': 8 })
      .first()
      .simulate('click');

    wrapper.find('div[id="runtime-ram"]').simulate('click');

    const memoryOptions = wrapper
      .find('div[id="runtime-ram"]')
      .find('.p-dropdown-items')
      .find('.p-dropdown-item');
    expect(memoryOptions.exists()).toBeTruthy();

    // See app/utils/machines.ts, these are the valid memory options for an 8
    // CPU machine in GCE.
    expect(memoryOptions.map((m) => m.text())).toEqual(['7.2', '30', '52']);
  });

  it('should disable the Next button if there are no changes and runtime is running', async () => {
    const wrapper = await component();

    expect(
      wrapper
        .find(Button)
        .find({ 'aria-label': 'Next' })
        .first()
        .prop('disabled')
    ).toBeTruthy();
  });

  it('should respect divergent sets of valid machine types across compute types', async () => {
    const wrapper = await component();

    await pickMainCpu(wrapper, 1);
    await pickMainRam(wrapper, 3.75);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // n1-standard-1 is illegal for Dataproc, so it should restore the default.
    expect(getMainCpu(wrapper)).toBe(4);
    expect(getMainRam(wrapper)).toBe(15);
  });

  it('should carry over valid main machine type across compute types', async () => {
    const wrapper = await component();

    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // n1-standard-2 is legal for Dataproc, so it should remain.
    expect(getMainCpu(wrapper)).toBe(2);
    expect(getMainRam(wrapper)).toBe(7.5);
  });

  it('should warn user about reboot if there are updates that require one - increase disk size', async () => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should not warn user for updates where not needed - number of workers', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      configurationType: RuntimeConfigurationType.UserOverride,
    });

    const wrapper = await component();

    await pickNumWorkers(wrapper, getNumWorkers(wrapper) + 2);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).exists()).toBeFalsy();
  });

  it('should not warn user for updates where not needed - number of preemptibles', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();

    await pickNumPreemptibleWorkers(
      wrapper,
      getNumPreemptibleWorkers(wrapper) + 2
    );
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).exists()).toBeFalsy();
  });

  it('should warn user about reboot if there are updates that require one - CPU', async () => {
    const wrapper = await component();

    await pickMainCpu(wrapper, getMainCpu(wrapper) + 4);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - Memory', async () => {
    const wrapper = await component();

    // 15 GB -> 26 GB
    await pickMainRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async () => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await mustClickButton(wrapper, 'Next');

    expect(
      wrapper.find(WarningMessage).text().includes('deletion')
    ).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async () => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) - 10);
    await mustClickButton(wrapper, 'Next');

    expect(
      wrapper.find(WarningMessage).text().includes('deletion')
    ).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();

    // 4 -> 8
    await pickWorkerCpu(wrapper, 8);
    await mustClickButton(wrapper, 'Next');

    expect(
      wrapper.find(WarningMessage).text().includes('deletion')
    ).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker RAM', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();

    // 15 -> 26
    await pickWorkerRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(
      wrapper.find(WarningMessage).text().includes('deletion')
    ).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker Disk', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();
    await pickWorkerDiskSize(wrapper, getWorkerDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(
      wrapper.find(WarningMessage).text().includes('deletion')
    ).toBeTruthy();
  });

  it('should retain original inputs when hitting cancel from the Confirm panel', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();

    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB + 10);
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickWorkerCpu(wrapper, 16);
    await pickWorkerRam(wrapper, 60);
    await pickNumPreemptibleWorkers(wrapper, 3);
    await pickNumWorkers(wrapper, 5);
    await pickWorkerDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Cancel');

    expect(getMainDiskSize(wrapper)).toBe(DATAPROC_MIN_DISK_SIZE_GB + 10);
    expect(getMainCpu(wrapper)).toBe(8);
    expect(getMainRam(wrapper)).toBe(30);
    expect(getWorkerCpu(wrapper)).toBe(16);
    expect(getWorkerRam(wrapper)).toBe(60);
    expect(getNumPreemptibleWorkers(wrapper)).toBe(3);
    expect(getNumWorkers(wrapper)).toBe(5);
    expect(getWorkerDiskSize(wrapper)).toBe(DATAPROC_MIN_DISK_SIZE_GB);
  });

  it('should disable Next button if Runtime is in between states', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
      status: RuntimeStatus.Creating,
    });

    const wrapper = await component();
    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    expect(
      wrapper
        .find(Button)
        .find({ 'aria-label': 'Next' })
        .first()
        .prop('disabled')
    ).toBeTruthy();
  });

  it('should send an updateRuntime API call if runtime changes do not require a delete', async () => {
    const wrapper = await component();

    const updateSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    const deleteSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(updateSpy).toHaveBeenCalled();
    expect(deleteSpy).toHaveBeenCalledTimes(0);
  });

  it('should send a delete call if an update requires delete', async () => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should show create button if runtime is deleted', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
    });

    const wrapper = await component();

    expect(
      wrapper.find(Button).find({ 'aria-label': 'Create' }).first().exists()
    ).toBeTruthy();
  });

  it('should add additional options when the compute type changes', async () => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    expect(wrapper.exists('span[id="num-workers"]')).toBeTruthy();
    expect(wrapper.exists('span[id="num-preemptible"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-cpu"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-ram"]')).toBeTruthy();
    expect(wrapper.exists('span[id="worker-disk"]')).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async () => {
    const wrapper = await component();

    expect(costEstimator(wrapper).exists()).toBeTruthy();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    expect(runningCost(wrapper).text()).toEqual('$0.20 per hour');
    expect(storageCost(wrapper).text()).toEqual('< $0.01 per hour');

    // Change the machine to n1-standard-8 and bump the storage to 300GB.
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickMainDiskSize(wrapper, 300);
    expect(runningCost(wrapper).text()).toEqual('$0.40 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.02 per hour');

    await pickPreset(wrapper, { displayName: 'General Analysis' });
    expect(runningCost(wrapper).text()).toEqual('$0.20 per hour');
    expect(storageCost(wrapper).text()).toEqual('< $0.01 per hour');

    await pickComputeType(wrapper, ComputeType.Dataproc);
    expect(runningCost(wrapper).text()).toEqual('$0.73 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.02 per hour');

    // Bump up all the worker values to increase the price on everything.
    await pickNumWorkers(wrapper, 4);
    await pickNumPreemptibleWorkers(wrapper, 4);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    expect(runningCost(wrapper).text()).toEqual('$2.88 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.14 per hour');
  });

  it('should update the cost estimator when master machine changes', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Running,
      configurationType: RuntimeConfigurationType.UserOverride,
      gceConfig: null,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 1000,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
      },
    });

    const wrapper = await component();

    // with Master disk size: 1000
    expect(costEstimator(wrapper).exists()).toBeTruthy();

    expect(runningCost(wrapper).text()).toEqual('$0.77 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.07 per hour');

    // Change the Master disk size or master size to 150
    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);

    expect(costEstimator(wrapper).exists()).toBeTruthy();

    expect(runningCost(wrapper).text()).toEqual('$0.73 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.02 per hour');
    // Switch to n1-highmem-4, double disk size.
    await pickMainRam(wrapper, 26);
    await pickMainDiskSize(wrapper, 2000);
    expect(runningCost(wrapper).text()).toEqual('$0.87 per hour');
    expect(storageCost(wrapper).text()).toEqual('$0.13 per hour');
  });

  it('should allow runtime deletion', async () => {
    const wrapper = await component();

    wrapper
      .find(LinkButton)
      .find({ 'aria-label': 'Delete Environment' })
      .first()
      .simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    await mustClickButton(wrapper, 'Delete');

    // Runtime should be deleting, and panel should have closed.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Deleting);
    expect(onClose).toHaveBeenCalled();
  });

  it('should allow cancelling runtime deletion', async () => {
    const wrapper = await component();

    wrapper
      .find(LinkButton)
      .find({ 'aria-label': 'Delete Environment' })
      .first()
      .simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    // Click cancel
    await mustClickButton(wrapper, 'Cancel');

    // Runtime should still be active, and confirm page should no longer be visible.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Running);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('should display the Running runtime status icon in state Running', async () => {
    const wrapper = await component();

    expect(
      wrapper.find('[data-test-id="runtime-status-icon-running"]').exists()
    ).toBeTruthy();
  });

  it('should display a compute-none when there is no runtime', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    expect(
      wrapper.find('[data-test-id="runtime-status-icon-none"]').exists()
    ).toBeTruthy();
  });

  it('should prevent runtime creation when disk size is invalid', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () =>
      wrapper.find({ 'aria-label': 'Create' }).first();

    await pickMainDiskSize(wrapper, 49);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, 4900);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerDiskSize(wrapper, 49);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickWorkerDiskSize(wrapper, 4900);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime update when disk size is invalid', async () => {
    const wrapper = await component();
    const getNextButton = () => wrapper.find({ 'aria-label': 'Next' }).first();

    await pickMainDiskSize(wrapper, 49);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, 4900);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerDiskSize(wrapper, 49);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickWorkerDiskSize(wrapper, 4900);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    expect(getNextButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime update when PD disk size is invalid', async () => {
    const wrapper = await component();
    const getNextButton = () => wrapper.find({ 'aria-label': 'Next' }).first();

    await enableDetachable(wrapper);
    await pickDetachableType(wrapper, DiskType.Standard);

    await pickDetachableDiskSize(wrapper, 49);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickDetachableType(wrapper, DiskType.Ssd);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickDetachableDiskSize(wrapper, 4900);
    expect(getNextButton().prop('disabled')).toBeTruthy();

    await pickDetachableType(wrapper, DiskType.Standard);
    expect(getNextButton().prop('disabled')).toBeTruthy();
  });

  it('should prevent detachable PD use for Dataproc', async () => {
    setCurrentRuntime(detachableDiskRuntime());

    const wrapper = await component();
    const getDetachableRadio = () =>
      wrapper.find({ name: 'detachableDisk' }).first();

    expect(getDetachableRadio().prop('disabled')).toBeFalsy();
    await enableDetachable(wrapper);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    expect(getDetachableRadio().prop('disabled')).toBeTruthy();
  });

  const pickSsdType = async (wrapper) =>
    pickDetachableType(wrapper, DiskType.Ssd);
  const decrementDetachableDiskSize = async (wrapper) => {
    const prevSize = await getDetachableDiskSize(wrapper);
    await pickDetachableDiskSize(wrapper, prevSize - 1);
  };
  const incrementDetachableDiskSize = async (wrapper) => {
    const prevSize = await getDetachableDiskSize(wrapper);
    await pickDetachableDiskSize(wrapper, prevSize + 1);
  };
  const changeMainCpu = async (wrapper) => pickMainCpu(wrapper, 8);

  type DetachableDiskCase = [
    string,
    ((w: ReactWrapper) => Promise<void>)[],
    {
      wantUpdateDisk?: boolean;
      wantDeleteDisk?: boolean;
      wantUpdateRuntime?: boolean;
      wantDeleteRuntime?: boolean;
    }
  ];

  async function runDetachableDiskCase(
    wrapper: ReactWrapper,
    [
      _,
      setters,
      {
        wantUpdateDisk = false,
        wantDeleteDisk = false,
        wantUpdateRuntime = false,
        wantDeleteRuntime = false,
      },
    ]: DetachableDiskCase,
    existingDiskName: string
  ) {
    const updateDiskSpy = jest.spyOn(disksApi(), 'updateDisk');
    const deleteDiskSpy = jest.spyOn(disksApi(), 'deleteDisk');
    const createRuntimeSpy = jest.spyOn(runtimeApi(), 'createRuntime');
    const updateRuntimeSpy = jest.spyOn(runtimeApi(), 'updateRuntime');

    for (const f of setters) {
      await f(wrapper);
    }

    await waitOneTickAndUpdate(wrapper);
    await clickButtonIfVisible(wrapper, 'Next');
    await clickButtonIfVisible(wrapper, 'Update');
    await clickButtonIfVisible(wrapper, 'Create');

    if (
      wrapper.find({ 'data-test-id': 'delete-unattached-pd-radio' }).exists()
    ) {
      await clickDeletePdRadioButton(wrapper);
      await clickButtonIfVisible(wrapper, 'Delete');
    }

    expect(updateDiskSpy).toHaveBeenCalledTimes(wantUpdateDisk ? 1 : 0);
    expect(deleteDiskSpy).toHaveBeenCalledTimes(wantDeleteDisk ? 1 : 0);

    expect(updateRuntimeSpy).toHaveBeenCalledTimes(wantUpdateRuntime ? 1 : 0);
    if (wantDeleteRuntime) {
      expect(runtimeApiStub.runtime.status).toEqual('Deleting');

      runtimeApiStub.runtime.status = RuntimeStatus.Deleted;

      // Dropdown adds a hacky setTimeout(.., 1), which causes exceptions here, hence the retries.
      await waitForFakeTimersAndUpdate(wrapper, /* maxRetries*/ 10);
      expect(createRuntimeSpy).toHaveBeenCalledTimes(1);
    }

    if (wantDeleteDisk) {
      expect(disksApiStub.disk.name).not.toEqual(existingDiskName);
    } else {
      expect(disksApiStub.disk.name).toEqual(existingDiskName);
    }
  }

  test.each([
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
    ['disk increase', [incrementDetachableDiskSize], { wantUpdateDisk: true }],
    ['in-place', [changeMainCpu], { wantUpdateRuntime: true }],
    [
      'in-place + disk type',
      [changeMainCpu, pickSsdType],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    [
      'in-place + disk decrease',
      [changeMainCpu, decrementDetachableDiskSize],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    [
      'in-place + disk increase',
      [changeMainCpu, incrementDetachableDiskSize],
      { wantUpdateDisk: true, wantUpdateRuntime: true },
    ],
    ['recreate', [clickEnableGpu], { wantDeleteRuntime: true }],
    [
      'recreate + disk type',
      [clickEnableGpu, pickSsdType],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    [
      'recreate + disk decrease',
      [clickEnableGpu, decrementDetachableDiskSize],
      { wantDeleteDisk: true, wantDeleteRuntime: true },
    ],
    [
      'recreate + disk increase',
      [clickEnableGpu, incrementDetachableDiskSize],
      { wantUpdateDisk: true, wantDeleteRuntime: true },
    ],
  ] as DetachableDiskCase[])(
    'should allow runtime updates to attached PD: %s',
    async (desc: string, setters, expectations) => {
      setCurrentRuntime(detachableDiskRuntime());

      const disk = existingDisk();
      setCurrentDisk(disk);

      const wrapper = await component();
      await runDetachableDiskCase(
        wrapper,
        [desc, setters, expectations],
        disk.name
      );
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

      const wrapper = await component();
      await mustClickButton(wrapper, 'Customize');
      await runDetachableDiskCase(
        wrapper,
        [desc, setters, expectations],
        disk.name
      );
    }
  );

  it('should allow Dataproc -> PD transition', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Running,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();
    pickComputeType(wrapper, ComputeType.Standard);

    enableDetachable(wrapper);
    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    runtimeApiStub.runtime.status = RuntimeStatus.Deleted;

    await waitForFakeTimersAndUpdate(wrapper, /* maxRetries*/ 10);
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Creating);
    expect(disksApiStub.disk).toBeTruthy();
  });

  it('should allow disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    setCurrentDisk(existingDisk());

    const wrapper = await component();
    pickComputeType(wrapper, ComputeType.Dataproc);

    await mustClickButton(wrapper, 'Next');
    expect(wrapper.text()).toContain(
      'will be unused after you apply this update'
    );

    // Click the "delete" radio button.
    wrapper
      .find({ 'data-test-id': 'delete-pd' })
      .find(RadioButton)
      .first()
      .simulate('change');

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    runtimeApiStub.runtime.status = RuntimeStatus.Deleted;
    await waitForFakeTimersAndUpdate(wrapper, /* maxRetries*/ 10);

    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Creating);
    expect(disksApiStub.disk).toBeNull();
  });

  it('should allow skipping disk deletion when detaching', async () => {
    setCurrentRuntime(detachableDiskRuntime());
    const disk = existingDisk();
    setCurrentDisk(disk);

    const wrapper = await component();
    enableDetachable(wrapper, false);

    await mustClickButton(wrapper, 'Next');

    expect(wrapper.text()).toContain(
      'will be unused after you apply this update'
    );

    // Default option should be NOT to delete.
    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    runtimeApiStub.runtime.status = RuntimeStatus.Deleted;
    await waitForFakeTimersAndUpdate(wrapper, /* maxRetries*/ 10);

    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Creating);
    expect(disksApiStub.disk?.name).toEqual(disk.name);
  });

  it('should prevent runtime creation when running cost is too high for free tier', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () =>
      wrapper.find({ 'aria-label': 'Create' }).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickMainDiskSize(wrapper, 150);
    // This should make the cost about $50 per hour.
    await pickNumWorkers(wrapper, 200);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime creation when worker count is invalid', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () =>
      wrapper.find({ 'aria-label': 'Create' }).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);

    await pickNumWorkers(wrapper, 0);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 1);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should allow runtime creation when running cost is too high for user provided billing', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.OWNER,
      billingAccountName: 'user provided billing',
    });

    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () =>
      wrapper.find({ 'aria-label': 'Create' }).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    // This should make the cost about $50 per hour.
    await pickNumWorkers(wrapper, 20000);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime creation when running cost is too high for paid tier', async () => {
    setCurrentRuntime(null);
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () =>
      wrapper.find({ 'aria-label': 'Create' }).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickMainDiskSize(wrapper, DATAPROC_MIN_DISK_SIZE_GB);
    // This should make the cost about $140 per hour.
    await pickNumWorkers(wrapper, 600);
    expect(getCreateButton().prop('disabled')).toBeFalsy();

    // This should make the cost around $160 per hour.
    await pickNumWorkers(wrapper, 700);
    // We don't want to disable for user provided billing. Just put a warning.
    expect(getCreateButton().prop('disabled')).toBeFalsy();
    expect(
      wrapper.find('[data-test-id="runtime-warning-messages"]').exists()
    ).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="runtime-error-messages"]').exists()
    ).toBeFalsy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should render disabled panel when creator billing disabled', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingStatus: BillingStatus.INACTIVE,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    });
    const wrapper = await component();

    const disabledPanel = wrapper.find({
      'data-test-id': 'runtime-disabled-panel',
    });
    expect(disabledPanel.exists()).toBeTruthy();
    const createPanel = wrapper.find({
      'data-test-id': 'runtime-create-panel',
    });
    expect(createPanel.exists()).toBeFalsy();
  });

  it('should allow creating gce with GPU', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    await clickEnableGpu(wrapper);
    await pickGpuType(wrapper, 'NVIDIA Tesla T4');
    await pickGpuNum(wrapper, 2);
    await pickMainCpu(wrapper, 8);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);

    await mustClickButton(wrapper, 'Create');
    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.gpuConfig.gpuType).toEqual(
      'nvidia-tesla-t4'
    );
    expect(runtimeApiStub.runtime.gceConfig.gpuConfig.numOfGpus).toEqual(2);
  });

  it('should allow creating gce without GPU', async () => {
    if (!enableGpu) {
      return;
    }
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    await pickMainCpu(wrapper, 8);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await mustClickButton(wrapper, 'Create');
    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.gpuConfig).toEqual(null);
  });

  it('should disable worker count updates for stopped dataproc cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Stopped,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig(),
    });

    const wrapper = await component();

    const workerCountInput = wrapper.find('#num-workers').first();
    expect(workerCountInput.prop('disabled')).toBeTruthy();
    expect(workerCountInput.prop('tooltip')).toBeTruthy();

    const preemptibleCountInput = wrapper.find('#num-preemptible').first();
    expect(preemptibleCountInput.prop('disabled')).toBeTruthy();
    expect(preemptibleCountInput.prop('tooltip')).toBeTruthy();
  });

  it('should allow worker configuration for stopped GCE runtime', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Stopped,
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: defaultGceConfig(),
      dataprocConfig: null,
    });

    const wrapper = await component();
    await pickComputeType(wrapper, ComputeType.Dataproc);

    const workerCountInput = wrapper.find('#num-workers').first();
    expect(workerCountInput.prop('disabled')).toBeFalsy();
    expect(workerCountInput.prop('tooltip')).toBeFalsy();

    const preemptibleCountInput = wrapper.find('#num-preemptible').first();
    expect(preemptibleCountInput.prop('disabled')).toBeFalsy();
    expect(preemptibleCountInput.prop('tooltip')).toBeFalsy();
  });

  it('should disable Spark console for non-running cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Stopped,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
    });

    const wrapper = await component();
    const manageButton = wrapper.find('[data-test-id="manage-spark-console"]');
    expect(manageButton.exists()).toBeTruthy();
    expect(manageButton.first().prop('disabled')).toBeTruthy();
  });

  it('should render Spark console links for running cluster', async () => {
    setCurrentRuntime({
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Running,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      dataprocConfig: defaultDataprocConfig(),
      gceConfig: null,
    });

    const wrapper = await component();
    const manageButton = wrapper.find('[data-test-id="manage-spark-console"]');
    expect(manageButton.exists()).toBeTruthy();
    manageButton.first().simulate('click');

    wrapper.update();
    expect(wrapper.text()).toContain('MapReduce History Server');
  });
});
