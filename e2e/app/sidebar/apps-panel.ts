import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import { waitForFn } from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';

const defaultXpath = '//*[@data-test-id="apps-panel"]';

export default class AppsPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.UserApps);
  }

  async pollForStatus(xPath: string, status: string) {
    const success = await waitForFn(
      async () => {
        await this.close();
        await this.open();
        const text = await BaseElement.asBaseElement(page, await page.waitForXPath(xPath)).getTextContent();
        return text.includes(`status: ${status}`);
      },
      10e3, // every 10 sec
      10 * 60e3 // with a 10 min timeout
    );
    expect(success).toBeTruthy();
    console.log(`Polling complete, status = ${status}`);
  }
}
