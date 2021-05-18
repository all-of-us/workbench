import ConceptDomainCard, { Domain } from 'app/component/concept-domain-card';
import Link from 'app/element/link';
import DataResourceCard from 'app/component/data-resource-card';
import ClrIconLink from 'app/element/clr-icon-link';
import { Language, MenuOption, ResourceCard } from 'app/text-labels';
import { ElementHandle, Page } from 'puppeteer';
import { makeRandomName } from 'utils/str-utils';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import CohortActionsPage from './cohort-actions-page';
import CohortBuildPage from './cohort-build-page';
import DatasetBuildPage from './dataset-build-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceBase from './workspace-base';
import ConceptSetSearchPage from './conceptset-search-page';
import { SaveOption } from 'app/modal/conceptset-save-modal';
import ConceptSetActionsPage from './conceptset-actions-page';
import { Visits } from './cohort-participants-group';
import CriteriaSearchPage from './criteria-search-page';

const PageTitle = 'Data Page';

export default class WorkspaceDataPage extends WorkspaceBase {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await this.imgDiagramLoaded();
    return true;
  }

  async imgDiagramLoaded(): Promise<ElementHandle[]> {
    return Promise.all<ElementHandle, ElementHandle>([
      this.page.waitForXPath('//img[@src="/assets/images/dataset-diagram.svg"]', { visible: true }),
      this.page.waitForXPath('//img[@src="/assets/images/cohort-diagram.svg"]', { visible: true })
    ]);
  }

  getAddDatasetButton(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: 'Datasets', iconShape: 'plus-circle' });
  }

  getAddCohortsButton(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: 'Cohorts', iconShape: 'plus-circle' });
  }

  // Click Add Datasets button.
  async clickAddDatasetButton(): Promise<DatasetBuildPage> {
    const addDatasetButton = this.getAddDatasetButton();
    await addDatasetButton.clickAndWait();
    await waitWhileLoading(this.page);

    // wait for Dataset Build page load and ready.
    const datasetPage = new DatasetBuildPage(this.page);
    await datasetPage.waitForLoad();
    return datasetPage;
  }

  async clickAddCohortsButton(): Promise<CohortBuildPage> {
    const addCohortsButton = this.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    return cohortBuildPage;
  }

  /**
   * Export Dataset to notebook thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName Dataset name.
   * @param {string} notebookName Notebook name.
   */
  async exportToNotebook(datasetName: string, notebookName: string): Promise<void> {
    const resourceCard = new DataResourceCard(this.page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { waitForNav: false });
    console.log(`Exported Dataset "${datasetName}" to notebook "${notebookName}"`);
  }

  async findCohortCard(cohortName?: string): Promise<DataResourceCard> {
    await this.openCohortsSubtab();
    if (cohortName) {
      // find Concept Set that match specified name.
      return new DataResourceCard(this.page).findCard(cohortName, ResourceCard.Cohort);
    }
    // if Concept Sets name isn't specified, find an existing Concept Sets.
    return new DataResourceCard(this.page).findAnyCard(ResourceCard.Cohort);
  }

  /**
   * Create a simple Cohort from Out-Patient Visit criteria.
   * @param {string} cohortName New Cohort name.
   */
  async createCohort(cohortName?: string): Promise<DataResourceCard> {
    const cohortBuildPage = await this.clickAddCohortsButton();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeVisits([Visits.OutpatientVisit]);
    await cohortBuildPage.getTotalCount();
    const name = cohortName === undefined ? makeRandomName() : cohortName;
    await cohortBuildPage.createCohort(name);
    await new CohortActionsPage(this.page).waitForLoad();
    const cohortCard = this.findCohortCard(name);
    console.log(`Created Cohort "${name}" from Outpatient Visit`);
    return cohortCard;
  }

  async createConceptSets(): Promise<DataResourceCard> {
    const { conceptSearchPage, criteriaSearch } = await this.openConceptSetSearch(Domain.Procedures);

    // Search Procedure name.
    const procedureName = 'Radiologic examination';
    await criteriaSearch.searchCriteria(procedureName);

    // Select first row.
    await criteriaSearch.resultsTableSelectRow(1, 1);

    await conceptSearchPage.reviewAndSaveConceptSet();
    const conceptName = await conceptSearchPage.saveConceptSet(SaveOption.CreateNewSet);

    // Open Concept Sets page.
    const conceptSetActionPage = new ConceptSetActionsPage(this.page);
    await conceptSetActionPage.openConceptSet(conceptName);

    await this.openConceptSetsSubtab();
    return await this.findConceptSetsCard(conceptName);
  }

  /**
   * Click Add Dataset button.
   * Click Add Concept Set button.
   * Click Domain card.
   * @param {Domain} domain
   */
  async openConceptSetSearch(
    domain: Domain
  ): Promise<{ conceptSearchPage: ConceptSetSearchPage; criteriaSearch: CriteriaSearchPage }> {
    // Click Add Datasets button.
    const datasetBuildPage = await this.clickAddDatasetButton();

    // Click Add Concept Sets button.
    const conceptSearchPage = await datasetBuildPage.clickAddConceptSetsButton();

    // Add Concept Set in domain.
    const procedures = ConceptDomainCard.findDomainCard(this.page, domain);
    const criteriaSearch = await procedures.clickSelectConceptButton();

    return { conceptSearchPage, criteriaSearch };
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
    await analysisPage.waitForLoad();
    return analysisPage.createNotebook(notebookName, lang);
  }

  /**
   * @param {string} workspaceName
   */
  async verifyWorkspaceNameOnDataPage(workspaceName: string): Promise<void> {
    await this.waitForLoad();

    const workspaceLink = new Link(this.page, `//a[text()='${workspaceName}']`);
    await workspaceLink.waitForXPath({ visible: true });
    expect(await workspaceLink.isVisible()).toBe(true);
  }

  async findConceptSetsCard(conceptSetsName?: string): Promise<DataResourceCard> {
    await this.openConceptSetsSubtab();
    if (conceptSetsName === undefined) {
      // if Concept Sets name isn't specified, find an existing Concept Sets.
      return new DataResourceCard(this.page).findAnyCard(ResourceCard.ConceptSet);
    }
    // find Concept Set that match specified name.
    return new DataResourceCard(this.page).findCard(conceptSetsName, ResourceCard.ConceptSet);
  }

  async findOrCreateCohort(): Promise<DataResourceCard> {
    // Search for any existing Cohorts
    const existingCohortsCard = await this.findCohortCard();
    if (existingCohortsCard) {
      return existingCohortsCard;
    }

    // None found, create a new Cohorts
    const cohortsCard = await this.createCohort();
    return cohortsCard;
  }

  async findOrCreateConceptSets(): Promise<DataResourceCard> {
    // Search for any existing Concept Sets
    const existingConceptSetCard = await this.findConceptSetsCard();
    if (existingConceptSetCard) {
      return existingConceptSetCard;
    }

    const conceptSetsCard = await this.createConceptSets();
    return conceptSetsCard;
  }
}
