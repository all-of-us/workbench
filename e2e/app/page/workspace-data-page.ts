import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
import DataResourceCard from 'app/component/data-resource-card';
import EllipsisMenu from 'app/component/ellipsis-menu';
import Modal from 'app/component/modal';
import ClrIconLink from 'app/element/clr-icon-link';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import {EllipsisMenuAction, Language, LinkText, ResourceCard} from 'app/text-labels';
import {ElementHandle, Page} from 'puppeteer';
import {makeRandomName} from 'utils/str-utils';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import CohortActionsPage from './cohort-actions-page';
import CohortBuildPage from './cohort-build-page';
import {Visits} from './cohort-search-page';
import ConceptsetSearchPage from './conceptset-search-page';
import DatasetBuildPage from './dataset-build-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceBase from './workspace-base';

const PageTitle = 'Data Page';

export default class WorkspaceDataPage extends WorkspaceBase {

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

  async getAddDatasetButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: 'Datasets', iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: 'Cohorts', iconShape: 'plus-circle'});
  }

  // Click Add Datasets button.
  async clickAddDatasetButton(): Promise<DatasetBuildPage> {
    const addDatasetButton = await this.getAddDatasetButton();
    await addDatasetButton.clickAndWait();
    await waitWhileLoading(this.page);

    // wait for Dataset Build page load and ready.
    const datasetPage = new DatasetBuildPage(this.page);
    await datasetPage.waitForLoad();
    return datasetPage;
  }

  /**
   * Export Dataset to notebook thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName Dataset name.
   * @param {string} notebookName Notebook name.
   */
  async exportToNotebook(datasetName: string, notebookName: string): Promise<void> {
    const resourceCard = new DataResourceCard(this.page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.clickEllipsisAction(EllipsisMenuAction.exportToNotebook, {waitForNav: false});
    console.log(`Exported Dataset "${datasetName}" to notebook "${notebookName}"`);
  }

  /**
   * Rename Dataset thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName
   * @param {string} newDatasetName
   */
  async renameDataset(datasetName: string, newDatasetName: string): Promise<void> {
    const datasetCard = await DataResourceCard.findCard(this.page, datasetName);
    await datasetCard.clickEllipsisAction(EllipsisMenuAction.RenameDataset, {waitForNav: false});

    const modal = new Modal(this.page);

    const newNameTextbox = new Textbox(this.page, `${modal.getXpath()}//*[@id="new-name"]`);
    await newNameTextbox.type(newDatasetName);

    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description:'}, modal);
    await descriptionTextarea.type('Puppeteer automation rename dataset.');

    await modal.clickButton(LinkText.RenameDataset, {waitForClose: true});
    await waitWhileLoading(this.page);

    console.log(`Renamed Dataset "${datasetName}" to "${newDatasetName}"`);
  }

  async renameCohort(cohortName: string, newCohortName: string): Promise<void> {
    const cohortCard = await DataResourceCard.findCard(this.page, cohortName);
    await cohortCard.clickEllipsisAction(EllipsisMenuAction.Rename, {waitForNav: false});
    const modal = new Modal(this.page);
    await modal.getTextContent();
    const newNameInput = new Textbox(this.page, `${modal.getXpath()}//*[@id="new-name"]`);
    await newNameInput.type(newCohortName);
    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description:'}, modal);
    await descriptionTextarea.type('Puppeteer automation rename cohort.');
    await modal.clickButton(LinkText.Rename, {waitForClose: true});
    await waitWhileLoading(this.page);
    console.log(`Cohort "${cohortName}" renamed to "${newCohortName}"`);
  }

  async findCohortCard(cohortName?: string): Promise<DataResourceCard> {
    await this.openDataPage();
    await this.openCohortsSubtab({waitPageChange: false});
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
    const searchPage = await group1.includeVisits();
    await searchPage.addVisits([Visits.OutpatientVisit]);
    // Open selection list and click Save Criteria button
    await searchPage.viewAndSaveCriteria();
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

  /**
   * Create a new empty notebook. Wait for Notebook server start.
   * - Open Analysis tab.
   * - Click "Create a New Notebook" link.
   * - Fill out New Notebook modal.
   * @param notebookName The notebook name.
   * @param {Language} lang The notebook language.
   */
  async createNotebook(notebookName: string, lang: Language = Language.Python): Promise<NotebookPage> {
    await this.openAnalysisPage();
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    return analysisPage.createNotebook(notebookName, lang);
  }

}
