import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import Button from 'app/element/button';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';

const PageTitle = 'Concept Set Actions';

export default class ConceptsetActionsPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
        this.getCreateAnotherConceptSetButton(),
        this.getCreateDatasetButton(),
      ]);
      return true;
    } catch (e) {
      console.log(`ConceptsetActionsPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async clickCreateAnotherConceptSetButton(): Promise<void> {
    const button = await this.getCreateAnotherConceptSetButton();
    return button.clickAndWait();
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = await this.getCreateDatasetButton();
    await button.clickAndWait();
    const datasetBuildPage = new DatasetBuildPage(this.page);
    return datasetBuildPage.waitForLoad();
  }

  async getCreateAnotherConceptSetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: 'Create another Concept Set'});
  }

  async getCreateDatasetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: 'Create a Dataset'});
  }

}
