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

  async pollForStatus(xPath: string, status: string, timeout: number = 10 * 60e3): Promise<void> {
    const interval = 10e3; // every 10 sec
    const success = await waitForFn(
      async () => {
        const text = await BaseElement.asBaseElement(page, await page.waitForXPath(xPath)).getTextContent();
        return text.includes(`Status: ${status}`);
      },
      interval,
      timeout
    );
    console.log(success ? `Polling complete, status = ${status}` : `Polling timed out after ${interval / 1e3} seconds`);
    expect(success).toBeTruthy();
  }
}
