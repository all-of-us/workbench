import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from './authenticated-page';
import ConceptSetPage from './conceptset-page';
import ConceptSetSearchPage from './conceptset-search-page';
import DatasetBuildPage from './dataset-build-page';

const PageTitle = 'Concept Set Actions';

export default class ConceptSetActionsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await Promise.all([
      this.getCreateAnotherConceptSetButton().asElementHandle(),
      this.getCreateDatasetButton().asElementHandle()
    ]);
    return true;
  }

  async clickCreateAnotherConceptSetButton(): Promise<void> {
    const button = this.getCreateAnotherConceptSetButton();
    return button.clickAndWait();
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = this.getCreateDatasetButton();
    await button.clickAndWait();
    const datasetBuildPage = new DatasetBuildPage(this.page);
    return datasetBuildPage.waitForLoad();
  }

  getCreateAnotherConceptSetButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateAnotherConceptSet });
  }

  getCreateDatasetButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateDataset });
  }

  async openConceptSet(conceptName: string): Promise<ConceptSetPage> {
    const link = new Link(this.page, `//a[text()="${conceptName}"]`);
    await link.click();
    const conceptSetPage = new ConceptSetPage(this.page);
    await conceptSetPage.waitForLoad();
    return conceptSetPage;
  }

  /**
   * Click Create another Concept Set button.
   * Click Domain card.
   * @param {Domain} domain
   */
  async openConceptSearch(): Promise<ConceptSetSearchPage> {
    await this.clickCreateAnotherConceptSetButton();

    const conceptSearchPage = new ConceptSetSearchPage(this.page);
    await conceptSearchPage.waitForLoad();
    return conceptSearchPage;
  }
}
