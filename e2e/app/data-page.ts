import {Page} from 'puppeteer';
import AuthenticatedPage from './authenticated-page';

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
    await this.waitUntilTitleMatch(PAGE.TITLE);
    await this.page.waitForXPath(TAB_SELECTOR.showAllTab, {visible: true});
    return true;
  }

  async waitForReady(): Promise<this> {
    super.waitForReady();
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

}
