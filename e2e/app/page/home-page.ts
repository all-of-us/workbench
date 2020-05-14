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

  // move to test-utils.ts
  async selfBypassWhenRequired() {
    const selfBypassXpath = '//*[@data-test-id="self-bypass"]';
    await Promise.race([
      this.page.waitForXPath(selfBypassXpath, {visible: true, timeout: 60000}),
      Link.forLabel(this.page, LABEL_ALIAS.SEE_ALL_WORKSPACES, {visible: true, timeout: 60000}),
    ]);
    // check to see if it is the Self-Bypass link
    const bypassLink = await this.page.$x(selfBypassXpath);
    if (bypassLink.length === 0) {
      return;
    }

    // Click Self-Bypass button to continue
    console.log('self-bypass button found');
    const selfBypass = await this.page.waitForXPath(`${selfBypassXpath}//div[@role="button"]`);
    await selfBypass.click();
    try {
      await this.waitUntilNoSpinner();
    } catch (timeouterr) {
      // wait more if 60 seconds wait time wasn't enough.
      await this.waitUntilNoSpinner(120000);
    }
    await this.waitForText('[data-test-id="self-bypass"]', 'Bypass action is complete. Reload the page to continue.', 60000);
    console.log('waitForText');
    await this.page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
    console.log('reload');
    await this.waitUntilNoSpinner();
    console.log('waitForNoSpinner');
    await Link.forLabel(this.page, LABEL_ALIAS.SEE_ALL_WORKSPACES);
    await takeScreenshot(this.page, 'selfBypassCallExit');
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        this.waitUntilTitleMatch(PAGE.TITLE),
        this.waitUntilNoSpinner(120000),
      ]);
      if (process.env.WORKBENCH_ENV === 'local') {
        console.log('WORKBENCH_ENV = ' + process.env.WORKBENCH_ENV);
        await this.selfBypassWhenRequired();
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
