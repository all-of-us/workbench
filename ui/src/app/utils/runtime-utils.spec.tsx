import { act } from 'react-dom/test-utils';
import {mount} from 'enzyme';
import {useCustomRuntime} from 'app/utils/runtime-utils';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {RuntimeApi} from 'generated/fetch/api';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import * as React from 'react';

const Runtime = ({id}) => {
  const [runtime, ] = useCustomRuntime('test')
  const {runtimeName = ''} = runtime || {};
  return <div id={id}>{runtimeName}</div>
}

const TestComponent = () => {
  return <div>
    <Runtime id='1'/>
    <Runtime id='2'/>
  </div>;
}

const handleUseEffect = async (component) => {
  await act(async () => {
    await Promise.resolve(component);
    await new Promise(resolve => setImmediate(resolve));
  });
}

describe('runtime-utils', () => {
  let runtimeApiStub: RuntimeApiStub;
  
  beforeEach(() => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
  });

  // TODO: additional tests around updating the store should be written.
  //       Enzyme has difficulty dealing with react effects and async operations
  it('should initialize with a value', async () => {
    const wrapper = mount(<TestComponent/>);
    await handleUseEffect(wrapper);
    const runtime = (id) => wrapper.find({id}).first();

    await waitOneTickAndUpdate(wrapper);

    expect(runtime('1').text()).toEqual('Runtime Name');
    expect(runtime('2').text()).toEqual('Runtime Name');
  });
});
