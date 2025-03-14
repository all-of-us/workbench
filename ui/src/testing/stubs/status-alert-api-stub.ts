import {
  StatusAlert,
  StatusAlertApi,
  StatusAlertLocation,
} from 'generated/fetch';

export class StatusAlertApiStub extends StatusAlertApi {
  private alerts: StatusAlert[] = [];
  private nextId = 1;

  constructor(alertLocation?: StatusAlertLocation) {
    super(undefined);
    // Initialize with a default alert if location is provided
    if (alertLocation) {
      this.alerts.push({
        statusAlertId: this.nextId++,
        title: 'Stub Title',
        message: 'This is a stub message.',
        link: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        alertLocation,
      });
    }
  }

  public async getStatusAlerts(): Promise<StatusAlert[]> {
    return Promise.resolve(this.alerts);
  }

  public async postStatusAlert(alert: StatusAlert): Promise<StatusAlert> {
    const newAlert = {
      ...alert,
      statusAlertId: this.nextId++
    };
    this.alerts.push(newAlert);
    return Promise.resolve(newAlert);
  }

  public async deleteStatusAlert(id: number): Promise<void> {
    this.alerts = this.alerts.filter(alert => alert.statusAlertId !== id);
    return Promise.resolve();
  }

  // Helper method for tests to add multiple alerts at once
  public addAlerts(alerts: Omit<StatusAlert, 'statusAlertId'>[]): void {
    alerts.forEach(alert => {
      this.alerts.push({
        ...alert,
        statusAlertId: this.nextId++
      });
    });
  }

  // Helper method for tests to clear all alerts
  public clearAlerts(): void {
    this.alerts = [];
    this.nextId = 1;
  }
}
