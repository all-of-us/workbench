import { Page } from 'puppeteer';
import { PageUrl } from 'app/text-labels';
import AuthenticatedPage from 'app/page/authenticated-page';
import Button from 'app/element/button';

export const ButtonAlias = {
  Acknowledge: 'Acknowledge'
};

export default class PrivacyPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await this.page.waitForXPath('//h3[contains(text(), "Warning Notice")]', { visible: true });
    return true;
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPage({ url: PageUrl.Home });
    return this;
  }

  getAcknowledgeButtonLink(): Button {
    return Button.findByName(this.page, { name: ButtonAlias.Acknowledge });
  }

  async acknowledgePrivacyWarning(): Promise<void> {
    await this.getAcknowledgeButtonLink().clickAndWait();
  }
}
