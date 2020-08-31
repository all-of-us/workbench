import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import Link from 'app/element/link';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {EllipsisMenuAction, LinkText, TabLabel} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';


export default abstract class WorkspaceBase extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param {TabLabel} tabName
   * @param opts
   */
  async openTab(tabName: TabLabel, opts: {waitPageChange?: boolean} = {}): Promise<void> {
    const { waitPageChange = true } = opts;
    const selector = buildXPath({name: tabName, type: ElementType.Tab});
    const tab = new Link(this.page, selector);
    waitPageChange ? await tab.clickAndWait() : await tab.click();
    await tab.dispose();
    return waitWhileLoading(this.page);
  }

  /**
   * Delete ConceptSet, Dataset or Cohort thru Ellipsis menu located inside the resource card.
   * @param {string} resourceName
   */
  async deleteResource(resourceName: string, resourceType: CardType): Promise<string[]> {
    const resourceCard = new DataResourceCard(this.page);
    const card = await resourceCard.findCard(resourceName, resourceType);
    if (!card) {
      throw new Error(`Failed to find ${resourceType} card "${resourceName}"`);
    }

    await card.clickEllipsisAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const modalTextContent = await modal.getTextContent();

    let link;
    switch (resourceType) {
    case CardType.Cohort:
      link = LinkText.DeleteCohort;
      break;
    case CardType.ConceptSet:
      link = LinkText.DeleteConceptSet;
      break;
    case CardType.Dataset:
      link = LinkText.DeleteDataset;
      break;
    case CardType.Notebook:
      link = LinkText.DeleteNotebook;
      break;
    default:
      throw new Error(`Case ${resourceType} handling is not defined.`);
    }

    await modal.clickButton(link, {waitForClose: true});
    await waitWhileLoading(this.page);

    console.log(`Deleted ${resourceType} card "${resourceName}"`);
    await this.waitForLoad();
    return modalTextContent;
  }


}
