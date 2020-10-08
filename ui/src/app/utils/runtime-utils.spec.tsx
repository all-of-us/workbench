import { atom } from './subscribable';
import { act } from 'react-dom/test-utils';
import {mount} from 'enzyme';
import {useCustomRuntime} from 'app/utils/runtime-utils';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {RuntimeApi} from 'generated/fetch/api';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import * as React from 'react';

export const delay = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

const promiseComplete = () => {
  let resolveHandler;
  const promise = new Promise<void>((resolve) => {
    resolveHandler = value => value && resolve();
  });
  return {promise, resolveHandler};
};

const Runtime = ({id}) => {
  const [runtime, setRuntime] = useCustomRuntime('test')

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

  it('should initialize with a value', async () => {

    const wrapper = mount(<TestComponent/>);
    await handleUseEffect(wrapper);
    const runtime = wrapper.find({id:'1'}).first();

    expect(runtime.text()).toEqual('Runtime Name');
    expect({value: 1}).toEqual({value: 1});
  });

});
