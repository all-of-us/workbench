import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient, statusApi} from 'app/services/swagger-fetch-clients';
import {fetchWithGlobalErrorHandler} from 'app/utils/retry';
import {StatusApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {StatusApiStub} from 'testing/stubs/status-api-stub';
import {ErrorHandler} from './error-handler';


describe('ErrorHandler', () => {
  const description = 'test';

  const component = () => {
    return mount(<ErrorHandler/>);
  };

  beforeEach(() => {
    registerApiClient(StatusApi, new StatusApiStub());
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should check status on 500', async () => {
    const wrapper = component();
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the global error handler
    // if we explicitly catch.
    try {
      await fetchWithGlobalErrorHandler(() => Promise.reject({status: 500}));
    } catch (e) {
      // expected
    }
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should check status on 503', async () => {
    const wrapper = component();
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the global error handler
    // if we explicitly catch.
    try {
      await fetchWithGlobalErrorHandler(() => Promise.reject({status: 503}));
    } catch (e) {
      // expected
    }
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should not check status on 502', async () => {
    const wrapper = component();
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the global error handler
    // if we explicitly catch.
    try {
      await fetchWithGlobalErrorHandler(() => Promise.reject({status: 502}));
    } catch (e) {
      // expected
    }
    await waitOneTickAndUpdate(wrapper);
    expect(spy).not.toHaveBeenCalled();
  });
});
