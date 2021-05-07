import { Page } from 'puppeteer';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';
import SelectMenu from 'app/component/select-menu';
import Table from 'app/component/table';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import Textbox from 'app/element/textbox';
import { waitForNumericalString, waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import CohortBuildPage from './cohort-build-page';

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

export default class CriteriaSearchPage extends AuthenticatedPage {
  private containerXpath = '//*[@id="criteria-search-container"]';

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([this.page.waitForXPath('//*[@id="criteria-search-container"]', { visible: true })]);
    await waitWhileLoading(this.page);
    return true;
  }

  // Add Physical Measurements Criteria only.
  async addPhysicalMeasurementsCriteria(
    criteriaList: PhysicalMeasurementsCriteria[],
    opts?: { filterSign?: FilterSign; filterValue?: number }
  ): Promise<string> {
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
            icon = ClrIconLink.findByName(this.page, { name: criteria, iconShape: 'slider', ancestorLevel: 2 });
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

    // Wait for Cohort Build page to load.
    await waitWhileLoading(this.page);
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    const total = cohortBuildPage.getTotalCount();
    await this.page.waitForTimeout(1000);
    return total;
  }

  async waitForParticipantResult(): Promise<string> {
    const selector = `${this.containerXpath}//*[./*[contains(text(), "Results")]]/div[contains(text(), "Number")]`;
    return waitForNumericalString(this.page, selector);
  }

  getConditionSearchResultsTable(): Table {
    return new Table(this.page, '//table[@class="p-datatable"]');
  }

  async searchCriteria(searchWord: string): Promise<Table> {
    const resultsTable = this.getConditionSearchResultsTable();
    const searchFilterTextbox = Textbox.findByName(this.page, { containsText: 'by code or description' });
    await searchFilterTextbox.type(searchWord);
    await searchFilterTextbox.pressReturn();
    await waitWhileLoading(this.page);
    return resultsTable;
  }

  async addAgeModifier(filterSign: FilterSign, filterValue: number): Promise<string> {
    const selectMenu = SelectMenu.findByName(this.page, { name: 'Age At Event', ancestorLevel: 2 });
    await selectMenu.select(filterSign);
    const numberField = await this.page.waitForXPath(`${this.containerXpath}//input[@type="number"]`, {
      visible: true
    });
    // Issue with Puppeteer type() function: typing value in this textbox doesn't always trigger change event. workaround is needed.
    // Error: "Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation."
    await numberField.focus();
    await numberField.click();
    await this.page.keyboard.type(String(filterValue));
    await numberField.press('Tab', { delay: 200 });

    let participantResult;
    await Button.findByName(this.page, { name: LinkText.Calculate }).click();
    try {
      participantResult = await this.waitForParticipantResult();
    } catch (e) {
      // Retry one more time.
      await Button.findByName(this.page, { name: LinkText.Calculate }).click();
      participantResult = await this.waitForParticipantResult();
    }
    console.debug(`Age Modifier: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);
    return participantResult;
  }

  async addVisits(visits: Visits[]): Promise<void> {
    for (const visit of visits) {
      const icon = ClrIconLink.findByName(this.page, { startsWith: visit, iconShape: 'plus-circle', ancestorLevel: 1 });
      await icon.click();
      await this.finishReviewAndSaveCriteria();
    }
  }

  getResultsTable(): Table {
    return new Table(this.page, '//table[@data-test-id="list-search-results-table"]');
  }

  async resultsTableSelectRow(
    rowIndex = 1,
    selectionColumnIndex = 1
  ): Promise<{ name: string; code: string; vocabulary: string; rollUpCount: string }> {
    const resultsTable = this.getResultsTable();

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

  // Click Finish and Review button in sidebar
  async clickFinishAndReviewButton(): Promise<void> {
    const finishAndReviewButton = Button.findByName(this.page, { name: LinkText.FinishAndReview });
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();
  }

  // Handling "Add selected criteria to cohort" sidebar. Save Criteria. The sidebar contents are not checked.
  async finishReviewAndSaveCriteria(): Promise<void> {
    await this.clickFinishAndReviewButton();
    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(this.page);
    await reviewCriteriaSidebar.waitUntilVisible();
    await reviewCriteriaSidebar.clickSaveCriteriaButton();
  }
}
