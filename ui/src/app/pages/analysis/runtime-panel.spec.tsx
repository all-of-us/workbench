import {mount} from 'enzyme';
import * as React from 'react';

import {Spinner} from 'app/components/spinners';
import {RuntimePanel, Props} from 'app/pages/analysis/runtime-panel';
import {workspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {RuntimeApi} from 'generated/fetch/api';
import {WorkspaceAccessLevel} from 'generated/fetch';
import {runtimeStore} from "app/utils/stores";
import {Runtime, RuntimeStatus} from "generated/fetch";

describe('RuntimePanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;

  const component = () => {
    return mount(<RuntimePanel {...props}/>);
  };

  beforeEach(() => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
    runtimeStore.set({runtime: runtimeApiStub.runtime, workspaceNamespace: workspaceStubs[0].namespace});
    props = {
      workspace: {...workspaceStubs[0], accessLevel: WorkspaceAccessLevel.WRITER}
    };
  });

  it('should show loading spinner while loading', async() => {
    const wrapper = component();

    // Check before ticking - stub returns the runtime asynchronously.
    expect(!wrapper.exists({'data-test-id': 'runtime-panel'}))
    expect(wrapper.exists(Spinner));

    // Now getRuntime returns.
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists({'data-test-id': 'runtime-panel'}));
    expect(!wrapper.exists(Spinner));
  });

  it('should render runtime details', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const imageDropdown = wrapper.find({'data-test-id': 'runtime-image-dropdown'}).find('label').first();
    expect(imageDropdown.exists()).toBeTruthy();

    expect(imageDropdown.text()).toContain(runtimeApiStub.runtime.toolDockerImage);
  });

  it('should restrict memory options by cpu', async() => {
    runtimeApiStub.runtime.dataprocConfig.masterMachineType = 'n1-standard-8';

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const memoryOptions = wrapper.find('#runtime-ram .p-dropdown-item');
    expect(memoryOptions.exists()).toBeTruthy();

    // See app/utils/machines.ts, these are the valid memory options for an 8
    // CPU machine in GCE.
    expect(memoryOptions.map(m => m.text())).toEqual(['7.2', '30', '52']);
  });
});
