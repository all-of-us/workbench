import {Page} from 'puppeteer';
import HelpSidebar from 'app/component/help-sidebar';
import Modal from 'app/component/modal';
import SelectMenu from 'app/component/select-menu';
import Table from 'app/component/table';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import Textbox from 'app/element/textbox';
import {centerPoint, dragDrop, waitWhileLoading} from 'utils/test-utils';
import {waitForNumericalString} from 'utils/waits-utils';
import {LinkText} from 'app/text-labels';
import {waitUntilChanged} from 'utils/element-utils';

const defaultXpath = '//*[@id="cohort-search-container"]';

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
  WheelChairUser = 'Wheelchair user at enrollment',
}

export enum Ethnicity {
  NotHispanicOrLatino = 'Not Hispanic or Latino',
  HispanicOrLatino = 'Hispanic or Latino',
  RaceEthnicityNoneOfThese = 'Race Ethnicity None Of These',
  PreferNotToAnswer = 'Prefer Not To Answer',
  Skip = 'Skip',
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
  PharmacyVisit = 'Pharmacy visit',
}

export enum FilterSign {
  Any = 'Any',
  AnyValue = 'Any value',
  Equals = 'Equals',
  GreaterThanOrEqualTo = 'Greater Than or Equal To',
  LessThanOrEqualTo = 'Less Than or Equal To',
  Between = 'Between',
}

export default class CohortSearchPage extends Modal {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async waitForPhysicalMeasurementCriteriaLink(criteriaType: PhysicalMeasurementsCriteria): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: criteriaType, iconShape: 'slider', ancestorLevel: 2}, this);
  }

  async waitForEthnicityCriteriaLink(criteriaType: Ethnicity): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {startsWith: criteriaType, iconShape: 'plus-circle', ancestorLevel: 0}, this);
  }

  async waitForVisitsCriteriaLink(criteriaType: Visits): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {startsWith: criteriaType, iconShape: 'plus-circle', ancestorLevel: 1}, this);
  }

  /**
   * Add single Physical Measurements Criteria.
   * @param {PhysicalMeasurementsCriteria} criteriaName
   * @param {FilterSign}  filterSign
   * @param {number} filterValue
   */
  async filterPhysicalMeasurementValue(criteriaName: PhysicalMeasurementsCriteria,
                                       filterSign: FilterSign,
                                       filterValue: number): Promise<string> {

    const link = await this.waitForPhysicalMeasurementCriteriaLink(criteriaName);
    await link.click();

    const helpSidebar = new HelpSidebar(this.page);
    const participantResult = helpSidebar.getPhysicalMeasurementParticipantResult(filterSign, filterValue);
    console.debug(`Physical Measurements ${criteriaName}: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);

    // return participants count for comparing
    return participantResult;
  }

  /**
   * Click FINISH button.
   */
  async clickFinishButton(): Promise<void> {
    return this.clickButton(LinkText.FinishAndReview, {waitForClose: true});
  }

  async viewAndSaveCriteria(): Promise<void> {
    const finishAndReviewButton = await Button.findByName(this.page, {name: LinkText.FinishAndReview});
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Click Save Criteria button in sidebar
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSaveCriteriaButton();
  }

  async waitForParticipantResult(): Promise<string> {
    const selector = `${this.xpath}//*[./*[contains(text(), "Results")]]/div[contains(text(), "Number")]`;
    return waitForNumericalString(this.page, selector);
  }

  getConditionSearchResultsTable(): Table {
    return new Table(this.page, '//table[@class="p-datatable"]', this);
  }

  async searchCondition(searchWord: string): Promise<Table> {
    const resultsTable = this.getConditionSearchResultsTable();
    const exists = await resultsTable.exists();
    const searchFilterTextbox = await Textbox.findByName(this.page, {containsText: 'by code or description'}, this);
    await searchFilterTextbox.type(searchWord);
    await searchFilterTextbox.pressReturn();
    if (exists) {
      // New search triggers new request to fetch new results. Need to wait for the old table detached from DOM.
      await waitUntilChanged(this.page, await resultsTable.asElement());
    }
    await waitWhileLoading(this.page);
    return resultsTable;
  }

  async addAgeModifier(filterSign: FilterSign, filterValue: number): Promise<string> {
    const selectMenu = await SelectMenu.findByName(this.page, {name: 'Age At Event', ancestorLevel: 2}, this);
    await selectMenu.clickMenuItem(filterSign);
    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, {visible: true});
    // Issue with Puppeteer type() function: typing value in this textbox doesn't always trigger change event. workaround is needed.
    // Error: "Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation."
    await numberField.focus();
    await numberField.click();
    await this.page.keyboard.type(String(filterValue));
    await numberField.press('Tab', { delay: 200 });

    let participantResult;
    await this.clickButton(LinkText.Calculate);
    try {
      participantResult = await this.waitForParticipantResult();
    } catch (e) {
      // Retry one more time.
      await this.clickButton(LinkText.Calculate);
      participantResult = await this.waitForParticipantResult();
    }
    console.debug(`Age Modifier: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);
    return participantResult;
  }

  async addEthnicity(ethnicities: Ethnicity[]): Promise<void> {
    for (const ethnicity of ethnicities) {
      const link = await this.waitForEthnicityCriteriaLink(ethnicity);
      await link.click();
    }
  }

  /**
   * Type age lower and upper bounds.
   * @param {number} minAge
   * @param {number} maxAge
   */
  async addAge(minAge: number, maxAge: number): Promise<string> {
    const selector = `${this.getXpath()}//input[@type="number"]`;
    await this.page.waitForXPath(selector, {visible: true});

    const [lowerNumberInput, upperNumberInput] = await this.page.$x(selector);
    await (Textbox.asBaseElement(this.page, lowerNumberInput)).type(minAge.toString()).then(input => input.pressTab());
    await (Textbox.asBaseElement(this.page, upperNumberInput)).type(maxAge.toString()).then(input => input.pressTab());

    // Get count from slider badge
    const count = await waitForNumericalString(this.page, `${this.xpath}//*[@id="age-count"]`);

    // Click ADD SELECTION to add selected age range
    await this.clickButton(LinkText.AddSelection);

    // Click FINISH & REVIEW button. Sidebar should open.
    const finishAndReviewButton = await Button.findByName(page, {name: LinkText.FinishAndReview});
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Click SAVE CRITERIA button. Sidebar closes.
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSaveCriteriaButton();
    return count;
  }

  // Experimental
  async drageAgeSlider(): Promise<void> {
    const getXpath = (classValue: string) => {
      return `${this.getXpath()}//*[text()="Age Range"]/ancestor::node()[1]//*[contains(@class,"${classValue}") and @role="slider"]`;
    }

    const lowerNumberInputHandle = await this.page.waitForXPath(getXpath('noUi-handle-lower'), {visible: true});
    const upperNumberInputHandle = await this.page.waitForXPath(getXpath('noUi-handle-upper'), {visible: true});

    const [x1, y1] = await centerPoint(lowerNumberInputHandle);
    // drag lowerHandle slider horizontally: 50 pixels to the right.
    await dragDrop(this.page, lowerNumberInputHandle, {x: x1+50, y: y1});
    const [x2, y2] = await centerPoint(upperNumberInputHandle);
    // drag upperHandle slider horizontally: 50 pixels to the left.
    await dragDrop(this.page, upperNumberInputHandle, {x: x2-50, y: y2});
  }

  async addVisits(visits: Visits[]): Promise<void> {
    for (const visit of visits) {
      await this.waitForVisitsCriteriaLink(visit).then((link) => link.click());
    }
  }

}
