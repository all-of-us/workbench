import { StatusAlert, StatusAlertApi } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class StatusAlertApiStub extends StatusAlertApi {
  constructor() {
    super(undefined);
  }

  public getStatusAlert(): Promise<StatusAlert> {
    const statusAlertStub = {
      statusAlertId: 1,
      title: 'lol',
      message: 'lol lol lol',
      link: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
    };
    return new Promise<StatusAlert>((resolve) => resolve(statusAlertStub));
  }
}
