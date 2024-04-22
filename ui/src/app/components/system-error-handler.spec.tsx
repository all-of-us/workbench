import * as React from 'react';

import { StatusApi } from 'generated/fetch';

import { render } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { fetchWithSystemErrorHandler } from 'app/utils/errors';

import { StatusApiStub } from 'testing/stubs/status-api-stub';

import { SystemErrorHandler } from './system-error-handler';

describe('SystemErrorHandler', () => {
  beforeEach(() => {
    registerApiClient(StatusApi, new StatusApiStub());
  });

  it('should render', () => {
    const { getByTestId } = render(<SystemErrorHandler />);
    expect(getByTestId('system-error-handler')).toBeInTheDocument();
  });

  it('should check status on 500', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(StatusApi.prototype, 'getStatus');
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 500 }));
    } catch (e) {
      // expected
    }
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should check status on 503', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(StatusApi.prototype, 'getStatus');
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 503 }));
    } catch (e) {
      // expected
    }
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should not check status on 502', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(StatusApi.prototype, 'getStatus');
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 502 }));
    } catch (e) {
      // expected
    }
    expect(spy).not.toHaveBeenCalled();
  });
});
