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

export enum PhysicalMeasurementsCriteria {
  BloodPressure = 'Blood Pressure',
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
    await Promise.all([
      this.page.waitForXPath('//*[@id="criteria-search-container"]', { visible: true })
      // this.page.waitForXPath('//*[@role="button"]/img[@alt="Go back"]', {visible: true})
    ]);
    await waitWhileLoading(this.page);
    return true;
  }

  waitForPhysicalMeasurementCriteriaLink(criteriaType: PhysicalMeasurementsCriteria): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: criteriaType, iconShape: 'slider', ancestorLevel: 2 });
  }

  waitForVisitsCriteriaLink(criteriaType: Visits): ClrIconLink {
    return ClrIconLink.findByName(this.page, { startsWith: criteriaType, iconShape: 'plus-circle', ancestorLevel: 1 });
  }

  /**
   * Add single Physical Measurements Criteria.
   * @param {PhysicalMeasurementsCriteria} criteriaName
   * @param {FilterSign}  filterSign
   * @param {number} filterValue
   */
  async filterPhysicalMeasurementValue(
    criteriaName: PhysicalMeasurementsCriteria,
    filterSign: FilterSign,
    filterValue: number
  ): Promise<string> {
    await waitWhileLoading(this.page);
    const link = this.waitForPhysicalMeasurementCriteriaLink(criteriaName);
    await link.click();

    // Delay to make sure correct sidebar content is showing
    await this.page.waitForTimeout(1000);

    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(this.page);
    await reviewCriteriaSidebar.waitUntilVisible();
    const participantResult = await reviewCriteriaSidebar.getPhysicalMeasurementParticipantResult(
      filterSign,
      filterValue
    );
    // return participants count for comparing
    return participantResult;
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
      await this.waitForVisitsCriteriaLink(visit).click();
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

  // Save Criteria
  async reviewAndSaveCriteria(): Promise<void> {
    await this.clickFinishAndReviewButton();
    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(this.page);
    await reviewCriteriaSidebar.waitUntilVisible();
    await reviewCriteriaSidebar.clickSaveCriteriaButton();
  }
}
