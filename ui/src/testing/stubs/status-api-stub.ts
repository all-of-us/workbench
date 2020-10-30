import {StatusApi, StatusResponse} from 'generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';

export class StatusApiStub extends StatusApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw StubImplementationRequired; });
  }

  getStatus(): Promise<StatusResponse> {
    return new Promise<StatusResponse>(resolve => {
      resolve({firecloudStatus: false, notebooksStatus: false});
    });
  }

}
