import { Page } from 'puppeteer';
import DataResourceCard from 'app/component/data-resource-card';
import Link from 'app/element/link';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import { waitForAttributeEquality, waitWhileLoading } from 'utils/waits-utils';
import SnowmanMenu from 'app/component/snowman-menu';
import { getPropValue } from 'utils/element-utils';
import Modal from 'app/modal/modal';
import AuthenticatedPage from './authenticated-page';
import BaseElement from 'app/element/base-element';
import ShareModal from 'app/modal/share-modal';

export const UseFreeCredits = 'Use All of Us free credits';

export enum TabLabels {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All'
}

export default abstract class WorkspaceBase extends AuthenticatedPage {
  protected constructor(page: Page) {
    super(page);
  }

  /**
   * Click Data tab to open Data page.
   * @param opts
   */
  async openDataPage(opts: { waitPageChange?: boolean } = {}): Promise<void> {
    return this.openTab(TabLabels.Data, opts);
  }

  /**
   * Click Analysis tab to open Analysis page.
   * @param opts
   */
  async openAnalysisPage(opts: { waitPageChange?: boolean } = {}): Promise<void> {
    return this.openTab(TabLabels.Analysis, opts);
  }

  /**
   * Click About tab to open About page.
   * @param opts
   */
  async openAboutPage(opts: { waitPageChange?: boolean } = {}): Promise<void> {
    return this.openTab(TabLabels.About, opts);
  }

  /**
   * Click Datasets subtab in Data page.
   * @param opts
   */
  async openDatasetsSubtab(): Promise<void> {
    return this.openDataPage().then(() => this.openTab(TabLabels.Datasets, { waitPageChange: false }));
  }

  /**
   * Click Cohorts subtab in Data page.
   * @param opts
   */
  async openCohortsSubtab(): Promise<void> {
    return this.openDataPage().then(() => this.openTab(TabLabels.Cohorts, { waitPageChange: false }));
  }

  /**
   * Click Cohorts Reviews subtab in Data page.
   * @param opts
   */
  async openCohortReviewsSubtab(): Promise<void> {
    return this.openDataPage().then(() => this.openTab(TabLabels.CohortReviews, { waitPageChange: false }));
  }

  /**
   * Click Concept Sets subtab in Data page.
   * @param opts
   */
  async openConceptSetsSubtab(): Promise<void> {
    return this.openDataPage().then(() => this.openTab(TabLabels.ConceptSets, { waitPageChange: false }));
  }

  /**
   * Click page tab.
   * @param {string} pageTabName Page tab name
   * @param opts
   */
  async openTab(pageTabName: TabLabels, opts: { waitPageChange?: boolean } = {}): Promise<void> {
    const { waitPageChange = true } = opts;
    const selector = buildXPath({ name: pageTabName, type: ElementType.Tab });
    const tab = new Link(this.page, selector);
    const isSelected = await getPropValue<boolean>(await tab.asElementHandle(), 'ariaSelected');
    if (isSelected === true) {
      return; // Tab is already open.
    }
    waitPageChange ? await tab.clickAndWait() : await tab.click();
    await tab.dispose();
    return waitWhileLoading(this.page);
  }

  /**
   * Is tab currently open or selected?
   * @param {TabLabels} pageTabName Tab name.
   */
  async isOpen(pageTabName: TabLabels): Promise<boolean> {
    const selector = buildXPath({ name: pageTabName, type: ElementType.Tab });
    return waitForAttributeEquality(this.page, { xpath: selector }, 'aria-selected', 'true');
  }

