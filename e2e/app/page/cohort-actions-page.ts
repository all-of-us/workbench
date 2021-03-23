import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';

const PageTitle = 'Cohort Actions';

export default class CohortActionsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await Promise.all([
      this.getCreateAnotherCohortButton().then((elemt) => elemt.asElementHandle()),
      this.getCreateReviewSetsButton().then((elemt) => elemt.asElementHandle()),
      this.getCreateDatasetButton().then((elemt) => elemt.asElementHandle())
    ]);
    return true;
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = await this.getCreateDatasetButton();
    await button.clickAndWait();
    return new DatasetBuildPage(this.page).waitForLoad();
  }

  async getCreateAnotherCohortButton(): Promise<Button> {
    return Button.findByName(this.page, { name: LinkText.CreateAnotherCohort });
  }

  async getCreateReviewSetsButton(): Promise<Button> {
    return Button.findByName(this.page, { name: LinkText.CreateReviewSets });
  }

  async getCreateDatasetButton(): Promise<Button> {
    return Button.findByName(this.page, { name: LinkText.CreateDataset });
  }
}
