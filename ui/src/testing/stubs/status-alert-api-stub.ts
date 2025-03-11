import {
  StatusAlert,
  StatusAlertApi,
  StatusAlertLocation,
} from 'generated/fetch';

export class StatusAlertApiStub extends StatusAlertApi {
  private alertLocationStub: StatusAlertLocation;
  constructor(alertLocation?: StatusAlertLocation) {
    super(undefined);
    this.alertLocationStub = alertLocation || StatusAlertLocation.AFTER_LOGIN;
  }

  public getStatusAlert(): Promise<StatusAlert> {
    const statusAlertStub = {
      statusAlertId: 1,
      title: 'Stub Title',
      message: 'This is a stub message.',
      link: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      alertLocation: this.alertLocationStub,
    };
    return new Promise<StatusAlert>((resolve) => resolve(statusAlertStub));
  }
}