  /**
   * Delete Notebook, Concept Set, Dataset or Cohort, Cohort Review via snowman menu located inside the data resource card.
   * @param {string} resourceName
   * @param {ResourceCard} resourceType
   */
  async deleteResource(resourceName: string, resourceType: ResourceCard): Promise<string[]> {
    const resourceCard = new DataResourceCard(this.page);
    const card = await resourceCard.findCard(resourceName, resourceType);
    if (!card) {
      throw new Error(`Failed to find ${resourceType} card "${resourceName}"`);
    }

    await card.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });

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
      case ResourceCard.CohortReview:
        link = MenuOption.Delete;
        break;
    }

    await modal.clickButton(link, { waitForClose: true });
    await waitWhileLoading(this.page);

    console.log(`Deleted ${resourceType} "${resourceName}"`);
    return modalTextContent;
  }

  /**
   * Rename Notebook, Concept Set, Dataset or Cohorts thru the snowman menu located inside the Dataset Resource card.
   * @param {string} resourceName
   * @param {string} newResourceName
   */
  async renameResource(resourceName: string, newResourceName: string, resourceType: ResourceCard): Promise<string[]> {
    // Find the Data resource card that match the resource name.
    const resourceCard = new DataResourceCard(this.page);
    const card = await resourceCard.findCard(resourceName, resourceType);
    if (!card) {
      throw new Error(`Failed to find ${resourceType} card "${resourceName}"`);
    }

    let option: MenuOption;
    switch (resourceType) {
      case ResourceCard.Dataset:
        option = MenuOption.RenameDataset;
        break;
      default:
        option = MenuOption.Rename;
        break;
    }
    await card.selectSnowmanMenu(option, { waitForNav: false });

    const modal = new Modal(this.page);
    await modal.waitForLoad();

    const modalTextContents = await modal.getTextContent();

    // Type new name.
    const newNameTextbox = new Textbox(this.page, `${modal.getXpath()}//*[@id="new-name"]`);
    await newNameTextbox.type(newResourceName);

    // Type description. Notebook rename modal does not have Description textarea.
    if (resourceType !== ResourceCard.Notebook) {
      const descriptionTextarea = Textarea.findByName(this.page, { containsText: 'Description:' }, modal);
      await descriptionTextarea.type(`Puppeteer automation test. Rename ${resourceName}.`);
    }

    let buttonLink;
    switch (resourceType) {
      case ResourceCard.Cohort:
        buttonLink = LinkText.RenameCohort;
        break;
      case ResourceCard.ConceptSet:
        buttonLink = LinkText.RenameConceptSet;
        break;
      case ResourceCard.Dataset:
        buttonLink = LinkText.RenameDataset;
        break;
      case ResourceCard.Notebook:
        buttonLink = LinkText.RenameNotebook;
        break;
      case ResourceCard.CohortReview:
        buttonLink = LinkText.RenameCohortReview;
        break;
    }

    await modal.clickButton(buttonLink, { waitForClose: true });
    await waitWhileLoading(this.page);
    console.log(`Renamed ${resourceType} "${resourceName}" to "${newResourceName}"`);
    return modalTextContents;
  }

  /**
   * Select Workspace action snowman menu option.
   * @param {MenuOption} option
   * @param opts
   */
  async selectWorkspaceAction(option: MenuOption, opts?: { waitForNav: false }): Promise<void> {
    const iconXpath = './/*[@data-test-id="workspace-menu-button"]';
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
    const snowmanMenu = new SnowmanMenu(this.page);
    return snowmanMenu.select(option, opts);
  }

  /**
   * Delete workspace via Workspace Actions snowman menu "Delete" option.
   */
  async deleteWorkspace(): Promise<string[]> {
    await this.selectWorkspaceAction(MenuOption.Delete, { waitForNav: false });
    // Handle Delete Confirmation modal
    return this.dismissDeleteWorkspaceModal();
  }

  /**
   * Dismss Delete Workspace Confirmation modal by click a button.
   * @return {LinkText} clickButtonText Button to click.
   */
  async dismissDeleteWorkspaceModal(clickButtonText: LinkText = LinkText.DeleteWorkspace): Promise<string[]> {
    const modal = new Modal(this.page);
    const textBox = modal.waitForTextbox('type DELETE to confirm');
    await textBox.type('delete');
    await modal.clickButton(clickButtonText, { waitForClose: true });
    await waitWhileLoading(this.page);
    return undefined;
  }

  /**
   * Edit workspace via Workspace Actions snowman menu "Edit" option.
   */
  async editWorkspace(): Promise<void> {
    await this.selectWorkspaceAction(MenuOption.Edit);
  }

  async getCdrVersion(): Promise<string> {
    const xpath = '//*[@data-test-id="cdr-version"]';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    return element.getTextContent();
  }

  async getNewCdrVersionFlag(): Promise<BaseElement> {
    const xpath = '//*[@data-test-id="new-version-flag"]';
    return BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
  }

  /**
   * Share workspace via Workspace Actions snowman menu "Share" option.
   */
  async shareWorkspace(): Promise<ShareModal> {
    await this.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    const modal = new ShareModal(this.page);
    await modal.waitForLoad();
    return modal;
  }
}
