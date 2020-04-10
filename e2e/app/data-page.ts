import {Page} from 'puppeteer';
import Ellipsis from 'app/aou-elements/ellipsis-dropdown';
import AuthenticatedPage from 'app/authenticated-page';
import {workspaceAction} from 'util/enums';

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
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.page.waitForXPath(TAB_SELECTOR.showAllTab, {visible: true});
      return true;
    } catch (e) {
      return false;
    }
  }

  async selectFromEllipsisMenu(action: workspaceAction) {
    const ellipsisMenu = new Ellipsis(this.page, './/*[@data-test-id="workspace-menu-button"]');
    switch (action) {
    case workspaceAction.DELETE:
      await ellipsisMenu.delete();
      break;
    case workspaceAction.DUPLICATE:
      await ellipsisMenu.duplicate();
      break;
    case workspaceAction.SHARE:
      await ellipsisMenu.share();
      break;
    case workspaceAction.EDIT:
      await ellipsisMenu.edit();
      break;
    }
  }

}
