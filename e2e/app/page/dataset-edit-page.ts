import {Page} from 'puppeteer';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import DatasetBuildPage from './dataset-build-page';

const pageTitle = 'Edit Dataset';

export default class DatasetEditPage extends DatasetBuildPage {

   constructor(page: Page) {
      super(page);
   }

   async isLoaded(): Promise<boolean> {
      await Promise.all([
         waitForDocumentTitle(this.page, pageTitle),
         waitWhileLoading(this.page),
      ]);
      return true;
   }

}
