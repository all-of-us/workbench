import Link from 'app/element/link';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForAttributeEquality} from 'utils/waits-utils';
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


}
