import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import DataResourceCard from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import Button from 'app/element/button';
import {EllipsisMenuAction, LinkText} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'View Notebooks';

export default class AnalysisPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (err) {
      console.log(`AnalysisPage isLoaded() encountered ${err}`);
      return false;
    }
  }

   /**
    * Delete notebook thru Ellipsis menu located inside the Notebook resource card.
    * @param {string} notebookName
    */
  async deleteNotebook(notebookName: string): Promise<string> {
    const resourceCard = await DataResourceCard.findCard(this.page, notebookName);
    const menu = resourceCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const modal = new Modal(this.page);
    const modalContentText = await modal.getContent();
    const deleteButton = await Button.findByName(this.page, {normalizeSpace: LinkText.DeleteNotebook}, modal);
    await Promise.all([
      deleteButton.click(),
      modal.waitUntilClose(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Deleted Notebook "${notebookName}"`);
    return modalContentText;
  }

}
