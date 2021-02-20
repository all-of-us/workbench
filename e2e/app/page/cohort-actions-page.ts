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
    const createCohortButton = this.getCreateAnotherCohortButton();
    const createReviewButton = this.getCreateReviewSetsButton();
    const createDatasetButton = this.getCreateDatasetButton();
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await Promise.all([
      createCohortButton.asElementHandle(),
      createReviewButton.asElementHandle(),
      createDatasetButton.asElementHandle()
    ]);
    return true;
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = this.getCreateDatasetButton();
    await button.clickAndWait();
    return new DatasetBuildPage(this.page).waitForLoad();
  }

  getCreateAnotherCohortButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateAnotherCohort });
  }

  getCreateReviewSetsButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateReviewSets });
  }

  getCreateDatasetButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateDataset });
  }
}
