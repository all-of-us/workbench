import { ElementHandle, Page } from 'puppeteer';
import CohortBuildPage, { FieldSelector } from 'app/page/cohort-build-page';
import { waitForNumericalString, waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortSearchPage, { Ethnicity } from 'app/page/cohort-search-page';
import TieredMenu from 'app/component/tiered-menu';
import { LinkText, MenuOption } from 'app/text-labels';
import { snowmanIconXpath } from 'app/component/snowman-menu';
import Modal from 'app/modal/modal';
import InputSwitch from 'app/element/input-switch';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import { getPropValue } from 'utils/element-utils';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';
import Textbox from 'app/element/textbox';
import RadioButton from 'app/element/radiobutton';
import { numericalStringToNumber } from 'utils/str-utils';
import Table from "app/component/table";

export enum Sex {
  FEMALE = 'Female',
  MALE = 'Male',
  UNKNOWN = 'Unknown',
  SKIPPED = 'Not male, not female, prefer not to answer, or skipped'
}
export enum Race {
  WHITE = 'White',
  BLACK = 'Black or African American',
  UNKNOWN = 'Unknown'
}

export enum PhysicalMeasurementsCriteria {
  BMI = 'BMI',
  BPHypotensive = 'Hypotensive (Systolic <= 90 / Diastolic <= 60)', // BP prefix for Blood Pressure
  BPNormal = 'Normal (Systolic <= 120 / Diastolic <= 80)', // Blood Pressure
  BPPrehypertensive = 'Pre-Hypertensive (Systolic 121-139 / Diastolic 81-89)', // Blood Pressure
  BPHypertensive = 'Hypertensive (Systolic >= 140 / Diastolic >= 90)', // Blood Pressure
  HeartRate = 'Heart Rate',
  Height = 'Height',
  HipCircumference = 'Hip Circumference',
  HRNoIrregularity = 'No-irregularity detected', // Heart Rhythm Status
  HRIrregularity = 'Irregularity detected', // Heart Rhythm Status
  PregnantEnrollment = 'Pregnant at enrollment',
  WaistCircumference = 'Waist Circumference',
  Weight = 'Weight',
  WheelChairUser = 'Wheelchair user at enrollment'
}

export enum Visits {
  AmbulanceVisit = 'Ambulance Visit',
  AmbulatoryClinicCenter = 'Ambulatory Clinic/Center',
  AmbulatoryRehabilitationVisit = 'Ambulatory Rehabilitation Visit',
  EmergencyRoomVisit = 'Emergency Room Visit',
  EmergencyRoomAndInpatientVisit = 'Emergency Room and Inpatient Visit',
  HomeVisit = 'Home Visit',
  InpatientVisit = 'Inpatient Visit',
  LaboratoryVisit = 'Laboratory Visit',
  NonhospitalInstitutionVisit = 'Non-hospital institution Visit',
  OfficeVisit = 'Office Visit',
  OutpatientVisit = 'Outpatient Visit',
  PharmacyVisit = 'Pharmacy visit'
}

export enum FilterSign {
  Any = 'Any',
  AnyValue = 'Any value',
  Equals = 'Equals',
  GreaterThanOrEqualTo = 'Greater Than or Equal To',
  LessThanOrEqualTo = 'Less Than or Equal To',
  Between = 'Between'
}

export default class CohortParticipantsGroup {
  private rootXpath: string;
  private cohortSearchContainerXpath = '//*[@id="cohort-search-container"]';
  private criteriaSearchContainerXpath = '//*[@id="criteria-search-container"]';

  constructor(private readonly page: Page) {}

  setXpath(xpath: string): void {
    this.rootXpath = xpath;
  }

  async exists(): Promise<boolean> {
    return (await this.page.$x(this.rootXpath)).length > 0;
  }

  getEnableCriteriaButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="ENABLE" and @role="button"]`;
    return new Button(this.page, xpath);
  }

  getUndoDeleteCriteriaButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="UNDO" and @role="button"]`;
    return new Button(this.page, xpath);
  }

  getAddCriteriaButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="Add Criteria"]/button`;
    return new Button(this.page, xpath);
  }

  getAnyMentionOfButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="Any mention of"]/button`;
    return new Button(this.page, xpath);
  }

  getDuringSameEncounterAsButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="During same encounter as"]/button`;
    return new Button(this.page, xpath);
  }

  getGroupCountXpath(): string {
    return `${this.rootXpath}${FieldSelector.GroupCount}`;
  }

  getCriteriaXpath(criteriaName: string): string {
    return `${this.rootXpath}//*[@data-test-id="item-list"][.//*[text()="${criteriaName}"]]${snowmanIconXpath}`;
  }
  async clickGroupSnowmanIcon(): Promise<void> {
    const iconXpath = `${this.rootXpath}${snowmanIconXpath}`;
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
  }

  async clickCriteriaSnowmanIcon(criteriaName: string): Promise<void> {
    const snowmanIcon = new ClrIconLink(this.page, this.getCriteriaXpath(criteriaName));
    await snowmanIcon.click();
  }

  /**
   * Build Cohort Criteria page: Finds the Group or Criteria Snowman menu.
   * @param {GroupMenuOption} option
   */
  async selectSnowmanMenu(option: MenuOption): Promise<void> {
    const menu = new TieredMenu(this.page);
    await menu.waitUntilVisible();
    return menu.select(option);
  }

  /**
   * Update Group name.
   * @param {string} newGroupName
   * @return {boolean} Returns TRUE if rename was successful.
   */
  async editGroupName(newGroupName: string): Promise<void> {
    await this.clickGroupSnowmanIcon();
    await this.selectSnowmanMenu(MenuOption.EditGroupName);
    const modal = new Modal(this.page);
    const textbox = modal.waitForTextbox('New Name:');
    await textbox.type(newGroupName);
    await modal.clickButton(LinkText.Rename, { waitForClose: true });
  }

  async editCriteriaName(criteriaName: string, newName: string): Promise<void> {
    await this.clickCriteriaSnowmanIcon(criteriaName);
    await this.selectSnowmanMenu(MenuOption.EditCriteriaName);
    const modal = new Modal(this.page);
    const textbox = modal.waitForTextbox('New Name:');
    await textbox.type(newName);
    await modal.clickButton(LinkText.Rename, { waitForClose: true });
    await waitWhileLoading(this.page);
  }

  async suppressCriteriaFromTotalCount(criteriaName: string): Promise<void> {
    await this.clickCriteriaSnowmanIcon(criteriaName);
    await this.selectSnowmanMenu(MenuOption.SuppressCriteriaFromTotalCount);
  }

  async deleteCriteria(criteriaName: string): Promise<void> {
    await this.clickCriteriaSnowmanIcon(criteriaName);
    await this.selectSnowmanMenu(MenuOption.DeleteCriteria);
    const xpath = `${this.rootXpath}//*[normalize-space()="UNDO" and @role="button"]`;
    const undoButton = new Button(this.page, xpath);
    await undoButton.waitForXPath(); // Find the UNDO button but do not click.
  }

  /**
   * Delete Group.
   * @return Returns array of criterias in this group.
   */
  async deleteGroup(): Promise<ElementHandle[]> {
    await this.clickGroupSnowmanIcon();
    await this.selectSnowmanMenu(MenuOption.DeleteGroup);
    try {
      await waitForText(this.page, 'This group has been deleted');
    } catch (err) {
      // Sometimes message fails to show up. Ignore error.
    }
    await waitWhileLoading(this.page);
    return this.findGroupCriteriaList();
  }

  async includePhysicalMeasurement(
    criteriaList: PhysicalMeasurementsCriteria[],
    opts?: { filterSign?: FilterSign; filterValue?: number }
  ): Promise<void> {
    await this.clickCriteriaMenuItems([MenuOption.PhysicalMeasurements]);
    let icon: ClrIconLink;
    // Add every criteria in list.
    for (const criteria of criteriaList) {
      switch (criteria) {
        // Blood Pressure
        case PhysicalMeasurementsCriteria.BPHypotensive:
        case PhysicalMeasurementsCriteria.BPNormal:
        case PhysicalMeasurementsCriteria.BPPrehypertensive:
        case PhysicalMeasurementsCriteria.BPHypertensive:
          // Expand Blood Pressure group.
          icon = ClrIconLink.findByName(this.page, {
            name: 'Blood Pressure',
            iconShape: 'angle right',
            ancestorLevel: 3
          });
          await icon.click();
          // Click Add Criteria link.
          icon = ClrIconLink.findByName(this.page, {
            startsWith: criteria,
            iconShape: 'plus-circle',
            ancestorLevel: 1
          });
          await icon.click();
          await this.finishReviewAndSaveCriteria();
          break;
        case PhysicalMeasurementsCriteria.Weight:
        case PhysicalMeasurementsCriteria.BMI:
        case PhysicalMeasurementsCriteria.Height:
          {
            icon = ClrIconLink.findByName(this.page, {
              name: criteria,
              iconShape: 'slider',
              ancestorLevel: 2
            });
            await icon.click();
            // The Review Criteria sidebar opens automatically.
            const reviewCriteriaSidebar = new ReviewCriteriaSidebar(this.page);
            await reviewCriteriaSidebar.waitUntilVisible();
            const { filterSign = FilterSign.GreaterThanOrEqualTo, filterValue = 101 } = opts;
            await reviewCriteriaSidebar.getPhysicalMeasurementParticipantResult(filterSign, filterValue);
          }
          break;
        case PhysicalMeasurementsCriteria.PregnantEnrollment:
        case PhysicalMeasurementsCriteria.WheelChairUser:
          icon = ClrIconLink.findByName(this.page, {
            startsWith: criteria,
            iconShape: 'plus-circle',
            ancestorLevel: 1
          });
          await icon.click();
          await this.finishReviewAndSaveCriteria();
          break;
        // No default case. Force user to config for new criteria explicitly.
      }
    }
  }

  async includeFitbit(): Promise<number> {
    await this.clickCriteriaMenuItems([MenuOption.Fitbit]);
    return this.getGroupCount();
  }

  async includeDemographicsDeceased(): Promise<number> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Deceased]);
    return this.getGroupCount();
  }

  async includeWholeGenomeVariant(): Promise<number> {
    await this.clickCriteriaMenuItems([MenuOption.WholeGenomeVariant]);
    return this.getGroupCount();
  }

  async includeConditions(condition: string, addTotal = 5): Promise<void> {
    await this.clickCriteriaMenuItems([MenuOption.Conditions]);
    await waitWhileLoading(this.page);

    const searchResultsTable = await this.searchCriteria(condition);

    // Add addTotal rows.
    const rows = await searchResultsTable.getRows();
    for (let i=0; i < rows.length; i++)  {
      const cellValue = await searchResultsTable.getCellValue(i, 1);
      const addIcon = ClrIconLink.findByName(
          this.page,
          { containsText: cellValue, iconShape: 'plus-circle' },
          searchResultsTable
      );
      await addIcon.click();
      console.log(`Added condition: ${cellValue}`);
      if (i == addTotal) {
        break;
      }
    }

    await this.finishReviewAndSaveCriteria();
  }

  async includeDrugs(drug: string, addTotal = 5): Promise<void> {
    await this.clickCriteriaMenuItems([MenuOption.Drugs]);
    await waitWhileLoading(this.page);

    // Search for drug.
    const searchResultsTable = await this.searchCriteria(drug);

    // Add addTotal rows.
    const rows = await searchResultsTable.getRows();
    for (let i=0; i < rows.length; i++) {
      const cellValue = await searchResultsTable.getCellValue(1, 1);
      const addIcon = ClrIconLink.findByName(
          this.page,
          { containsText: cellValue, iconShape: 'plus-circle' },
          searchResultsTable
      );
      await addIcon.click();
      if (i == addTotal) {
        break;
      }
    }

    await this.finishReviewAndSaveCriteria();
  }

  async includeProcedures(procedure: string, addTotal = 5): Promise<void> {
    await this.clickCriteriaMenuItems([MenuOption.Procedures]);
    await waitWhileLoading(this.page);

    // Search for procedure.
    const searchResultsTable = await this.searchCriteria(procedure);

    // Add addTotal rows.
    const rows = await searchResultsTable.getRows();
    for (let i=0; i < rows.length; i++) {
      const cellValue = await searchResultsTable.getCellValue(1, 1);
      const addIcon = ClrIconLink.findByName(
          this.page,
          { containsText: cellValue, iconShape: 'plus-circle' },
          searchResultsTable
      );
      await addIcon.click();
      if (i == addTotal) {
        break;
      }
    }

    await this.finishReviewAndSaveCriteria();
  }

  async includeLabsAndMeasurements(lab: string, addTotal = 5): Promise<void> {
    await this.clickCriteriaMenuItems([MenuOption.LabsAndMeasurements]);
    await waitWhileLoading(this.page);

    // Search for procedure.
    const searchResultsTable = await this.searchCriteria(lab);

    // Add addTotal rows.
    const rows = await searchResultsTable.getRows();
    for (let i=0; i < rows.length; i++) {
      const cellValue = await searchResultsTable.getCellValue(1, 1);
      const addIcon = ClrIconLink.findByName(
          this.page,
          { containsText: cellValue, iconShape: 'plus-circle' },
          searchResultsTable
      );
      await addIcon.click();
      if (i == addTotal) {
        break;
      }
    }

    await this.finishReviewAndSaveCriteria();
  }

  async includeEthnicity(ethnicities: Ethnicity[]): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Ethnicity]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    for (const ethnicity of ethnicities) {
      const link = searchPage.waitForEthnicityCriteriaLink(ethnicity);
      await link.click();
      await this.getCriteriaAddedSuccessMessage();
    }
    const count = waitForNumericalString(this.page, this.numberOfParticipantsXpath());
    await this.finishReviewAndSaveCriteria();
    return count;
  }

  async includeGenderIdentity(identities: string[]): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.GenderIdentity]);
    for (const identity of identities) {
      const link = ClrIconLink.findByName(this.page, {
        startsWith: identity,
        iconShape: 'plus-circle',
        ancestorLevel: 0
      });
      await link.click();
      await this.getCriteriaAddedSuccessMessage();
    }
    const count = waitForNumericalString(this.page, this.numberOfParticipantsXpath());
    await this.finishReviewAndSaveCriteria();
    return count;
  }

  // Demographics -> Race
  async includeRace(race: string[]): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Race]);
    for (const r of race) {
      const link = ClrIconLink.findByName(this.page, {
        startsWith: r,
        iconShape: 'plus-circle',
        ancestorLevel: 0
      });
      await link.click();
      await this.getCriteriaAddedSuccessMessage();
    }
    const count = waitForNumericalString(this.page, this.numberOfParticipantsXpath());
    await this.finishReviewAndSaveCriteria();
    return count;
  }

  async includeSexAssignedAtBirth(selections: Sex[]) {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.SexAssignedAtBirth]);
    for (const selection of selections) {
      const link = ClrIconLink.findByName(this.page, {
        startsWith: selection,
        iconShape: 'plus-circle',
        ancestorLevel: 0
      });
      await link.click();
      await this.getCriteriaAddedSuccessMessage();
    }
    const count = waitForNumericalString(this.page, this.numberOfParticipantsXpath());
    await this.finishReviewAndSaveCriteria();
    return count;
  }

  async getGroupCount(): Promise<number> {
    const [count] = await Promise.all([
      waitForNumericalString(this.page, this.getGroupCountXpath(), 120000),
      waitWhileLoading(this.page, 120000)
    ]);
    return numericalStringToNumber(count);
  }

  /**
   * Type age lower and upper bounds.
   * @param {number} minAge
   * @param {number} maxAge
   */
  async includeAge(minAge: number, maxAge: number): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Age]);
    await waitWhileLoading(this.page);
    const selector = `${this.cohortSearchContainerXpath}//input[@type="number"]`;
    await this.page.waitForXPath(selector, { visible: true });

    const [lowerNumberInput, upperNumberInput] = await this.page.$x(selector);
    await Textbox.asBaseElement(this.page, lowerNumberInput)
      .type(minAge.toString())
      .then((input) => input.pressTab());
    await Textbox.asBaseElement(this.page, upperNumberInput)
      .type(maxAge.toString())
      .then((input) => input.pressTab());

    // Select "Age at Consent" radiobutton.
    const radio = RadioButton.findByName(this.page, {
      name: 'Age at Consent'
    });
    await radio.select();

    // Get count from slider badge
    const count = await waitForNumericalString(this.page, `${this.cohortSearchContainerXpath}//*[@id="age-count"]`);

    // Click "ADD SELECTION" button to add selected age range
    await Button.findByName(this.page, { name: LinkText.AddSelection }).click();
    await this.finishReviewAndSaveCriteria();
    await waitWhileLoading(this.page);
    return count;
  }

  async includeVisits(visits: Visits[]): Promise<number> {
    await this.clickCriteriaMenuItems([MenuOption.Visits]);
    for (const visit of visits) {
      const icon = ClrIconLink.findByName(this.page, {
        startsWith: visit,
        iconShape: 'plus-circle',
        ancestorLevel: 1
      });
      await icon.click();
      await this.getCriteriaAddedSuccessMessage();
    }
    await this.finishReviewAndSaveCriteria();
    // Wait for Cohort Build page to load.
    await waitWhileLoading(this.page);
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    return cohortBuildPage.getTotalCount();
  }

  async clickCriteriaMenuItems(menuItemLinks: MenuOption[]): Promise<void> {
    const menu = await this.openAddCriteriaTieredMenu();
    await menu.select(menuItemLinks);
    await waitWhileLoading(this.page);
  }

  async openAddCriteriaTieredMenu(): Promise<TieredMenu> {
    const addCriteriaButton = this.getAddCriteriaButton();
    await addCriteriaButton.waitUntilEnabled();
    await addCriteriaButton.focus();
    await addCriteriaButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async openAnyMentionOfTieredMenu(): Promise<TieredMenu> {
    const anyMentionOfButton = this.getAnyMentionOfButton();
    await anyMentionOfButton.waitUntilEnabled();
    await anyMentionOfButton.focus();
    await anyMentionOfButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async openDuringSameEncounterAsTieredMenu(): Promise<TieredMenu> {
    const duringSameEncounterButton = this.getDuringSameEncounterAsButton();
    await duringSameEncounterButton.waitUntilEnabled();
    await duringSameEncounterButton.focus();
    await duringSameEncounterButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async findGroupCriteriaList(): Promise<ElementHandle[]> {
    const selector = `${this.rootXpath}//*[@data-test-id="item-list"]`;
    return this.page.$x(selector);
  }

  numberOfParticipantsXpath(): string {
    return (
      this.cohortSearchContainerXpath +
      '//*[./*[contains(text(), "Results")]]/div[contains(text(), "Number Participants:")]'
    );
  }

  async clickTemporalSwitch(onoff: boolean): Promise<void> {
    // InputSwitch constructor takes xpath selector.
    const xpath = '//*[contains(concat(" ", normalize-space(@class), " ")," p-inputswitch ")  and @role="checkbox"]';
    const inputSwitch = new InputSwitch(this.page, xpath);
    onoff ? await inputSwitch.check() : await inputSwitch.unCheck();

    // waitForFunction takes css selector.
    const inputSwitchCss = '.p-inputswitch.p-component[role="checkbox"]';
    await this.page.waitForFunction(
      (css) => document.querySelectorAll(css),
      { polling: 'mutation', timeout: 30000 },
      inputSwitchCss
    );
    await this.page.waitForTimeout(1000);
  }

  async getCriteriaAddedSuccessMessage(): Promise<string> {
    const css = '.p-growl-message-success .p-growl-image.pi-check + .p-growl-message .p-growl-details';
    const msg = await this.page.waitForSelector(css, { visible: true, timeout: 5000 });
    return getPropValue<string>(msg, 'textContent');
  }

  // Click Finish and Review button in sidebar
  async clickFinishAndReviewButton(): Promise<void> {
    const finishAndReviewButton = Button.findByName(this.page, { name: LinkText.FinishAndReview });
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();
  }

  // Handling "Add selected criteria to cohort" sidebar. Save Criteria. The sidebar contents are not checked.
  async finishReviewAndSaveCriteria(): Promise<number> {
    await this.clickFinishAndReviewButton();
    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(this.page);
    await reviewCriteriaSidebar.waitUntilVisible();
    await reviewCriteriaSidebar.clickSaveCriteriaButton();
    // Wait for Cohort Build page to load.
    await waitWhileLoading(this.page);
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    const count = cohortBuildPage.getTotalCount();
    await this.page.waitForTimeout(1000); // Short pause to wait for charts animation finish.
    return count;
  }

  async searchCriteria(searchWord: string): Promise<Table> {
    const resultsTable = new Table(this.page, `${this.criteriaSearchContainerXpath}//table[@class="p-datatable"]`);
    await resultsTable.waitForVisible();
    const searchFilterTextbox = Textbox.findByName(this.page, { containsText: 'by code or description' }, resultsTable);
    await searchFilterTextbox.type(searchWord);
    await searchFilterTextbox.pressReturn();
    await waitWhileLoading(this.page);
    return resultsTable;
  }

  async resultsTableSelectRow(
      rowIndex = 1,
      selectionColumnIndex = 1
  ): Promise<{ name: string; code: string; vocabulary: string; rollUpCount: string }> {
    const resultsTable = this.getSearchResultsTable();

    // Name column #1
    const nameCell = await resultsTable.getCell(rowIndex, 1);
    const nameElem = (await nameCell.$x('.//div[@data-test-id="name-column-value"]'))[0];
    const nameValue = await getPropValue<string>(nameElem, 'textContent');

    // Code column #2
    const codeCell = await resultsTable.getCell(rowIndex, 2);
    const codeValue = await getPropValue<string>(codeCell, 'textContent');

    // Vocabulary column #3
    const vocabularyCell = await resultsTable.getCell(rowIndex, 3);
    const vocabValue = await getPropValue<string>(vocabularyCell, 'textContent');

    // Roll-up Count column #6
    const rollUpCountCell = await resultsTable.getCell(rowIndex, 6);
    const rollUpCountValue = await getPropValue<string>(rollUpCountCell, 'textContent');

    const selectCheckCell = await resultsTable.getCell(rowIndex, selectionColumnIndex);
    const elemt = (await selectCheckCell.$x('.//*[@shape="plus-circle"]'))[0];
    await elemt.click();

    return { name: nameValue, code: codeValue, vocabulary: vocabValue, rollUpCount: rollUpCountValue };
  }

  getSearchResultsTable(): Table {
    return new Table(this.page,
        `${this.criteriaSearchContainerXpath}//table[@data-test-id="list-search-results-table"]`);
  }

}
