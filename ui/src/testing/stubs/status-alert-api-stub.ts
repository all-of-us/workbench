import {StatusAlert, StatusAlertApi} from 'generated/fetch';

export class StatusAlertApiStub extends StatusAlertApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public getStatusAlert(): Promise<StatusAlert> {
    const statusAlertStub = {
      statusAlertId: 1,
      title: 'lol',
      message: 'lol lol lol',
      link: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
    };
    return new Promise<StatusAlert>(resolve => resolve(statusAlertStub));
  }

}
