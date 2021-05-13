import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';
import Link from 'app/element/link';
import CohortBuildPage from './cohort-build-page';
import WorkspaceDataPage from './workspace-data-page';

const PageTitle = 'Cohort Actions';

export default class CohortActionsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await Promise.all([
      this.getCreateAnotherCohortButton().asElementHandle(),
      this.getCreateReviewSetsButton().asElementHandle(),
      this.getCreateDatasetButton().asElementHandle()
    ]);
    return true;
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    await this.getCreateDatasetButton().clickAndWait();
    return new DatasetBuildPage(this.page).waitForLoad();
  }

  async clickCreateAnotherCohortButton(): Promise<CohortBuildPage> {
    await this.getCreateAnotherCohortButton().clickAndWait();
    return new CohortBuildPage(this.page).waitForLoad();
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

  getCohortLink(): Link {
    const xpath =
      '//*[starts-with(text(), "The cohort") and ' +
      'substring(text(), string-length(text()) - string-length("has been saved.") +1)]/a';
    return new Link(this.page, xpath);
  }

  async clickCohortName(): Promise<CohortBuildPage> {
    await this.getCohortLink().clickAndWait();
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    await cohortBuildPage.getTotalCount();
    await waitWhileLoading(this.page);
    return cohortBuildPage;
  }

  // A helper function to avoid clutter tests.
  async deleteCohort(): Promise<void> {
    await this.waitForLoad();
    const link = await this.getCohortLink();
    const cohortName = await link.getProperty<string>('textContent');
    await link.clickAndWait();

    // Delete cohort in Cohort Build page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();
    await cohortBuildPage.getTotalCount();

    const modalText = await cohortBuildPage.deleteCohort();
    // Verify Delete dialog content text
    expect(modalText).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

    // Verify Cohort card is gone after delete.
    expect(await new WorkspaceDataPage(page).findCohortCard(cohortName)).toBeFalsy();
  }
}
