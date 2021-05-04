import { Page } from 'puppeteer';
import Table from 'app/component/table';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import ClrIconLink from 'app/element/clr-icon-link';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import DatasetSaveModal from 'app/modal/dataset-save-modal';
import AuthenticatedPage from './authenticated-page';
import CohortBuildPage from './cohort-build-page';
import ConceptSetSearchPage from './conceptset-search-page';

const pageTitle = 'Dataset Page';

enum LabelAlias {
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
      name: LabelAlias.SelectCohorts,
      ancestorLevel: 3,
      iconShape: 'plus-circle'
    });
    await addCohortButton.clickAndWait();
    const cohortPage = new CohortBuildPage(this.page);
    return cohortPage.waitForLoad();
  }

  async selectCohorts(cohortNames: string[]): Promise<void> {
    for (const cohortName of cohortNames) {
      const xpath = this.getCheckboxXpath(LabelAlias.SelectCohorts, cohortName);
      const checkbox = new Checkbox(this.page, xpath);
      await checkbox.check();
    }
  }

  async selectConceptSets(conceptSetNames: string[]): Promise<void> {
    for (const conceptSetName of conceptSetNames) {
      const xpath = this.getCheckboxXpath(LabelAlias.SelectConceptSets, conceptSetName);
      const checkbox = new Checkbox(this.page, xpath);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  /**
   * Click Add Concept Sets button, opened the Concept Sets page.
   */
  async clickAddConceptSetsButton(): Promise<ConceptSetSearchPage> {
    const addConceptSetsButton = ClrIconLink.findByName(this.page, {
      name: LabelAlias.SelectConceptSets,
      ancestorLevel: 3,
      iconShape: 'plus-circle'
    });
    await addConceptSetsButton.clickAndWait();
    const conceptSetSearchPage = new ConceptSetSearchPage(this.page);
    await conceptSetSearchPage.waitForLoad();
    return conceptSetSearchPage;
  }

  /**
   * Check or uncheck the Select All checkbox.
   * @param {boolean} selectAll
   */
  async selectAllValues(selectAll = true): Promise<void> {
    const xpath = buildXPath({ containsText: LabelAlias.SelectValues, ancestorLevel: 3, type: ElementType.Checkbox });
    const selectAllCheckbox = new Checkbox(this.page, xpath);
    selectAll ? await selectAllCheckbox.check() : await selectAllCheckbox.unCheck();
    await waitWhileLoading(this.page);
  }

  async selectValues(values: string[]): Promise<void> {
    for (const valueName of values) {
      const xpath = this.getCheckboxXpath(LabelAlias.SelectValues, valueName);
      const checkbox = new Checkbox(this.page, xpath);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  /**
   * Click "Save and Analyze" button.
   * @returns Instance of DatasetSaveModal.
   */
  async clickSaveAndAnalyzeButton(): Promise<DatasetSaveModal> {
    const saveButton = Button.findByName(this.page, { name: 'Save and Analyze' });
    await saveButton.waitUntilEnabled();
    await saveButton.click();
    await waitWhileLoading(this.page);
    const saveModal = new DatasetSaveModal(this.page);
    await saveModal.waitForLoad();
    return saveModal;
  }

  async clickAnalyzeButton(): Promise<void> {
    const saveButton = Button.findByName(this.page, { containsText: 'Analyze' });
    await saveButton.waitUntilEnabled();
    await saveButton.click();
  }

  async getPreviewTable(): Promise<Table> {
    const previewButton = Button.findByName(this.page, { name: 'View Preview Table' });
    await previewButton.click();
    await waitWhileLoading(this.page);
    return new Table(this.page, '//table[@class="p-datatable-scrollable-body-table"]');
  }

  private getCheckboxXpath(labelName: string, valueName: string): string {
    return (
      `//label[contains(normalize-space(), "${labelName}")]/ancestor::node()[3]` +
      `//input[@type="checkbox" and @value="${valueName}"]`
    );
  }
}
