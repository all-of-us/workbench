import {StatusApi, StatusResponse} from 'generated/fetch';

export class StatusApiStub extends StatusApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getStatus(): Promise<StatusResponse> {
    return new Promise<StatusResponse>(resolve => {
      resolve({firecloudStatus: false, notebooksStatus: false});
    });
  }

}
