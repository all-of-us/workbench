import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';
import Link from 'app/element/link';
import CohortBuildPage from './cohort-build-page';
import WorkspaceDataPage from './workspace-data-page';
import { getPropValue } from 'utils/element-utils';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceAboutPage from './workspace-about-page';
import { openTab } from 'utils/test-utils';
import { Tabs } from './workspace-base';

const PageTitle = 'Cohort Actions';

export default class CohortActionsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await Promise.all([
      this.getCreateAnotherCohortButton().asElementHandle(),
      this.getCreateReviewSetsButton().asElementHandle(),
      this.getCreateDatasetButton().asElementHandle()
    ]);
    await waitWhileLoading(this.page);
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

  async getCohortName(): Promise<string> {
    const link = this.getCohortLink();
    return getPropValue<string>(await link.asElementHandle(), 'textContent');
  }

  async clickCohortName(): Promise<CohortBuildPage> {
    await this.getCohortLink().clickAndWait();
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    await waitWhileLoading(this.page);
    return cohortBuildPage;
  }

  async clickDataTab(): Promise<WorkspaceDataPage> {
    const dataPage = new WorkspaceDataPage(this.page);
    await openTab(this.page, Tabs.Data, dataPage);
    return dataPage;
  }

  async clickAnalysisTab(): Promise<WorkspaceAnalysisPage> {
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await openTab(this.page, Tabs.Analysis, analysisPage);
    return analysisPage;
  }

  async clickAboutTab(): Promise<WorkspaceAboutPage> {
    const aboutPage = new WorkspaceAboutPage(this.page);
    await openTab(this.page, Tabs.About, aboutPage);
    return aboutPage;
  }

  // A helper function to avoid clutter tests.
  async deleteCohort(): Promise<void> {
    await this.waitForLoad();
    const link = this.getCohortLink();
    const cohortName = await link.getProperty<string>('textContent');
    await link.clickAndWait();

    // Delete cohort in Cohort Build page.
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();

    const modalText = await cohortBuildPage.deleteCohort();
    // Verify Delete dialog content text
    expect(modalText).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

    // Verify Cohort card is gone after delete.
    expect(await new WorkspaceDataPage(this.page).findCohortCard(cohortName)).toBeFalsy();
  }
}
