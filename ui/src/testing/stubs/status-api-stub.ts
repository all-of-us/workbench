import { StatusApi, StatusResponse } from 'generated/fetch';
import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class StatusApiStub extends StatusApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  getStatus(): Promise<StatusResponse> {
    return new Promise<StatusResponse>((resolve) => {
      resolve({ firecloudStatus: false, notebooksStatus: false });
    });
  }
}
