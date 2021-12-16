import {mount} from 'enzyme';
import {act} from 'react-dom/test-utils';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {Button, LinkButton} from 'app/components/buttons';
import {Spinner} from 'app/components/spinners';
import {WarningMessage} from 'app/components/messages';
import {
  ConfirmDelete,
  DATAPROC_WORKER_MIN_DISK_SIZE_GB,
  MIN_DISK_SIZE_GB,
  Props,
  RuntimePanelWrapper
} from 'app/pages/analysis/runtime-panel';
import {profileApi, registerApiClient, runtimeApi} from 'app/services/swagger-fetch-clients';
import {findMachineByName, ComputeType} from 'app/utils/machines';
import {runtimePresets} from 'app/utils/runtime-presets';
import {
  ProfileApi,
  RuntimeConfigurationType,
  RuntimeStatus,
  WorkspaceAccessLevel,
  WorkspacesApi
} from 'generated/fetch';
import {RuntimeApi} from 'generated/fetch/api';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {cdrVersionTiersResponse, CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {
  defaultGceConfig,
  defaultDataprocConfig,
  RuntimeApiStub
} from 'testing/stubs/runtime-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {workspaceStubs} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {BillingStatus} from 'generated/fetch';
import {
  cdrVersionStore,
  clearCompoundRuntimeOperations,
  serverConfigStore,
  runtimeStore,
  profileStore
} from 'app/utils/stores';
import {currentWorkspaceStore} from 'app/utils/navigation';


describe('RuntimePanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;
  let workspacesApiStub: WorkspacesApiStub;
  let onClose: () => void;
  let enablePd: boolean;
  let enableGpu: boolean;
  let freeTierBillingAccountId: string;

  const component = async(propOverrides?: object) => {
    const allProps = {...props, ...propOverrides}
    const c = mount(<RuntimePanelWrapper {...allProps}/>);
    await waitOneTickAndUpdate(c);
    return c;
  };

  let runtimeStoreStub;

  beforeEach(async () => {
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({config: {...defaultServerConfig}});
    enablePd = serverConfigStore.get().config.enablePersistentDisk;
    enableGpu = serverConfigStore.get().config.enableGpu;
    freeTierBillingAccountId = serverConfigStore.get().config.freeTierBillingAccountId;

    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn()
    });

    onClose = jest.fn();
    props = {
      onClose
    };

    cdrVersionStore.set(cdrVersionTiersResponse);

    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingAccountName: 'billingAccounts/' + freeTierBillingAccountId,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
    });

    runtimeStoreStub = {runtime: runtimeApiStub.runtime, workspaceNamespace: workspaceStubs[0].namespace, runtimeLoaded: true};
    runtimeStore.set(runtimeStoreStub);

    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.useRealTimers();
  });

  const pickDropdownOption = async(wrapper, id, label) => {
    act(() => {
      wrapper.find(id).first().simulate('click');
    });
    const item = wrapper.find(`${id} .p-dropdown-item`).find({'aria-label': label}).first();
    expect(item.exists()).toBeTruthy();

    item.simulate('click');

    // In some cases, picking an option may require some waiting, e.g. for
    // rerendering of RAM options based on CPU selection.
    await waitOneTickAndUpdate(wrapper);
  };

  const getInputValue = (wrapper, id) => {
    return wrapper.find(id).first().prop('value');
  };

  const enterNumberInput = async(wrapper, id, value) => {
    // TODO: Find a way to invoke this without props.
    act(() => {wrapper.find(id).first().prop('onChange')({value} as any);});
    await waitOneTickAndUpdate(wrapper);
  };

  function getCheckbox(wrapper, id) {
    return wrapper.find(id).first().prop('checked');
  }

  function clickCheckbox(wrapper, id) {
    const currentChecked = getCheckbox(wrapper, id);
    wrapper.find(id).first().simulate('change', {target: {checked: !currentChecked}});
  }

  const getMainCpu = (wrapper) => getInputValue(wrapper, '#runtime-cpu');
  const pickMainCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#runtime-cpu', cpu);

  const getMainRam = (wrapper) => getInputValue(wrapper, '#runtime-ram');
  const pickMainRam = (wrapper, ram) => pickDropdownOption(wrapper, '#runtime-ram', ram);

  const getMainDiskSize = (wrapper) => getInputValue(wrapper, '#runtime-disk');
  const pickMainDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#runtime-disk', diskSize);

  const getPdSize = (wrapper) => getInputValue(wrapper, '#persistent-disk');
  const pickPdSize = (wrapper, pdSize) => enterNumberInput(wrapper, '#persistent-disk', pdSize);

  const getGpuType = (wrapper) => getInputValue(wrapper, '#gpu-type');
  const pickGpuType = (wrapper, gpuType) => pickDropdownOption(wrapper, '#gpu-type', gpuType);

  const getGpuNum = (wrapper) => getInputValue(wrapper, '#gpu-num');
  const pickGpuNum = (wrapper, gpuNum) => pickDropdownOption(wrapper, '#gpu-num', gpuNum);

  const getEnableGpu = (wrapper) => getCheckbox(wrapper, '#enable-gpu');
  const clickEnableGpu = (wrapper) => clickCheckbox(wrapper, '#enable-gpu');

  const pickComputeType = (wrapper, computeType) => pickDropdownOption(wrapper, '#runtime-compute', computeType);

  const getWorkerCpu = (wrapper) => getInputValue(wrapper, '#worker-cpu');
  const pickWorkerCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#worker-cpu', cpu);

  const getWorkerRam = (wrapper) => getInputValue(wrapper, '#worker-ram');
  const pickWorkerRam = (wrapper, ram) => pickDropdownOption(wrapper, '#worker-ram', ram);

  const getWorkerDiskSize = (wrapper) => getInputValue(wrapper, '#worker-disk');
  const pickWorkerDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#worker-disk', diskSize);

  const getNumWorkers = (wrapper) => getInputValue(wrapper, '#num-workers');
  const pickNumWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-workers', n);

  const getNumPreemptibleWorkers = (wrapper) => getInputValue(wrapper, '#num-preemptible');
  const pickNumPreemptibleWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-preemptible', n);

  const pickPreset = (wrapper, {displayName}) => pickDropdownOption(wrapper, '#runtime-presets-menu', displayName);

  const mustClickButton = async(wrapper, label) => {
    const createButton = wrapper.find(Button).find({'aria-label': label}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    createButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);
  };

  it('should show loading spinner while loading', async() => {
    const wrapper = await component();

    // Check before ticking - stub returns the runtime asynchronously.
    expect(!wrapper.exists('#runtime-panel'));
    expect(wrapper.exists(Spinner));

    // Now getRuntime returns.
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('#runtime-panel'));
    expect(!wrapper.exists(Spinner));
  });

  it('should show Create panel when no runtime', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    // TODO(RW-7152): This test is incorrectly depending on "default" values in runtime-panel, and
    // not general analysis. Ensure this test passes for the right reasons when fixing.
    const computeDefaults = wrapper.find('#compute-resources').first();
    // defaults to generalAnalysis preset, which is a n1-standard-4 machine with a 100GB disk
    expect(computeDefaults.text()).toEqual('- Compute size of 4 CPUs, 15 GB memory, and a 100 GB disk')
  });

  it('should allow creation when no runtime exists with defaults', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    if (enablePd) {
      expect(runtimeApiStub.runtime.gceWithPdConfig.machineType).toEqual('n1-standard-4');
    } else {
      expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual('n1-standard-4');
    }
  });

  it('should show customize after create', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapperBefore = await component();

    await mustClickButton(wrapperBefore, 'Create');

    const wrapperAfter = await component();
    expect(wrapperAfter.find('#runtime-cpu').exists()).toBeTruthy();
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is GeneralAnalysis', async() => {
    // In the case where the user's latest runtime is a preset (GeneralAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime
    // skip this test after enabling pd
    if (enablePd) {
      return;
    }
    const runtime = {...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: 1000
      },
      dataprocConfig: null
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expectEqualFields(
      runtimeApiStub.runtime.gceConfig,
      runtimePresets.generalAnalysis.runtimeTemplate.gceConfig,
      ['machineType', 'diskSize']
    );
  });

  it('should create runtime with preset values instead of getRuntime values if configurationType is HailGenomicsAnalysis', async() => {
    // In the case where the user's latest runtime is a preset (HailGenomicsAnalysis in this case)
    // we should ignore the other runtime config values that were delivered with the getRuntime response
    // and instead, defer to the preset values defined in runtime-presets.ts when creating a new runtime

    const runtime = {...runtimeApiStub.runtime,
      status: RuntimeStatus.Deleted,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      gceConfig: null,
      dataprocConfig: {
        ...defaultDataprocConfig(),
        masterMachineType: 'n1-standard-16',
        masterDiskSize: 999,
        workerDiskSize: 444,
        numberOfWorkers: 5
      }
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();
    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expectEqualFields(
      runtimeApiStub.runtime.dataprocConfig,
      runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig,
      ['masterMachineType', 'masterDiskSize', 'workerDiskSize', 'numberOfWorkers']
    );
  });

  const expectEqualFields = (a, b, fieldNames) => {
    const pick = fp.flow(fp.pick(fieldNames));
    expect(pick(a)).toEqual(pick(b));
  };

  it('should allow creation when runtime has error status', async() => {
    const runtime = {...runtimeApiStub.runtime,
      status: RuntimeStatus.Error,
      errors: [{errorMessage: 'I\'m sorry Dave, I\'m afraid I can\'t do that'}],
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: 100
      },
      dataprocConfig: null
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Try Again');

    // Kicks off a deletion to first clear the error status runtime.
    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should allow creation with update from error', async() => {
    const runtime = {...runtimeApiStub.runtime,
      status: RuntimeStatus.Error,
      errors: [{errorMessage: 'I\'m sorry Dave, I\'m afraid I can\'t do that'}],
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: {
        ...defaultGceConfig(),
        machineType: 'n1-standard-16',
        diskSize: 100
      },
      dataprocConfig: null
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    await pickMainCpu(wrapper, 8);
    await mustClickButton(wrapper, 'Try Again');

    // Kicks off a deletion to first clear the error status runtime.
    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should disable controls when runtime has non-updateable status', async() => {
    runtimeApiStub.runtime.status = RuntimeStatus.Stopping;
    runtimeStoreStub.runtime = runtimeApiStub.runtime;

    const wrapper = await component();

    expect(wrapper.find('#runtime-presets-menu').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('#runtime-cpu').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find(Button).find({'aria-label': 'Next'}).first().prop('disabled')).toBeTruthy();
  });

  it('should allow creation with GCE config', async() => {
    // skip this test after enabling pd
    if (enablePd) {
      return;
    }
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 52);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.gceConfig).toEqual({
      machineType: 'n1-highmem-8',
      diskSize: MIN_DISK_SIZE_GB + 10,
      gpuConfig: null
    });
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // master settings
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);

    if (enablePd) {
      await pickComputeType(wrapper, ComputeType.Dataproc);

      await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    } else {
      await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);

      await pickComputeType(wrapper, ComputeType.Dataproc);
    }
    // worker settings
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.dataprocConfig).toEqual({
      masterMachineType: 'n1-standard-2',
      masterDiskSize: MIN_DISK_SIZE_GB,
      workerMachineType: 'n1-standard-8',
      workerDiskSize: 300,
      numberOfWorkers: 10,
      numberOfPreemptibleWorkers: 20
    });
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should allow configuration via GCE preset', async() => {
    // skip this test after enabling pd
    if (enablePd) {
      return;
    }
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Ensure set the form to something non-standard to start
    await pickMainCpu(wrapper, 8);
    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickPreset(wrapper, runtimePresets.generalAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
    expect(runtimeApiStub.runtime.gceConfig)
      .toEqual(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig);
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should set runtime preset values in customize panel instead of getRuntime values if configurationType is GeneralAnalysis',
    async() => {
      // skip this test after enabling pd
      if (enablePd) {
        return;
      }
      const runtime = {
        ...runtimeApiStub.runtime,
        status: RuntimeStatus.Deleted,
        configurationType: RuntimeConfigurationType.GeneralAnalysis,
        gceConfig: {
          ...defaultGceConfig(),
          machineType: 'n1-standard-16',
          diskSize: 1000
        },
        dataprocConfig: null
      };
      runtimeApiStub.runtime = runtime;
      runtimeStoreStub.runtime = runtime;

      const wrapper = await component();
      await mustClickButton(wrapper, 'Customize');

      expect(getMainCpu(wrapper)).toEqual(findMachineByName(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.machineType).cpu);
      expect(getMainRam(wrapper)).toEqual(findMachineByName(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.machineType).memory);
      expect(getMainDiskSize(wrapper)).toEqual(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.diskSize);
    });

  it('should set runtime preset values in customize panel instead of getRuntime values if configurationType is HailGenomicsAnalysis',
    async() => {
      const runtime = {...runtimeApiStub.runtime,
        status: RuntimeStatus.Deleted,
        configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
        gceConfig: null,
        dataprocConfig: {
          ...defaultDataprocConfig(),
          masterMachineType: 'n1-standard-16',
          masterDiskSize: 999,
          workerDiskSize: 444,
          numberOfWorkers: 5
        }
      };
      runtimeApiStub.runtime = runtime;
      runtimeStoreStub.runtime = runtime;

      const wrapper = await component();
      await mustClickButton(wrapper, 'Customize');

      expect(getMainCpu(wrapper)).
        toEqual(findMachineByName(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig.masterMachineType).cpu);
      expect(getMainRam(wrapper)).
        toEqual(findMachineByName(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig.masterMachineType).memory);
      expect(getMainDiskSize(wrapper)).toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig.masterDiskSize);
      expect(getWorkerDiskSize(wrapper)).toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig.workerDiskSize);
      expect(getNumWorkers(wrapper)).toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig.numberOfWorkers);
    });

  it('should allow configuration via dataproc preset from modified form', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    if (enablePd) {
      await pickComputeType(wrapper, ComputeType.Dataproc);
      await pickMainDiskSize(wrapper, 100);
    } else {
      await pickMainDiskSize(wrapper, 100);
      await pickComputeType(wrapper, ComputeType.Dataproc);
    }
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should tag as user override after preset modification', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Take the preset but make a solitary modification.
    await pickPreset(wrapper, runtimePresets.hailAnalysis);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
  });

  it('should tag as preset if configuration matches', async() => {
    // skip this test after enabling pd
    if (enablePd) {
      return;
    }
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;

    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');

    // Take the preset, make a change, then revert.
    await pickPreset(wrapper, runtimePresets.generalAnalysis);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 2);
    await pickComputeType(wrapper, ComputeType.Standard);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
  });

  it('should restrict memory options by cpu', async() => {
    const wrapper = await component();

    wrapper.find('div[id="runtime-cpu"]').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 8}).first().simulate('click');

    const memoryOptions = wrapper.find('#runtime-ram').first().find('.p-dropdown-item');
    expect(memoryOptions.exists()).toBeTruthy();

    // See app/utils/machines.ts, these are the valid memory options for an 8
    // CPU machine in GCE.
    expect(memoryOptions.map(m => m.text())).toEqual(['7.2', '30', '52']);
  });

  it('should disable the Next button if there are no changes and runtime is running', async() => {
    const wrapper = await component();

    expect(wrapper.find(Button).find({'aria-label': 'Next'}).first().prop('disabled')).toBeTruthy();
  });

  it('should respect divergent sets of valid machine types across compute types', async() => {
    const wrapper = await component();

    await pickMainCpu(wrapper, 1);
    await pickMainRam(wrapper, 3.75);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // n1-standard-1 is illegal for Dataproc, so it should restore the default.
    expect(getMainCpu(wrapper)).toBe(4);
    expect(getMainRam(wrapper)).toBe(15);
  });

  it('should carry over valid main machine type across compute types', async() => {
    const wrapper = await component();

    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // n1-standard-2 is legal for Dataproc, so it should remain.
    expect(getMainCpu(wrapper)).toBe(2);
    expect(getMainRam(wrapper)).toBe(7.5);
  });

  it('should warn user about reboot if there are updates that require one - increase disk size', async() => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should not warn user for updates where not needed - number of workers',
    async() => {
      const runtime = {
        ...runtimeApiStub.runtime,
        gceConfig: null,
        dataprocConfig: defaultDataprocConfig(),
        configurationType: RuntimeConfigurationType.UserOverride
      };
      runtimeStoreStub.runtime = runtime;

      const wrapper = await component();

      await pickNumWorkers(wrapper, getNumWorkers(wrapper) + 2);
      await mustClickButton(wrapper, 'Next');

      expect(wrapper.find(WarningMessage).exists()).toBeFalsy();
    });

  it('should not warn user for updates where not needed - number of preemptibles', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    await pickNumPreemptibleWorkers(wrapper, getNumPreemptibleWorkers(wrapper) + 2);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).exists()).toBeFalsy();
  });

  it('should warn user about reboot if there are updates that require one - CPU', async() => {
    const wrapper = await component();

    await pickMainCpu(wrapper, getMainCpu(wrapper) + 4);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - Memory', async() => {
    const wrapper = await component();

    // 15 GB -> 26 GB
    await pickMainRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async() => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) - 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    // 4 -> 8
    await pickWorkerCpu(wrapper, 8);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker RAM', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    // 15 -> 26
    await pickWorkerRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker Disk', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();
    await pickWorkerDiskSize(wrapper, getWorkerDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should retain original inputs when hitting cancel from the Confirm panel', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB + 10);
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickWorkerCpu(wrapper, 16);
    await pickWorkerRam(wrapper, 60);
    await pickNumPreemptibleWorkers(wrapper, 3);
    await pickNumWorkers(wrapper, 5);
    await pickWorkerDiskSize(wrapper, DATAPROC_WORKER_MIN_DISK_SIZE_GB);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Cancel');

    expect(getMainDiskSize(wrapper)).toBe(MIN_DISK_SIZE_GB + 10);
    expect(getMainCpu(wrapper)).toBe(8);
    expect(getMainRam(wrapper)).toBe(30);
    expect(getWorkerCpu(wrapper)).toBe(16);
    expect(getWorkerRam(wrapper)).toBe(60);
    expect(getNumPreemptibleWorkers(wrapper)).toBe(3);
    expect(getNumWorkers(wrapper)).toBe(5);
    expect(getWorkerDiskSize(wrapper)).toBe(DATAPROC_WORKER_MIN_DISK_SIZE_GB);
  });

  it('should disable Next button if Runtime is in between states', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig(), status: RuntimeStatus.Creating};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();
    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    expect(wrapper.find(Button).find({'aria-label': 'Next'}).first().prop('disabled')).toBeTruthy();
  });

  it('should send an updateRuntime API call if runtime changes do not require a delete', async() => {
    const wrapper = await component();

    const updateSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    const deleteSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(updateSpy).toHaveBeenCalled();
    expect(deleteSpy).toHaveBeenCalledTimes(0);
  });

  it('should send a delete call if an update requires delete', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should show create button if runtime is deleted', async() => {
    const runtime = {...runtimeApiStub.runtime, status: RuntimeStatus.Deleted};
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    expect(wrapper.find(Button).find({'aria-label': 'Create'}).first().exists()).toBeTruthy();
  });

  it('should add additional options when the compute type changes', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    expect(wrapper.exists('span[id="num-workers"]')).toBeTruthy();
    expect(wrapper.exists('span[id="num-preemptible"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-cpu"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-ram"]')).toBeTruthy();
    expect(wrapper.exists('span[id="worker-disk"]')).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async() => {
    const wrapper = await component();

    const costEstimator = () => wrapper.find('[data-test-id="cost-estimator"]');
    expect(costEstimator().exists()).toBeTruthy();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    const runningCost = () => costEstimator().find('[data-test-id="running-cost"]');
    const storageCost = () => costEstimator().find('[data-test-id="storage-cost"]');
    expect(runningCost().text()).toEqual('$0.20/hour');
    expect(storageCost().text()).toEqual('< $0.01/hour');

    // Change the machine to n1-standard-8 and bump the storage to 300GB. This should make the running cost 40 cents an hour and the storage cost 2 cents an hour.
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickMainDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$0.40/hour');
    expect(storageCost().text()).toEqual('$0.02/hour');

    // Selecting the General Analysis preset should bring the machine back to n1-standard-4 with 100GB storage.
    await pickPreset(wrapper, {displayName: 'General Analysis'});
    expect(runningCost().text()).toEqual('$0.20/hour');
    expect(storageCost().text()).toEqual('< $0.01/hour');

    // After selecting Dataproc, the Dataproc defaults should make the running cost 72 cents an hour. The storage cost increases due to worker disk.
    await pickComputeType(wrapper, ComputeType.Dataproc);
    expect(runningCost().text()).toEqual('$0.72/hour');
    expect(storageCost().text()).toEqual('$0.02/hour');

    // Bump up all the worker values to increase the price on everything.
    await pickNumWorkers(wrapper, 4);
    await pickNumPreemptibleWorkers(wrapper, 4);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$2.87/hour');
    expect(storageCost().text()).toEqual('$0.14/hour');
  });

  it('should update the cost estimator when master machine changes', async() => {
    const runtime = {...runtimeApiStub.runtime,
      status: RuntimeStatus.Running,
      configurationType: RuntimeConfigurationType.UserOverride,
      gceConfig: null,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 1000,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: DATAPROC_WORKER_MIN_DISK_SIZE_GB,
      }
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    const costEstimator = () => wrapper.find('[data-test-id="cost-estimator"]');
    expect(costEstimator().exists()).toBeTruthy();

    const runningCost = () => costEstimator().find('[data-test-id="running-cost"]');
    const storageCost = () => costEstimator().find('[data-test-id="storage-cost"]');
    expect(runningCost().text()).toEqual('$0.77/hour');
    expect(storageCost().text()).toEqual('$0.07/hour');

    // Switch to n1-highmem-4, double disk size.
    await pickMainRam(wrapper, 26);
    await pickMainDiskSize(wrapper, 2000);
    expect(runningCost().text()).toEqual('$0.87/hour');
    expect(storageCost().text()).toEqual('$0.13/hour');
  });

  it('should allow runtime deletion', async() => {
    const wrapper = await component();

    wrapper.find(LinkButton).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    await mustClickButton(wrapper, 'Delete');

    // Runtime should be deleting, and panel should have closed.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Deleting);
    expect(onClose).toHaveBeenCalled();
  });

  it('should allow cancelling runtime deletion', async() => {
    const wrapper = await component();

    wrapper.find(LinkButton).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    // Click cancel
    await mustClickButton(wrapper, 'Cancel');

    // Runtime should still be active, and confirm page should no longer be visible.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Running);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('should display the Running runtime status icon in state Running', async() => {
    const wrapper = await component();

    expect(wrapper.find('[data-test-id="runtime-status-icon-running"]').exists()).toBeTruthy();
  });

  it('should display a compute-none when there is no runtime', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    expect(wrapper.find('[data-test-id="runtime-status-icon-none"]').exists()).toBeTruthy();
  });

  it('should prevent runtime creation when disk size is invalid', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () => wrapper.find({'aria-label': 'Create'}).first();

    if (enablePd) {
      await pickPdSize(wrapper, 49);
      expect(getCreateButton().prop('disabled')).toBeTruthy();

      await pickPdSize(wrapper, 4900);
      expect(getCreateButton().prop('disabled')).toBeTruthy();

      await pickPdSize(wrapper, MIN_DISK_SIZE_GB);
    } else {
      await pickMainDiskSize(wrapper, 49);
      expect(getCreateButton().prop('disabled')).toBeTruthy();

      await pickMainDiskSize(wrapper, 4900);
      expect(getCreateButton().prop('disabled')).toBeTruthy();

      await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    }
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerDiskSize(wrapper, 49);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickWorkerDiskSize(wrapper, 4900);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(wrapper, DATAPROC_WORKER_MIN_DISK_SIZE_GB);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime update when disk size is invalid', async() => {
    const wrapper = await component();
    const getNextButton = () => wrapper.find({'aria-label': 'Next'}).first();

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

    await pickMainDiskSize(wrapper, MIN_DISK_SIZE_GB);
    await pickWorkerDiskSize(wrapper, DATAPROC_WORKER_MIN_DISK_SIZE_GB);
    expect(getNextButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime creation when running cost is too high for free tier', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () => wrapper.find({'aria-label': 'Create'}).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // This should make the cost about $50/hour.
    await pickNumWorkers(wrapper, 200);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime creation when worker count is invalid', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () => wrapper.find({'aria-label': 'Create'}).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickNumWorkers(wrapper, 0);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 1);
    expect(getCreateButton().prop('disabled')).toBeTruthy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should allow runtime creation when running cost is too high for user provided billing', async() => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      billingAccountName: 'user provided billing',
    });

    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () => wrapper.find({'aria-label': 'Create'}).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // This should make the cost about $50/hour.
    await pickNumWorkers(wrapper, 20000);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should prevent runtime creation when running cost is too high for paid tier', async() => {
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
    });
    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');
    const getCreateButton = () => wrapper.find({'aria-label': 'Create'}).first();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // This should make the cost about $140/hour.
    await pickNumWorkers(wrapper, 600);
    expect(getCreateButton().prop('disabled')).toBeFalsy();

    // This should make the cost around $160/hour.
    await pickNumWorkers(wrapper, 700);
    // We don't want to disable for user provided billing. Just put a warning.
    expect(getCreateButton().prop('disabled')).toBeFalsy();
    expect(wrapper.find('[data-test-id="runtime-warning-messages"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="runtime-error-messages"]').exists()).toBeFalsy();

    await pickNumWorkers(wrapper, 2);
    expect(getCreateButton().prop('disabled')).toBeFalsy();
  });

  it('should render disabled panel when creator billing disabled', async () => {
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
      billingStatus: BillingStatus.INACTIVE,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
    });
    const wrapper = await component();

    const disabledPanel = wrapper.find({'data-test-id': 'runtime-disabled-panel'});
    expect(disabledPanel.exists()).toBeTruthy();
    const createPanel = wrapper.find({'data-test-id': 'runtime-create-panel'});
    expect(createPanel.exists()).toBeFalsy();
  });

  it('should allow creating gce with GPU', async() => {
    if (!enableGpu) {
      return;
    }
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    await clickEnableGpu(wrapper);
    await pickGpuType(wrapper, 'nvidia-tesla-t4');
    await pickGpuNum(wrapper, 2);
    if (enablePd) {
      await pickMainCpu(wrapper, 8);
      await pickPdSize(wrapper, 75);
      await mustClickButton(wrapper, 'Create');
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig.gpuType).toEqual('nvidia-tesla-t4');
      expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig.numOfGpus).toEqual(2);
    } else {
      await pickMainCpu(wrapper, 8);
      await pickMainDiskSize(wrapper, 75);
      await mustClickButton(wrapper, 'Create');
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceConfig.gpuConfig.gpuType).toEqual('nvidia-tesla-t4');
      expect(runtimeApiStub.runtime.gceConfig.gpuConfig.numOfGpus).toEqual(2);
    }
  });

  it('should allow creating gce without GPU', async() => {
    if (!enableGpu) {
      return;
    }
    runtimeApiStub.runtime = null;
    runtimeStoreStub.runtime = null;
    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');
    await pickComputeType(wrapper, ComputeType.Standard);
    if (enablePd) {
      await pickMainCpu(wrapper, 8);
      await pickPdSize(wrapper, 75);
      await mustClickButton(wrapper, 'Create');
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceWithPdConfig.gpuConfig).toEqual(null);
    } else {
      await pickMainCpu(wrapper, 8);
      await pickMainDiskSize(wrapper, 75);
      await mustClickButton(wrapper, 'Create');
      expect(runtimeApiStub.runtime.status).toEqual('Creating');
      expect(runtimeApiStub.runtime.gceConfig.gpuConfig).toEqual(null);
    }
  });

  it('should disable worker count updates for stopped dataproc cluster', async() => {
    const runtime = {
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Stopped,
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      gceConfig: null,
      dataprocConfig: defaultDataprocConfig()
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();

    const workerCountInput = wrapper.find('#num-workers').first();
    expect(workerCountInput.prop('disabled')).toBeTruthy();
    expect(workerCountInput.prop('tooltip')).toBeTruthy();

    const preemptibleCountInput = wrapper.find('#num-preemptible').first();
    expect(preemptibleCountInput.prop('disabled')).toBeTruthy();
    expect(preemptibleCountInput.prop('tooltip')).toBeTruthy();
  });

  it('should allow worker configuration for stopped GCE runtime', async() => {
    const runtime = {
      ...runtimeApiStub.runtime,
      status: RuntimeStatus.Stopped,
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      gceConfig: defaultGceConfig(),
      dataprocConfig: null
    };
    runtimeApiStub.runtime = runtime;
    runtimeStoreStub.runtime = runtime;

    const wrapper = await component();
    await pickComputeType(wrapper, ComputeType.Dataproc);

    const workerCountInput = wrapper.find('#num-workers').first();
    expect(workerCountInput.prop('disabled')).toBeFalsy();
    expect(workerCountInput.prop('tooltip')).toBeFalsy();

    const preemptibleCountInput = wrapper.find('#num-preemptible').first();
    expect(preemptibleCountInput.prop('disabled')).toBeFalsy();
    expect(preemptibleCountInput.prop('tooltip')).toBeFalsy();
  });
});
