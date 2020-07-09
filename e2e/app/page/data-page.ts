import DataResourceCard from 'app/component/data-resource-card';
import Dialog from 'app/component/dialog';
import EllipsisMenu from 'app/component/ellipsis-menu';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import {buildXPath} from 'app/xpath-builders';
import {EllipsisMenuAction, LinkText} from 'app/text-labels';
import AuthenticatedPage from 'app/page/authenticated-page';
import {ElementType} from 'app/xpath-options';
import {ElementHandle, Page, Response} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import {makeRandomName} from 'utils/str-utils';
import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
import CohortActionsPage from './cohort-actions-page';
import CohortBuildPage from './cohort-build-page';
import {Visits} from './cohort-criteria-modal';
import ConceptsetSearchPage from './conceptset-search-page';
import DatasetBuildPage from './dataset-build-page';

export enum TabLabelAlias {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All',
}

const PageTitle = 'Data Page';

export default class DataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        this.imgDiagramLoaded(),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`DataPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async imgDiagramLoaded(): Promise<ElementHandle[]> {
    return Promise.all<ElementHandle, ElementHandle>([
      this.page.waitForXPath('//img[@src="/assets/images/dataset-diagram.svg"]', {visible: true}),
      this.page.waitForXPath('//img[@src="/assets/images/cohort-diagram.svg"]', {visible: true}),
    ]);
  }

  async selectWorkspaceAction(action: EllipsisMenuAction): Promise<void> {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    return ellipsisMenu.clickAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param {TabLabelAlias} tabName
   * @param opts
   */
  async openTab(tabName: TabLabelAlias, opts: {waitPageChange?: boolean} = {}): Promise<void> {
    const {waitPageChange = true} = opts;
    const selector = buildXPath({name: tabName, type: ElementType.Tab});
    const tab = await this.page.waitForXPath(selector, {visible: true});
    if (waitPageChange) {
      await Promise.all<void, Response>([
        tab.click(),
        this.page.waitForNavigation({waitUntil: ['load', 'domcontentloaded', 'networkidle0']}),
      ]);
    } else {
      await tab.click();
    }
    return waitWhileLoading(this.page);
  }

  async getAddDatasetButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: TabLabelAlias.Datasets, iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: TabLabelAlias.Cohorts, iconShape: 'plus-circle'});
  }

  // Click Add Datasets button.
  async clickAddDatasetButton(waitForDatasetBuildPage: boolean = true): Promise<DatasetBuildPage> {
    const addDatasetButton = await this.getAddDatasetButton();
    await addDatasetButton.clickAndWait();

    const datasetPage = new DatasetBuildPage(this.page);
    // wait for Dataset Build page load and ready.
    if (waitForDatasetBuildPage) {
      await datasetPage.waitForLoad();
    }
    return datasetPage;
  }

  /**
   * Delete cohort by look up its name using Ellipsis menu.
   * @param {string} cohortName
   */
  async deleteCohort(cohortName: string): Promise<string> {
    const cohortCard = await DataResourceCard.findCard(this.page, cohortName);
    if (cohortCard == null) {
      throw new Error(`Failed to find Cohort: "${cohortName}".`);
    }
    const menu = cohortCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});
    const dialogContent = await (new CohortBuildPage(this.page)).deleteConfirmationDialog();
    console.log(`Deleted Cohort "${cohortName}"`);
    return dialogContent;
  }

  /**
   * Delete Dataset thru Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName
   */
  async deleteDataset(datasetName: string): Promise<string> {
    const datasetCard = await DataResourceCard.findCard(this.page, datasetName);
    if (datasetCard == null) {
      throw new Error(`Failed to find Dataset: "${datasetName}".`);
    }
    const menu = datasetCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const dialog = new Dialog(this.page);
    const dialogContentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {normalizeSpace: LinkText.DeleteDataset}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Deleted Dataset "${datasetName}"`);
    return dialogContentText;
  }

  /**
   * Rename Dataset thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName
   * @param {string} newDatasetName
   */
  async renameDataset(datasetName: string, newDatasetName: string): Promise<void> {
    const datasetCard = await DataResourceCard.findCard(this.page, datasetName);
    const menu = datasetCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.RenameDataset, {waitForNav: false});

    const dialog = new Dialog(this.page);

    const newNameTextbox = new Textbox(this.page, '//*[@id="new-name"]');
    await newNameTextbox.type(newDatasetName);

    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description:'}, dialog);
    await descriptionTextarea.type('Puppeteer automation rename dataset.');

    const renameButton = await Button.findByName(this.page, {normalizeSpace: LinkText.RenameDataset}, dialog);
    await Promise.all([
      renameButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    return waitWhileLoading(this.page);
  }

  /**
   * Delete ConceptSet thru Ellipsis menu located inside the Concept Set resource card.
   * @param {string} conceptsetName
   */
  async deleteConceptSet(conceptsetName: string): Promise<string> {
    const conceptSetCard = await DataResourceCard.findCard(this.page, conceptsetName);
    if (conceptSetCard == null) {
      throw new Error(`Failed to find Concept Set: "${conceptsetName}".`);
    }
    const menu = conceptSetCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const dialog = new Dialog(this.page);
    const dialogContentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {normalizeSpace: LinkText.DeleteConceptSet}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Deleted Concept Set "${conceptsetName}"`);
    return dialogContentText;
  }

  async renameCohort(cohortName: string, newCohortName: string): Promise<void> {
    const cohortCard = await DataResourceCard.findCard(this.page, cohortName);
    const menu = cohortCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Rename, {waitForNav: false});
    const dialog = new Dialog(this.page);
    await dialog.getContent();
    const newNameInput = new Textbox(this.page, `${dialog.getXpath()}//*[@id="new-name"]`);
    await newNameInput.type(newCohortName);
    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description:'}, dialog);
    await descriptionTextarea.type('Puppeteer automation rename cohort.');
    await dialog.clickButton(LinkText.Rename);
    await dialog.waitUntilDialogIsClosed();
    await waitWhileLoading(this.page);
    console.log(`Cohort "${cohortName}" renamed to "${newCohortName}"`);
  }

  async findCohortCard(cohortName?: string): Promise<DataResourceCard> {
    await this.openTab(TabLabelAlias.Data);
    await this.openTab(TabLabelAlias.Cohorts, {waitPageChange: false});
    if (cohortName === undefined) {
      // if cohort name isn't specified, find any existing cohort.
      return DataResourceCard.findAnyCard(this.page);
    } else {
      // find cohort matching name.
      return DataResourceCard.findCard(this.page, cohortName, 2000);
    }
  }

  /**
   * Create a simple Cohort from Out-Patient Visit criteria.
   * @param {string} cohortName New Cohort name.
   */
  async createCohort(cohortName?: string): Promise<DataResourceCard> {
    await this.getAddCohortsButton().then((butn) => butn.clickAndWait());
    // Land on Build Cohort page.
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const modal = await group1.includeVisits();
    await modal.addVisits([Visits.OutpatientVisit]);
    await modal.clickFinishButton();
    await waitWhileLoading(this.page);
    await cohortBuildPage.getTotalCount();
    const name = (cohortName === undefined) ? makeRandomName() : cohortName;
    await cohortBuildPage.saveCohortAs(name);
    await (new CohortActionsPage(this.page)).waitForLoad();
    return this.findCohortCard(name);
  }

  /**
   * Click Add Dataset button.
   * Click Add Concept Set button.
   * Click Domain card.
   * @param {Domain} domain
   */
  async openConceptSearch(domain: Domain): Promise<ConceptsetSearchPage> {
    // Click Add Datasets button.
    const datasetBuildPage = await this.clickAddDatasetButton();

    // Click Add Concept Sets button.
    const conceptSearchPage = await datasetBuildPage.clickAddConceptSetsButton();

    // Add Concept Set in domain.
    const procedures = await ConceptDomainCard.findDomainCard(this.page, domain);
    await procedures.clickSelectConceptButton();

    return conceptSearchPage;
  }


}
