import {ElementHandle, Page} from 'puppeteer';
import {PageUrl} from 'app/page-identifiers';
import Link from 'app/element/link';
import {findIcon} from 'app/element/xpath-finder';
import AuthenticatedPage from 'app/page/authenticated-page';
import {takeScreenshot} from '../../utils/save-file-utils';

export const PAGE = {
  TITLE: 'Homepage',
  HEADER: 'Workspaces',
};

export const LABEL_ALIAS = {
  SEE_ALL_WORKSPACES: 'See all Workspaces',
  CREATE_NEW_WORKSPACE: 'Workspaces',
};


export default class HomePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async selfBypass() {
    try {
      // Handle Self-Bypass if found
      await this.page.waitForXPath('//*[@data-test-id="self-bypass"]', {visible: true, timeout: 10000});
      console.log('self-bypass button found');
      const selfBypass = await this.page.waitForXPath('//*[@data-test-id="self-bypass"]//div[@role="button"]');
      await takeScreenshot(this.page, 'BeforeClickButton');
      await selfBypass.click();
      await this.page.waitFor(2000);
      await takeScreenshot(this.page, 'AfterClickedButton');
      await this.waitUntilNoSpinner(120000);
      await this.page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
      await this.waitUntilNoSpinner(120000);
    } catch (e) {
      // Do nothing if Self-Bypass is not found.
      await takeScreenshot(this.page, 'SelfBypassButtonNotFound');
    }
    await takeScreenshot(this.page, 'Outside');
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        this.waitUntilTitleMatch(PAGE.TITLE),
        this.waitUntilNoSpinner(180000),
      ]);
      if (process.env.WORKBENCH_ENV === 'local') {
        await this.selfBypass();
      }
      await Promise.all([
        Link.forLabel(this.page, LABEL_ALIAS.SEE_ALL_WORKSPACES),
        this.waitForTextExists(PAGE.HEADER)
      ]);
      await takeScreenshot(this.page, 'HomePageIsLoaded');
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findIcon(this.page, {text: LABEL_ALIAS.CREATE_NEW_WORKSPACE}, 'plus-circle');
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.HOME);
    return this;
  }

}
