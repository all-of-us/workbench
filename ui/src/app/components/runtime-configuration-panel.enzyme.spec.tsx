import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';

import {
  DisksApi,
  RuntimeConfigurationType,
  RuntimeStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';
import { Disk, DiskType, Runtime, RuntimeApi } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import {
  RuntimeConfigurationPanel,
  RuntimeConfigurationPanelProps,
} from 'app/components/runtime-configuration-panel';
import {
  disksApi,
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';
import { ComputeType, MIN_DISK_SIZE_GB } from 'app/utils/machines';
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
  mountWithRouter,
  waitForFakeTimersAndUpdate,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

describe(RuntimeConfigurationPanel.name, () => {
  const defaultProps: RuntimeConfigurationPanelProps = {
    onClose: jest.fn(),
    profileState: {
      profile: ProfileStubVariables.PROFILE_STUB,
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    },
  };
  let runtimeApiStub: RuntimeApiStub;
  let disksApiStub: DisksApiStub;
  let workspacesApiStub: WorkspacesApiStub;
  let freeTierBillingAccountId: string;

  const component = async (
    propOverrides?: Partial<RuntimeConfigurationPanelProps>
  ) => {
    const allProps = { ...defaultProps, ...propOverrides };
    const c = mountWithRouter(<RuntimeConfigurationPanel {...allProps} />);
    await waitOneTickAndUpdate(c);
    return c;
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

  let runtimeStoreStub;
  let runtimeDiskStoreStub;

  const setCurrentDisk = (d: Disk) => {
    disksApiStub.disk = d;
    runtimeDiskStoreStub.gcePersistentDisk = d;
  };

  const setCurrentRuntime = (r: Runtime) => {
    runtimeApiStub.runtime = r;
    runtimeStoreStub.runtime = r;
  };

  beforeEach(async () => {
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    freeTierBillingAccountId =
      serverConfigStore.get().config.freeTierBillingAccountId;

    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

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

    runtimeDiskStoreStub = {
      workspaceNamespace: workspaceStubs[0].namespace,
      gcePersistentDisk: null,
    };
    runtimeDiskStore.set(runtimeDiskStoreStub);

    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.clearAllTimers();
    jest.useRealTimers();
  });

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

  const pickMainCpu = (wrapper, cpu) =>
    pickDropdownOption(wrapper, '#runtime-cpu', cpu);

  const pickMainRam = (wrapper, ram) =>
    pickDropdownOption(wrapper, '#runtime-ram', ram);

  const pickMainDiskSize = (wrapper, diskSize) =>
    enterNumberInput(wrapper, '#standard-disk', diskSize);

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

  const pickWorkerCpu = (wrapper, cpu) =>
    pickDropdownOption(wrapper, '#worker-cpu', cpu);

  const pickWorkerRam = (wrapper, ram) =>
    pickDropdownOption(wrapper, '#worker-ram', ram);

  const pickWorkerDiskSize = (wrapper, diskSize) =>
    enterNumberInput(wrapper, '#worker-disk', diskSize);

  const pickNumWorkers = (wrapper, n) =>
    enterNumberInput(wrapper, '#num-workers', n);

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

  it('should allow configuration via GCE preset', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Ensure set the form to something non-standard to start
    await pickMainCpu(wrapper, 8);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);

    await pickPreset(wrapper, runtimePresets.generalAnalysis);

    await mustClickButton(wrapper, 'Create');

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

  it('should allow configuration via dataproc preset', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS
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
      // Moved to RTL
    }
  );

  it(
    'should set runtime preset values in customize panel instead of getRuntime values ' +
      'if configurationType is HailGenomicsAnalysis',
    async () => {
      // Moved to rtl
    }
  );

  it('should reattach to an existing disk by default, for deleted VMs', async () => {
    // Moved to RTL
  });

  it('should allow configuration via dataproc preset from modified form', async () => {
    setCurrentRuntime(null);

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    await pickDetachableDiskSize(wrapper, MIN_DISK_SIZE_GB);
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
      RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS
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
      RuntimeConfigurationType.USER_OVERRIDE
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
    await pickDetachableDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType).toEqual(
      RuntimeConfigurationType.GENERAL_ANALYSIS
    );
  });

  it('should restrict memory options by cpu', async () => {
    // Moved to rtl
  });

  it('should disable the Next button if there are no changes and runtime is running', async () => {
    // migrated to RTL
  });

  it('should respect divergent sets of valid machine types across compute types', async () => {
    // Moved to RTL
  });

  it('should carry over valid main machine type across compute types', async () => {
    // Moved to RTL
  });

  it('should warn user about re-creation if there are updates that require one - increase disk size', async () => {
    // Moved to RTL
  });

  it('should warn user about re-boot if there are updates that require one - increase disk size', async () => {
    // Moved to RTL
  });

  it('should not warn user for updates where not needed - number of workers', async () => {
    // Moved to RTL
  });

  it('should not warn user for updates where not needed - number of preemptibles', async () => {
    // Moved to RTL
  });

  it('should warn user about reboot if there are updates that require one - CPU', async () => {
    // Moved to RTL
  });

  it('should warn user about reboot if there are updates that require one - Memory', async () => {
    // Moved to RTL
  });

  it('should warn user about re-creation if there are updates that require one - CPU', async () => {
    // Moved to RTL
  });

  it('should warn user about re-creation if there are updates that require one - Memory', async () => {
    // Moved to RTL
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async () => {
    // Moved to RTL
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async () => {
    // Moved to RTL
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async () => {
    // Moved to RTL
  });

  it('should warn the user about deletion if there are updates that require one - Worker RAM', async () => {
    // Moved to RTL
  });

  it('should warn the user about deletion if there are updates that require one - Worker Disk', async () => {
    // moved to RTL
  });

  it('should retain original inputs when hitting cancel from the Confirm panel', async () => {
    // Moved to RTL
  });

  it('should disable Next button if Runtime is in between states', async () => {
    // Moved to RTL
  });

  it('should send an updateRuntime API call if runtime changes do not require a delete', async () => {
    // Moved to RTL
  });

  it('should send an updateDisk API call if disk changes do not require a delete', async () => {
    // Moved to RTL
  });

  it('should send a delete call if an update requires delete', async () => {
    // Moved to RTL
  });

  it('should show create button if runtime is deleted', async () => {
    // migrated to RTL
  });

  it('should add additional options when the compute type changes', async () => {
    // migrated to RTL
  });

  it('should update the cost estimator when the compute profile changes', async () => {
    // migrated to RTL
  });

  it('should update the cost estimator when master machine changes', async () => {
    // migrated to RT
  });

  it('should allow runtime deletion', async () => {
    // migrated to RTL
  });

  it('should allow cancelling runtime deletion', async () => {
    // migrated to RTL
  });

  it('should prevent runtime creation when disk size is invalid', async () => {
    // Moved to RTL
  });

  it('should prevent runtime update when disk size is invalid', async () => {
    // Moved to RTL
  });

  it('should prevent runtime update when PD disk size is invalid', async () => {
    // Moved to RTL
  });

  it('should prevent detachable PD use for Dataproc', async () => {
    // migrate to RTL
  });

  const pickSsdType = async (wrapper) =>
    pickDetachableType(wrapper, DiskType.SSD);
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
    expect(updateRuntimeSpy).toHaveBeenCalledTimes(wantUpdateRuntime ? 1 : 0);
    expect(deleteDiskSpy).toHaveBeenCalledTimes(wantDeleteDisk ? 1 : 0);

    if (wantDeleteRuntime) {
      expect(runtimeApiStub.runtime.status).toEqual('Deleting');

      runtimeApiStub.runtime.status = RuntimeStatus.DELETED;

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
    [
      'disk increase',
      [incrementDetachableDiskSize],
      { wantUpdateRuntime: true },
    ],
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
      { wantUpdateRuntime: true },
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
      { wantUpdateRuntime: true },
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

  // test.each([
  //   ['disk type', [pickSsdType], { wantDeleteDisk: true }],
  //   ['disk decrease', [decrementDetachableDiskSize], { wantDeleteDisk: true }],
  //   ['disk increase', [incrementDetachableDiskSize], { wantUpdateDisk: true }],
  // ] as DetachableDiskCase[])(
  //   'should allow runtime creates with existing disk: %s',
  //   async (desc, setters, expectations) => {
  //     // Moved to RTL
  //   }
  // );

  it('should allow Dataproc -> PD transition', async () => {
    // migrated to RTL
  });

  it('should allow disk deletion when detaching', async () => {
    // Moved to RTL
  });

  it('should allow skipping disk deletion when detaching', async () => {
    // Moved to RTL
  });

  it('should prevent runtime creation when running cost is too high for free tier', async () => {
    // Moved to RTL
  });

  it('should prevent runtime creation when worker count is invalid', async () => {
    // Moved to RTL
  });

  it('should allow runtime creation when running cost is too high for user provided billing', async () => {
    // Moved to RTL
  });

  it('should prevent runtime creation when running cost is too high for paid tier', async () => {
    // Moved to RTL
  });

  it('should allow creating gcePD with GPU', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    await clickEnableGpu(wrapper);
    await pickGpuType(wrapper, 'NVIDIA Tesla T4');
    await pickGpuNum(wrapper, 2);
    await pickMainCpu(wrapper, 8);
    await pickDetachableDiskSize(wrapper, MIN_DISK_SIZE_GB);

    await mustClickButton(wrapper, 'Create');
    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig).toBeUndefined();
    expect(runtimeApiStub.runtime.gceWithPdConfig.persistentDisk.name).toEqual(
      'stub-disk'
    );
    expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig.numOfGpus).toEqual(
      2
    );
  });

  it('should allow creating gce without GPU', async () => {
    setCurrentRuntime(null);
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    await pickMainCpu(wrapper, 8);
    await pickDetachableDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await mustClickButton(wrapper, 'Create');
    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig).toEqual(null);
  });

  it('should disable worker count updates for stopped dataproc cluster', async () => {
    // Moved to RTL
  });

  it('should allow worker configuration for stopped GCE runtime', async () => {
    // migrated to RTL
  });

  it('should disable Spark console for non-running cluster', async () => {
    // migrated to RTL
  });

  it('should render Spark console links for running cluster', async () => {
    // migrated to RTL
  });

  it('Should disable standard storage option for existing GCE runtime and have reattachable selected', async () => {
    // migrated to RTL
  });
});
