import { Page } from 'puppeteer';

export default class CookiePolicyPage {
  constructor(private readonly page: Page) {}

  async loaded(): Promise<void> {
    await this.page.waitForXPath('//h3[contains(text(), "Cookie Policy")]', { visible: true });
  }
}
