import { StatusApi, StatusResponse } from 'generated/fetch';
export class StatusApiStub extends StatusApi {
  constructor() {
    super(undefined);
  }

  getStatus(): Promise<StatusResponse> {
    return new Promise<StatusResponse>((resolve) => {
      resolve({ firecloudStatus: false, notebooksStatus: false });
    });
  }
}
