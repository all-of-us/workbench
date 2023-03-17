import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';

const defaultXpath = '//*[@data-test-id="apps-panel"]';

export default class AppsPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.UserApps);
  }
}
