import Dialog, {ButtonLabel} from 'app/component/dialog';
import SelectMenu from 'app/component/select-menu';
import Table from 'app/component/table';
import ClrIconLink from 'app/element/clr-icon-link';
import Textbox from 'app/element/textbox';
import {xPathOptionToXpath} from 'app/element/xpath-defaults';
import {ElementType} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {centerPoint, dragDrop, waitWhileLoading} from 'utils/test-utils';
import {waitForNumericalString, waitForPropertyNotExists} from 'utils/waits-utils';

const defaultXpath = '//*[@class="modal-container"]';

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

export enum FilterSign {
  Any = 'Any',
  AnyValue = 'Any value',
  Equals = 'Equals',
  GreaterThanOrEqualTo = 'Greater Than or Equal To',
  LessThanOrEqualTo = 'Less Than or Equal To',
  Between = 'Between',
}

export default class CohortCriteriaModal extends Dialog {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async waitForPhysicalMeasurementCriteriaLink(criteriaType: PhysicalMeasurementsCriteria): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: criteriaType, iconShape: 'slider', ancestorLevel: 2}, this);
  }

  async waitForEthnicityCriteriaLink(criteriaType: Ethnicity): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {startsWith: criteriaType, iconShape: 'plus-circle', ancestorLevel: 0}, this);
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

    const selectMenu = await SelectMenu.findByName(this.page, {ancestorLevel: 2}, this);
    await selectMenu.clickMenuItem(filterSign);

    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, {visible: true});
    await numberField.type(String(filterValue));

    await this.clickButton(ButtonLabel.Calculate);
    const participantResult = await this.waitForParticipantResult();
    console.debug(`Physical Measurements ${criteriaName}: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);

    // Find criteria in Selected Criteria Content Box.
    const removeSelectedCriteriaIconSelector = xPathOptionToXpath({type: ElementType.Icon, iconShape: 'times-circle'}, this);
    // Before add criteria, first check for nothing in Selected Criteria Content Box.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{hidden: true});

    await this.clickButton(ButtonLabel.AddThis);
    // After add criteria, look for X (remove) icon for indication that add succeeded.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{visible: true});

    // dialog close after click FINISH button.
    await this.clickFinishButton();

    // return participants count for comparing
    return participantResult;
  }

  // Click FINISH button.
  async clickFinishButton(): Promise<void> {
    return this.clickButton(ButtonLabel.Finish);
  }

  async waitForParticipantResult(): Promise<string> {
    const selector = `${this.xpath}//*[./*[contains(text(), "Results")]]/div[contains(text(), "Number")]`;
    return waitForNumericalString(this.page, selector);
  }

  async getConditionSearchResultsTable(): Promise<Table> {
    return new Table(this.page, '//table[@class="p-datatable"]', this);
  }

  async searchCondition(searchWord: string): Promise<Table> {
    const searchFilterTextbox = await Textbox.findByName(this.page, {containsText: 'by code or description'}, this);
    await searchFilterTextbox.type(searchWord);
    await searchFilterTextbox.pressReturnKey();
    await waitWhileLoading(this.page);
    return this.getConditionSearchResultsTable();
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
    await this.clickButton(ButtonLabel.Calculate);
    try {
      participantResult = await this.waitForParticipantResult();
    } catch (e) {
      // Retry one more time.
      await this.clickButton(ButtonLabel.Calculate);
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
    await (Textbox.asBaseElement(this.page, lowerNumberInput)).type(minAge.toString()).then(input => input.tabKey());
    await (Textbox.asBaseElement(this.page, upperNumberInput)).type(maxAge.toString()).then(input => input.tabKey());

    // Click Calculate button.
    const button = await this.waitForButton(ButtonLabel.Calculate);
    await waitForPropertyNotExists(this.page, button.getXpath(), 'disabled');
    await button.click();

    const calcuatedResult = await this.waitForParticipantResult();
    console.log(`Age min: ${minAge}, max: ${maxAge} ==> number of participants: ${calcuatedResult}`);

    // Click FINISH button. Dialog should close.
    await this.clickFinishButton();
    return calcuatedResult;
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


}
