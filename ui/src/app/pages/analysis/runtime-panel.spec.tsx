import {mount} from 'enzyme';
import { act } from 'react-dom/test-utils';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Spinner} from 'app/components/spinners';
import {RuntimePanel, Props} from 'app/pages/analysis/runtime-panel';
import {workspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {RuntimeApi} from 'generated/fetch/api';
import {WorkspaceAccessLevel} from 'generated/fetch';
import {runtimeStore} from 'app/utils/stores';
import {cdrVersionListResponse, CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {cdrVersionStore, serverConfigStore} from 'app/utils/navigation';



describe('RuntimePanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;

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
    runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    const createButton = wrapper.find(Button).find({'aria-label': 'Create'}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    act(() => { createButton.simulate('click') });
    await waitOneTickAndUpdate(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual('n1-standard-4');
  });

  it('should allow creation with GCE config', async() => {
    runtimeApiStub.runtime = null;
    runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    act(() => { wrapper.find('#runtime-cpu').first().simulate('click') });
    act(() => { wrapper.find('#runtime-cpu .p-dropdown-item').find({'aria-label': 8}).first().simulate('click') });
    await waitOneTickAndUpdate(wrapper);

    act(() => { wrapper.find('#runtime-ram').first().find('.p-dropdown-item').first().simulate('click') });
    act(() => { wrapper.find('#runtime-ram .p-dropdown-item').find({'aria-label': 52}).first().simulate('click') });

    // TODO: Find a way to invoke this without props.
    act(() => { wrapper.find('*[id="runtime-disk"]').first().prop('onChange')({value: 75} as any)});

    const createButton = wrapper.find(Button).find({'aria-label': 'Create'}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    act(() => { createButton.simulate('click') });
    await waitOneTickAndUpdate(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual('n1-highmem-8');
    expect(runtimeApiStub.runtime.gceConfig.diskSize).toEqual(75);
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async() => {
    runtimeApiStub.runtime = null;
    runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = component();
    await handleUseEffect(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // master settings
    act(() => { wrapper.find('#runtime-cpu').first().simulate('click') });
    act(() => { wrapper.find('#runtime-cpu .p-dropdown-item').find({'aria-label': 2}).first().simulate('click') });
    await waitOneTickAndUpdate(wrapper);
    act(() => { wrapper.find('#runtime-ram').first().find('.p-dropdown-item').first().simulate('click') });
    act(() => { wrapper.find('#runtime-ram .p-dropdown-item').find({'aria-label': 7.5}).first().simulate('click') });
    act(() => { wrapper.find('#runtime-disk').first().prop('onChange')({value: 100} as any)});

    // pick Dataproc
    act(() => { wrapper.find('#runtime-compute').first().simulate('click') });
    act(() => { wrapper.find('#runtime-compute .p-dropdown-item').find({'aria-label': 'Dataproc Cluster'}).first().simulate('click') });
    await waitOneTickAndUpdate(wrapper);

    // worker settings
    act(() => { wrapper.find('#worker-cpu').first().simulate('click') });
    act(() => { wrapper.find('#worker-cpu .p-dropdown-item').find({'aria-label': 8}).first().simulate('click') });
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('#worker-ram').first().find('.p-dropdown-item').first().simulate('click');
    wrapper.find('#worker-ram .p-dropdown-item').find({'aria-label': 30}).first().simulate('click');
    act(() => { wrapper.find('#worker-disk').first().prop('onChange')({value: 300} as any)});
    act(() => { wrapper.find('#num-workers').first().prop('onChange')({value: 10} as any)});
    act(() => { wrapper.find('#num-preemptible').first().prop('onChange')({value: 20} as any)});

    const createButton = wrapper.find(Button).find({'aria-label': 'Create'}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    act(() => { createButton.simulate('click') });
    await waitOneTickAndUpdate(wrapper);

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
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
});
