import Link from 'app/element/link';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForAttributeEquality} from 'utils/waits-utils';
import DataResourceCard from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import {EllipsisMenuAction, LinkText, ResourceCard} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';

export enum TabLabels {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All',
}

export default abstract class WorkspaceBase extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Click Data tab to open Data page.
   * @param opts
   */
  async openDataPage(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.Data, opts);
  }

  /**
   * Click Analysis tab to open Analysis page.
   * @param opts
   */
  async openAnalysisPage(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.Analysis, opts);
  }

  /**
   * Click About tab to open About page.
   * @param opts
   */
  async openAboutPage(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.About, opts);
  }

  /**
   * Click Datasets subtab in Data page.
   * @param opts
   */
  async openDatasetsSubtab(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.Datasets, opts);
  }

  /**
   * Click Cohorts subtab in Data page.
   * @param opts
   */
  async openCohortsSubtab(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.Cohorts, opts);
  }

  /**
   * Click Concept Sets subtab in Data page.
   * @param opts
   */
  async openConceptSetsSubtab(opts: {waitPageChange?: boolean} = {}): Promise<void> {
    return this.openTab(TabLabels.ConceptSets, opts);
  }

  /**
   * Click page tab.
   * @param {string} pageTabName Page tab name
   * @param opts
   */
  async openTab(pageTabName: TabLabels, opts: {waitPageChange?: boolean} = {}): Promise<void> {
    const { waitPageChange = true } = opts;
    const selector = buildXPath({name: pageTabName, type: ElementType.Tab});
    const tab = new Link(this.page, selector);
    waitPageChange ? await tab.clickAndWait() : await tab.click();
    await tab.dispose();
    return waitWhileLoading(this.page);
  }

  /**
   * Is tab currently open or selected?
   * @param {TabLabels} pageTabName Tab name.
   */
  async isOpen(pageTabName: TabLabels): Promise<boolean> {
    const selector = buildXPath({name: pageTabName, type: ElementType.Tab});
    return waitForAttributeEquality(this.page, {xpath: selector}, 'aria-selected', 'true');
  }

  /**
   * Delete Notebook, Concept Set, Dataset or Cohort thru Ellipsis menu located inside the data resource card.
   * @param {string} resourceName
   * @param {ResourceCard} resourceType
   */
  async deleteResource(resourceName: string, resourceType: ResourceCard): Promise<string[]> {
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
      case ResourceCard.Cohort:
        link = LinkText.DeleteCohort;
        break;
      case ResourceCard.ConceptSet:
        link = LinkText.DeleteConceptSet;
        break;
      case ResourceCard.Dataset:
        link = LinkText.DeleteDataset;
        break;
      case ResourceCard.Notebook:
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
