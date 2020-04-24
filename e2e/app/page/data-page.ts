import {Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import AuthenticatedPage from 'app/page/authenticated-page';
import {WorkspaceAction, PageTab} from 'app/page-identifiers';


export const TAB_SELECTOR = {
  cohortsTab: '//*[@role="button"][(text()="Cohorts")]',
  dataSetsTab: '//*[@role="button"][(text()="Datasets")]',
  cohortReviewsTab: '//*[@role="button"][(text()="Cohort Reviews")]',
  conceptSetsTab: '//*[@role="button"][(text()="Concept Sets")]',
  showAllTab: '//*[@role="button"][(text()="Show All")]',
};


export default class DataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch('Data Page');
      await this.page.waitForXPath(TAB_SELECTOR.showAllTab, {visible: true});
      return true;
    } catch (e) {
      return false;
    }
  }

  async selectWorkspaceAction(action: WorkspaceAction) {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    await ellipsisMenu.selectAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param tabName
   */
  async selectTab(tabName: PageTab): Promise<void> {
    const selector = '//*[@aria-selected and @role="button"]';
    await this.page.waitForXPath(selector, {visible: true});
    const tabs = await this.page.$x(selector);
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
