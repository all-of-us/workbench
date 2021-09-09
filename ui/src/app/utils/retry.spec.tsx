import * as React from 'react';
import {jest} from '@jest/globals';

import {apiCallWithGatewayTimeoutRetries} from 'app/utils/retry';

const functionStub = {
  successfulFunction() {
    return Promise.resolve();
  },
  failedFunction() {
    return Promise.resolve({status: 504}).then(response => {throw response;});
  }
};


describe('IndexUtils', () => {
  it('should not retry if successful', async() => {
    const successfulFunctionSpy = jest.spyOn(functionStub, 'successfulFunction');
    await apiCallWithGatewayTimeoutRetries(() => functionStub.successfulFunction());
    expect(successfulFunctionSpy).toHaveBeenCalledTimes(1);
  });

  it('should retry three times by default using apiCallWithGatewayTimeout', async() => {
    const failedFunctionSpy = jest.spyOn(functionStub, 'failedFunction');
    await apiCallWithGatewayTimeoutRetries(() => functionStub.failedFunction(), 3, 1).catch(() => {});
    expect(failedFunctionSpy).toHaveBeenCalledTimes(4);
  });
});
