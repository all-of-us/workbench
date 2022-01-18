import { Page } from 'puppeteer';
import DatasetBuildPage from './dataset-build-page';

export default class DatasetEditPage extends DatasetBuildPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    return super.isLoaded();
  }
}
