import * as React from 'react';

import { StatusApi } from 'generated/fetch';

import { render, waitFor } from '@testing-library/react';
import {
  registerApiClient,
  statusApi,
} from 'app/services/swagger-fetch-clients';
import { fetchWithSystemErrorHandler } from 'app/utils/errors';

import { StatusApiStub } from 'testing/stubs/status-api-stub';

import { SystemErrorHandler } from './system-error-handler';

describe(SystemErrorHandler.name, () => {
  beforeEach(() => {
    registerApiClient(StatusApi, new StatusApiStub());
  });

  it('should check status on 500', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the system error handler
    // if we explicitly catch.
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 500 }));
    } catch (e) {
      // expected
    }

    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  it('should check status on 503', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the system error handler
    // if we explicitly catch.
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 503 }));
    } catch (e) {
      // expected
    }
    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  it('should not check status on 502', async () => {
    render(<SystemErrorHandler />);
    const spy = jest.spyOn(statusApi(), 'getStatus');
    // We need to use a try statement here, as opposed to catching on the promise itself, because the order
    // of precedence would mean that the error gets swallowed and isn't caught within the system error handler
    // if we explicitly catch.
    try {
      await fetchWithSystemErrorHandler(() => Promise.reject({ status: 502 }));
    } catch (e) {
      // expected
    }
    expect(spy).not.toHaveBeenCalled();
  });
});
