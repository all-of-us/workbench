import { Page } from 'puppeteer';
import StaticText from 'app/element/staticText';

export const DataTestIds = {
  AccessRenewal: 'access-renewal-notification'
};

export const Texts = {
  AccessExpired: 'Time for access renewal. Your access has expired.'
};

export async function waitForNotification(page: Page, dataTestId: string): Promise<StaticText> {
  return StaticText.findByName(page, { dataTestId });
}

export async function assertNoNotification(page: Page, dataTestId: string) {
  return StaticText.assertNotPresent(page, { dataTestId });
}

export async function getNotificationText(page: Page, dataTestId: string): Promise<string> {
  return (await waitForNotification(page, dataTestId)).getText();
}
