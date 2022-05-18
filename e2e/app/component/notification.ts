import { Page } from 'puppeteer';
import StaticText from 'app/element/staticText';

export const DataTestIds = {
  AccessRenewal: 'access-renewal-notification'
};

export const Texts = {
  AccessExpired: 'Time for access renewal. Your access has expired.'
};

export async function getNotificationText(page: Page, dataTestId: string): Promise<string> {
  const element = StaticText.findByName(page, { dataTestId });
  return element?.getText();
}
