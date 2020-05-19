import {Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import AuthenticatedPage from 'app/page/authenticated-page';
import {WorkspaceAction, PageTab} from 'app/page-identifiers';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/wait-utils';


export const TAB_SELECTOR = {
  cohortsTab: '//*[@role="button"][(text()="Cohorts")]',
  dataSetsTab: '//*[@role="button"][(text()="Datasets")]',
  cohortReviewsTab: '//*[@role="button"][(text()="Cohort Reviews")]',
  conceptSetsTab: '//*[@role="button"][(text()="Concept Sets")]',
  showAllTab: '//*[@role="button"][(text()="Show All")]',
};

export const PAGE = {
  TITLE: 'Data Page',
};

export default class DataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.puppeteerPage, PAGE.TITLE),
        this.puppeteerPage.waitForXPath(TAB_SELECTOR.showAllTab, {visible: true}),
        waitWhileLoading(this.puppeteerPage),
      ]);
      return true;
    } catch (err) {
      console.log(`DataPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async selectWorkspaceAction(action: WorkspaceAction) {
    const ellipsisMenu = new EllipsisMenu(this.puppeteerPage, './/*[@data-test-id="workspace-menu-button"]');
    await ellipsisMenu.selectAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param tabName
   */
  async selectTab(tabName: PageTab): Promise<void> {
    const selector = '//*[@aria-selected and @role="button"]';
    await this.puppeteerPage.waitForXPath(selector, {visible: true});
    const tabs = await this.puppeteerPage.$x(selector);
    for (const tab of tabs) {
      const contentProp = await tab.getProperty('textContent');
      if (await contentProp.jsonValue() === tabName) {
        return tab.click();
      }
      await tab.dispose();
    }
    throw new Error(`Failed to find page tab with name ${tabName}`);
  }

}
