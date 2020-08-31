import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import DataResourceCard, {CardType} from '../component/data-resource-card';
import Modal from '../component/modal';
import Link from '../element/link';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {EllipsisMenuAction, LinkText, TabLabelAlias} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';


export default abstract class WorkspaceBase extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param {TabLabelAlias} tabName
   * @param opts
   */
  async openTab(tabName: TabLabelAlias, opts: {waitPageChange?: boolean} = {}): Promise<void> {
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

    const menu = card.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const modal = new Modal(this.page);
    const modalTextContent = await modal.getTextContent();
    await modal.clickButton(LinkText.DeleteConceptSet, {waitForClose: true});
    await waitWhileLoading(this.page);

    console.log(`Deleted Concept Set "${resourceName}"`);
    return modalTextContent;
  }


}
