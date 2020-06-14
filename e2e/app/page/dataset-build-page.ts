import Table from 'app/component/table';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import {xPathOptionToXpath} from 'app/element/xpath-defaults';
import {ElementType} from 'app/xpath-options';
import AuthenticatedPage from './authenticated-page';
import CohortBuildPage from './cohort-build-page';

const PageTitle = 'Dataset Page';

enum LabelAlias {
  SelectValues = 'Select Values',
  SelectConceptSets = 'Select Concept Sets',
  SelectCohorts = 'Select Cohorts',
}

export default class DatasetBuildPage extends AuthenticatedPage {

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`DatasetBuildPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async getAddCohortsButton(): Promise<Button> {
    const buttonXpath = xPathOptionToXpath({name: LabelAlias.SelectCohorts, ancestorLevel: 3, iconShape: 'is-solid'});
    return new Button(this.page, buttonXpath);
  }

  async clickAddCohortsButton(): Promise<CohortBuildPage> {
    const addCohortButton = await this.getAddCohortsButton();
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
   * Check or uncheck the Select All checkbox.
   * @param {boolean} selectAll
   */
  async selectAllValues(selectAll: boolean = false): Promise<void> {
    const xpath = xPathOptionToXpath({name: LabelAlias.SelectValues, ancestorLevel: 3, type: ElementType.Checkbox});
    const selectAllCheckbox = new Checkbox(this.page, xpath);
    selectAll ? await selectAllCheckbox.check() : await selectAllCheckbox.unCheck();
    await waitWhileLoading(this.page);
  }

  async selectValues(values?: string[]): Promise<void> {
    for (const valueName of values) {
      const xpath = this.getCheckboxXpath(LabelAlias.SelectValues, valueName);
      const checkbox = new Checkbox(this.page, xpath);
      await checkbox.check();
      await waitWhileLoading(this.page);
    }
  }

  async clickSaveAndAnalyzeButton(): Promise<void> {
    const saveButton = await Button.findByName(this.page, {name: 'Save and Analyze'});
    await saveButton.waitUntilEnabled();
    await saveButton.click();
  }

  async clickAnalyzeButton(): Promise<void> {
    const saveButton = await Button.findByName(this.page, {containsText: 'Analyze'});
    await saveButton.waitUntilEnabled();
    await saveButton.click();
  }

  async getPreviewTable(): Promise<Table> {
    const previewButton = await Button.findByName(this.page, {name: 'View Preview Table'});
    await previewButton.click();
    await waitWhileLoading(this.page);
    return new Table(this.page, '//table[@class="p-datatable-scrollable-body-table"]');
  }

  private getCheckboxXpath(labelName: string, valueName: string): string {
    return `//label[contains(normalize-space(), "${labelName}")]/ancestor::node()[3]//input[@type="checkbox" and @value="${valueName}"]`;
  }

}
