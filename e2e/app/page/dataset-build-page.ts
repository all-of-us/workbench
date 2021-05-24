import { Page } from 'puppeteer';
import Table from 'app/component/table';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import ClrIconLink from 'app/element/clr-icon-link';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import AuthenticatedPage from './authenticated-page';
import CohortBuildPage from './cohort-build-page';
import ConceptSetSearchPage from './conceptset-search-page';
import Link from 'app/element/link';
import DatasetCreateModal from 'app/modal/dataset-create-modal';
import WorkspaceDataPage from './workspace-data-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceAboutPage from './workspace-about-page';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import { getPropValue } from 'utils/element-utils';
import { LinkText } from 'app/text-labels';

const pageTitle = 'Dataset Page';

enum StepLabels {
  SelectValues = 'Select Values',
  SelectConceptSets = 'Select Concept Sets',
  SelectCohorts = 'Select Cohorts'
}

export default class DatasetBuildPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, pageTitle), waitWhileLoading(this.page)]);
    return true;
  }

  async clickAddCohortsButton(): Promise<CohortBuildPage> {
    const addCohortButton = ClrIconLink.findByName(this.page, {
      name: StepLabels.SelectCohorts,
      ancestorLevel: 3,
      iconShape: 'plus-circle'
    });
    await addCohortButton.clickAndWait();
    const cohortPage = new CohortBuildPage(this.page);
    return cohortPage.waitForLoad();
  }

  async selectCohorts(cohortNames: string[]): Promise<void> {
    for (const cohortName of cohortNames) {
      const checkbox = this.getCohortCheckBox(cohortName);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  async unselectCohort(cohortName: string): Promise<void> {
    const xpath = this.getCheckboxXpath(StepLabels.SelectCohorts, cohortName);
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.unCheck();
  }

  async selectConceptSets(conceptSetNames: string[]): Promise<void> {
    for (const conceptSetName of conceptSetNames) {
      const checkbox = this.getConceptSetCheckBox(conceptSetName);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  async unselectConceptSet(conceptSetName: string): Promise<void> {
    const xpath = this.getCheckboxXpath(StepLabels.SelectConceptSets, conceptSetName);
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.unCheck();
    await waitWhileLoading(this.page);
  }

  /**
   * Click Add Concept Sets button, opened the Concept Sets page.
   */
  async clickAddConceptSetsButton(): Promise<ConceptSetSearchPage> {
    const addConceptSetsButton = ClrIconLink.findByName(this.page, {
      name: StepLabels.SelectConceptSets,
      ancestorLevel: 3,
      iconShape: 'plus-circle'
    });
    await addConceptSetsButton.clickAndWait();
    const conceptSetSearchPage = new ConceptSetSearchPage(this.page);
    await conceptSetSearchPage.waitForLoad();
    return conceptSetSearchPage;
  }

  /**
   * Check or uncheck Select Values: Select All checkbox.
   * @param {boolean} selectAll
   */
  async selectAllValues(check = true): Promise<void> {
    check ? await this.getSelectAllCheckbox().check() : await this.getSelectAllCheckbox().unCheck();
    await waitWhileLoading(this.page);
  }

  async selectValues(values: string[]): Promise<void> {
    for (const valueName of values) {
      const xpath = this.getCheckboxXpath(StepLabels.SelectValues, valueName);
      const checkbox = new Checkbox(this.page, xpath);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  /**
   * Click "Create" button.
   * @returns Instance of DatasetCreateModal.
   */
  async clickCreateButton(): Promise<DatasetCreateModal> {
    const createButton = this.getCreateDatasetButton();
    await createButton.waitUntilEnabled();
    await createButton.click();
    await waitWhileLoading(this.page);
    const createModal = new DatasetCreateModal(this.page);
    await createModal.waitForLoad();
    return createModal;
  }

  async clickAnalyzeButton(): Promise<ExportToNotebookModal> {
    const analyzeButton = this.getAnalyzeButton();
    await analyzeButton.waitUntilEnabled();
    await analyzeButton.click();
    await waitWhileLoading(this.page);
    const exportModal = new ExportToNotebookModal(this.page);
    await exportModal.waitForLoad();
    return exportModal;
  }

  getCreateDatasetButton(): Button {
    return Button.findByName(this.page, { name: 'Create Dataset' });
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  getAnalyzeButton(): Button {
    return Button.findByName(this.page, { containsText: 'Analyze' });
  }

  async getPreviewTable(): Promise<Table> {
    await this.getPreviewTableButton().click();
    await waitWhileLoading(this.page);
    return new Table(this.page, '//table[@class="p-datatable-scrollable-body-table"]');
  }

  getPreviewTableButton(): Button {
    return Button.findByName(this.page, { name: 'View Preview Table' });
  }

  getSelectAllCheckbox(): Checkbox {
    const xpath = buildXPath({ dataTestId: 'select-all', type: ElementType.Checkbox });
    return new Checkbox(this.page, xpath);
  }

  getBackToWorkspacesLink(): Link {
    return new Link(this.page, '//a[text()="Workspaces" and @href="/workspaces"]');
  }

  getCohortCheckBox(cohortName: string): Checkbox {
    const xpath = this.getCheckboxXpath(StepLabels.SelectCohorts, cohortName);
    return new Checkbox(this.page, xpath);
  }

  getConceptSetCheckBox(conceptSetName: string): Checkbox {
    const xpath = this.getCheckboxXpath(StepLabels.SelectConceptSets, conceptSetName);
    return new Checkbox(this.page, xpath);
  }

  async clickDataTab(): Promise<WorkspaceDataPage> {
    const dataPage = new WorkspaceDataPage(this.page);
    await dataPage.openDataPage({ waitPageChange: true });
    await dataPage.waitForLoad();
    return dataPage;
  }

  async clickAnalysisTab(): Promise<WorkspaceAnalysisPage> {
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.openAnalysisPage({ waitPageChange: true });
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  async clickAboutTab(): Promise<WorkspaceAboutPage> {
    const aboutPage = new WorkspaceAboutPage(this.page);
    await aboutPage.openAboutPage({ waitPageChange: true });
    await aboutPage.waitForLoad();
    return aboutPage;
  }

  async getDatasetName(): Promise<string> {
    const h2 = await this.page.waitForXPath('//h2', { visible: true });
    return getPropValue<string>(h2, 'textContent');
  }

  private getCheckboxXpath(labelName: string, valueName: string): string {
    return (
      `//label[contains(normalize-space(), "${labelName}")]/ancestor::node()[3]` +
      `//input[@type="checkbox" and @value="${valueName}"]`
    );
  }
}
