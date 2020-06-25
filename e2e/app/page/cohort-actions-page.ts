import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import Button from 'app/element/button';
import {LinkText} from 'app/page-identifiers';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';

const PageTitle = 'Cohort Actions';

export default class CohortActionsPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
        this.getCreateAnotherCohortButton(),
        this.getCreateReviewSetsButton(),
        this.getCreateDatasetButton(),
      ]);
      return true;
    } catch (e) {
      console.log(`CohortActionsPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = await this.getCreateDatasetButton();
    await button.clickAndWait();
    return (new DatasetBuildPage(this.page)).waitForLoad();
  }

  async getCreateAnotherCohortButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateAnotherCohort});
  }

  async getCreateReviewSetsButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateReviewSets});
  }

  async getCreateDatasetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateDataset});
  }

}
