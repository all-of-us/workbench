import {Page} from 'puppeteer';
import {waitUntilTitleMatch} from '../driver/waitFuncs';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import {SideNav} from './page-mixin/SideNav';

export const TAB_SELECTOR = {
  cohortsTab: '//*[@role="button"][(text()="Cohorts")]',
  dataSetsTab: '//*[@role="button"][(text()="Datasets")]',
  cohortReviewsTab: '//*[@role="button"][(text()="Cohort Reviews")]',
  conceptSetsTab: '//*[@role="button"][(text()="Concept Sets")]',
  showAllTab: '//*[@role="button"][(text()="Show All")]',
};

class Data extends AuthenticatedPage {

  public page: Page;
  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  public async waitUntilPageReady() {
    await waitUntilTitleMatch(this.puppeteerPage, 'Data Page');
    await this.puppeteerPage.waitForXPath(TAB_SELECTOR.showAllTab, {visible: true});
    await this.waitForSpinner();
  }

}

export const DataPage = SideNav(Data);
