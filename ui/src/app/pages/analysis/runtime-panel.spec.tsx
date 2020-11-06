import {mount} from 'enzyme';
import { act } from 'react-dom/test-utils';
import * as React from 'react';

import {Button, Link} from 'app/components/buttons';
import {Spinner} from 'app/components/spinners';
import {ConfirmDelete, RuntimePanel, Props} from 'app/pages/analysis/runtime-panel';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ComputeType} from 'app/utils/machines';
import {cdrVersionStore, serverConfigStore} from 'app/utils/navigation';
import {runtimePresets} from 'app/utils/runtime-presets';
import {runtimeStore} from 'app/utils/stores';
import {RuntimeConfigurationType, RuntimeStatus, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {RuntimeApi} from 'generated/fetch/api';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {cdrVersionListResponse, CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {WorkspacesApiStub, workspaceStubs} from 'testing/stubs/workspaces-api-stub';

describe('RuntimePanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;
  let workspacesApiStub: WorkspacesApiStub;

  const component = () => {
    return mount(<RuntimePanel {...props}/>);
  };

  // Invokes react "act" in order to handle async component updates: https://reactjs.org/docs/testing-recipes.html#act
  // This code waits for all updates to complete.
  // There is probably a better way to handle this - but it may mean not using enzyme
  const handleUseEffect = async (component) => {
    await act(async () => {
      await Promise.resolve(component); // Wait for the component to finish rendering (mount returns a promise)
      await new Promise(resolve => setImmediate(resolve)); // Wait for all outstanding requests to complete
    });
  }

  beforeEach(() => {
    cdrVersionStore.next(cdrVersionListResponse);
    serverConfigStore.next({...defaultServerConfig, enableCustomRuntimes: true});

    runtimeApiStub = new RuntimeApiStub();
    runtimeApiStub.runtime.dataprocConfig = null;
    registerApiClient(RuntimeApi, runtimeApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub)

    runtimeStore.set({runtime: runtimeApiStub.runtime, workspaceNamespace: workspaceStubs[0].namespace});
    props = {
      workspace: {
        ...workspaceStubs[0],
        accessLevel: WorkspaceAccessLevel.WRITER,
        cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
      },
      cdrVersionListResponse
    };
  });

  const pickDropdownOption = async(wrapper, id, label) => {
    act(() => { wrapper.find(id).first().simulate('click') });
    const item = wrapper.find(`${id} .p-dropdown-item`).find({'aria-label': label}).first()
    expect(item.exists()).toBeTruthy();

    act(() => { item.simulate('click') });

    // In some cases, picking an option may require some waiting, e.g. for
    // rerendering of RAM options based on CPU selection.
    await waitOneTickAndUpdate(wrapper);
  };

  const enterNumberInput = (wrapper, id, value) => {
    // TODO: Find a way to invoke this without props.
    act(() => { wrapper.find(id).first().prop('onChange')({value} as any)});
  };

  const pickMainCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#runtime-cpu', cpu);
  const pickMainRam = (wrapper, ram) => pickDropdownOption(wrapper, '#runtime-ram', ram);
  const pickMainDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#runtime-disk', diskSize);
  const pickComputeType = (wrapper, computeType) => pickDropdownOption(wrapper, '#runtime-compute', computeType);
  const pickWorkerCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#worker-cpu', cpu);
  const pickWorkerRam = (wrapper, cpu) => pickDropdownOption(wrapper, '#worker-ram', cpu);
  const pickWorkerDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#worker-disk', diskSize);
  const pickNumWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-workers', n);
  const pickNumPreemptibleWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-preemptible', n);

  const pickPreset = async(wrapper, {displayName}) => {
    act(() => { wrapper.find({'data-test-id': 'runtime-presets-menu'}).first().simulate('click') });
    act(() => {(document.querySelector(`#popup-root [aria-label="${displayName}"]`) as HTMLElement).click()});
    await waitOneTickAndUpdate(wrapper);
  }

  const mustClickCreateButton = async(wrapper) => {
    const createButton = wrapper.find(Button).find({'aria-label': 'Create'}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    act(() => { createButton.simulate('click') });
    await waitOneTickAndUpdate(wrapper);
  };

  it('should show loading spinner while loading', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);

    // Check before ticking - stub returns the runtime asynchronously.
    expect(!wrapper.exists({'data-test-id': 'runtime-panel'}));
    expect(wrapper.exists(Spinner));

    // Now getRuntime returns.
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists({'data-test-id': 'runtime-panel'}));
    expect(!wrapper.exists(Spinner));
  });

  it('should allow creation when no runtime exists with defaults', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual('n1-standard-4');
  });

  it('should allow creation with GCE config', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 52);
    pickMainDiskSize(wrapper, 75);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.gceConfig).toEqual({
      machineType: 'n1-highmem-8',
      diskSize: 75
    });
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // master settings
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    pickMainDiskSize(wrapper, 100);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // worker settings
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    pickWorkerDiskSize(wrapper, 300);
    pickNumWorkers(wrapper, 10);
    pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.dataprocConfig).toEqual({
      masterMachineType: 'n1-standard-2',
      masterDiskSize: 100,
      workerMachineType: 'n1-standard-8',
      workerDiskSize: 300,
      numberOfWorkers: 10,
      numberOfPreemptibleWorkers: 20
    });
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should allow configuration via GCE preset', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Ensure set the form to something non-standard to start
    await pickMainCpu(wrapper, 8);
    pickMainDiskSize(wrapper, 75);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickPreset(wrapper, runtimePresets.generalAnalysis);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
    expect(runtimeApiStub.runtime.gceConfig)
      .toEqual(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig);
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset from modified form', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    pickMainDiskSize(wrapper, 100);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    pickWorkerDiskSize(wrapper, 300);
    pickNumWorkers(wrapper, 10);
    pickNumPreemptibleWorkers(wrapper, 20);

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should tag as user override after preset modification', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Take the preset but make a solitary modification.
    await pickPreset(wrapper, runtimePresets.hailAnalysis);
    pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
  });

  it('should tag as preset if configuration matches', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Take the preset, make a change, then revert.
    await pickPreset(wrapper, runtimePresets.generalAnalysis);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 2);
    await pickComputeType(wrapper, ComputeType.Standard);

    await mustClickCreateButton(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
  });

  it('should restrict memory options by cpu', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('div[id="runtime-cpu"]').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 8}).first().simulate('click');

    const memoryOptions = wrapper.find('#runtime-ram').first().find('.p-dropdown-item');
    expect(memoryOptions.exists()).toBeTruthy();

    // See app/utils/machines.ts, these are the valid memory options for an 8
    // CPU machine in GCE.
    expect(memoryOptions.map(m => m.text())).toEqual(['7.2', '30', '52']);
  });

  it('should toggle the disabled state of the update button when the configuration changes', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    const updateButton = () => wrapper.find(Button).find({'aria-label': 'Update'}).first();
    expect(updateButton().prop('disabled')).toBeTruthy();

    wrapper.find('#runtime-cpu .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 8}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeFalsy();

    wrapper.find('#runtime-ram').first().find('.p-dropdown-item').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 4}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeTruthy();

    wrapper.find('#runtime-ram .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 26}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeFalsy();

    wrapper.find('#runtime-ram .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 15}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeTruthy();

    wrapper.find('#runtime-ram .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 15}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeTruthy();

    wrapper.find('#runtime-compute .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 'Dataproc Cluster'}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeFalsy();

    wrapper.find('#runtime-compute .p-dropdown').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 'Standard VM'}).first().simulate('click');
    expect(updateButton().prop('disabled')).toBeTruthy();

  });

  it('should add additional options when the compute type changes', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('div[id="runtime-compute"]').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 'Dataproc Cluster'}).first().simulate('click');

    expect(wrapper.exists('span[id="num-workers"]')).toBeTruthy();
    expect(wrapper.exists('span[id="num-preemptible"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-cpu"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-ram"]')).toBeTruthy();
    expect(wrapper.exists('span[id="worker-disk"]')).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    const costEstimator = () => wrapper.find('[data-test-id="cost-estimator"]');
    expect(costEstimator()).toBeTruthy();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    const runningCost = () => costEstimator().find('[data-test-id="running-cost"]');
    const storageCost = () => costEstimator().find('[data-test-id="storage-cost"]');
    expect(runningCost().text()).toEqual('$0.20/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // Change the machine to n1-standard-8 and bump the storage to 300GB. This should make the running cost 40 cents an hour and the storage cost 2 cents an hour.
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    pickMainDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$0.40/hr');
    expect(storageCost().text()).toEqual('$0.02/hr');

    // Selecting the General Analysis preset should bring the machine back to n1-standard-4 with 50GB storage.
    await pickPreset(wrapper, {displayName: 'General Analysis'});
    expect(runningCost().text()).toEqual('$0.20/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // After selecting Dataproc, the Dataproc defaults should make the running cost 71 cents an hour. The storage cost should remain unchanged.
    await pickComputeType(wrapper, ComputeType.Dataproc);
    expect(runningCost().text()).toEqual('$0.71/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // Bump up all the worker values to increase the price on everything.
    pickNumWorkers(wrapper, 4);
    pickNumPreemptibleWorkers(wrapper, 4);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    pickWorkerDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$2.87/hr');
    expect(storageCost().text()).toEqual('$0.13/hr');
  })

  it('should allow runtime deletion', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find(Link).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    // Click delete
    wrapper.find(ConfirmDelete).find({'aria-label': 'Delete'}).first().simulate('click');

    // Runtime should be deleting, and confirm page should no longer be visible.
    await waitOneTickAndUpdate(wrapper);
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Deleting);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
  });

  it('should allow cancelling runtime deletion', async() => {
    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find(Link).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    // Click cancel
    wrapper.find(ConfirmDelete).find({'aria-label': 'Cancel'}).first().simulate('click');

    // Runtime should still be active, and confirm page should no longer be visible.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Running);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
  });
});
