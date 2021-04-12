import { Page } from 'puppeteer';
import { config } from 'resources/workbench-config';
import Button from 'app/element/button';
import ClrIcon from 'app/element/clr-icon-link';
import Modal from './modal';

export default class CdrVersionUpgradeModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    const xpath = '//*[@data-test-id="cdr-version-upgrade-modal"]';
    await this.page.waitForXPath(xpath, { visible: true });
    return true;
  }

  getCancelButton(): ClrIcon {
    return ClrIcon.findByName(this.page, { iconShape: 'times' }, this);
  }

  getUpgradeButton(): Button {
    return Button.findByName(this.page, { normalizeSpace: `Try ${config.defaultCdrVersionName}` }, this);
  }
}
