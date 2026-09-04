import {
  VwbSystemNotification,
  VwbSystemNotificationAdminApi,
  VwbSystemNotificationPriority,
  VwbSystemNotificationType,
} from 'generated/fetch';

export class VwbSystemNotificationAdminApiStub extends VwbSystemNotificationAdminApi {
  public notifications: VwbSystemNotification[];
  private nextId: number;

  constructor(notifications: VwbSystemNotification[] = []) {
    super(undefined);
    this.notifications = notifications;
    this.nextId = notifications.length + 1;
  }

  public async listVwbSystemNotifications(): Promise<VwbSystemNotification[]> {
    return Promise.resolve(this.notifications);
  }

  public async createVwbSystemNotification(
    notification: VwbSystemNotification
  ): Promise<VwbSystemNotification> {
    const created = {
      ...notification,
      id: `vwb-notification-uuid-${this.nextId++}`,
    };
    this.notifications.push(created);
    return Promise.resolve(created);
  }

  public async deleteVwbSystemNotification(id: string): Promise<void> {
    this.notifications = this.notifications.filter((n) => n.id !== id);
    return Promise.resolve();
  }
}

export const stubVwbSystemNotification = (
  overrides: Partial<VwbSystemNotification> = {}
): VwbSystemNotification => ({
  id: 'vwb-notification-uuid-1',
  title: 'VWB Stub Title',
  message: 'This is a stub Verily Workbench notification.',
  notificationType: VwbSystemNotificationType.PASSIVE,
  notificationPriority: VwbSystemNotificationPriority.INFO,
  startTimeEpochMillis: Date.now(),
  ...overrides,
});
