import {mount} from 'enzyme';
import * as React from 'react';

import {Props, ResetRuntimeButton} from './reset-runtime-button';

import {runtimeApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';

import {RuntimeApi} from 'generated/fetch/api';


describe('ResetRuntimeButton', () => {
  let props: Props;

  const component = () => {
    return mount(<ResetRuntimeButton {...props}/>);
  };

  beforeEach(() => {
    props = {
      workspaceNamespace: 'billing-project-123',
    };

    registerApiClient(RuntimeApi, new RuntimeApiStub());
  });

  it('should not open the runtime reset modal when no runtime', () => {
    const wrapper = component();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });

  it('should allow deleting the runtime when there is one', async() => {
    const spy = jest.spyOn(runtimeApi(), 'deleteRuntime');
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
    wrapper.find('[data-test-id="reset-runtime-send"]').at(0).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalled();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });

});
