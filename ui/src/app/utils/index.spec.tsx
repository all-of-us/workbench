import * as React from 'react';

import {apiCallWithGatewayTimeoutRetries} from 'app/utils/index';

const functionStub = {
  successfulFunction() {
    return Promise.resolve();
  },
  failedFunction() {
    return Promise.resolve({status: 504}).then(response => {throw response});
  }
}


describe('IndexUtils', () => {
  it('should not retry if successful', async() => {
    const successfulFunctionSpy = spyOn(functionStub, 'successfulFunction').and.callThrough();
    await apiCallWithGatewayTimeoutRetries(() => functionStub.successfulFunction());
    expect(successfulFunctionSpy).toHaveBeenCalledTimes(1);
  });

  it('should retry three times by default using apiCallWithGatewayTimeout', async() => {
    const failedFunctionSpy = spyOn(functionStub, 'failedFunction').and.callThrough();
    await apiCallWithGatewayTimeoutRetries(() => functionStub.failedFunction(), 3, 1).catch(() => {});
    expect(failedFunctionSpy).toHaveBeenCalledTimes(4);
  });
});
