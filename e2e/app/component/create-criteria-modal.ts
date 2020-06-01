import ClrIconLink from 'app/element/clr-icon-link';
import {ElementType} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {waitForNumericalString} from 'utils/waits-utils';
import {xPathOptionToXpath} from 'app/element/xpath-defaults';
import Dialog, {ButtonLabel} from './dialog';
import SelectMenu from './select-menu';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " crit-modal-container ")]';

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

export enum FilterSign {
  AnyValue = 'Any value',
  Equals = 'Equals',
  GreaterThanOrEqualTo = 'Greater than or Equal to',
  LessThanOrEqualTo = 'Less than or Equal to',
  Between = 'Between',
}

export default class CreateCriteriaModal extends Dialog {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async waitForPhysicalMeasurementCriteriaLink(criteriaType: PhysicalMeasurementsCriteria): Promise<ClrIconLink> {
    return ClrIconLink.forLabel(this.page, {type: ElementType.Icon, name: criteriaType, iconShape: 'slider', ancestorLevel: 2}, this);
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
     filterValue: number): Promise<string> {

    const link = await this.waitForPhysicalMeasurementCriteriaLink(criteriaName);
    await link.click();

    const selectMenu = await SelectMenu.forLabel(this.page, {ancestorLevel: 2});
    await selectMenu.clickMenuItem(filterSign);

    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, {visible: true});
    await numberField.type('30');

    await this.clickButton(ButtonLabel.Calculate);
    const participantResult = await this.getParticipantsResult();
    console.debug(`${criteriaName}: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);

    // Find criteria in Selected Criteria Content Box.
    const removeSelectedCriteriaIconSelector = xPathOptionToXpath({type: ElementType.Icon, iconShape: 'times-circle'}, this);
    // Before add criteria, first check for nothing in Selected Criteria Content Box.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{hidden: true});

    await this.clickButton(ButtonLabel.AddThis);
    // After add criteria, look for X (remove) icon for indication that add succeeded.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{visible: true});

    // dialog close after click FINISH button.
    await this.clickButton(ButtonLabel.Finish);

    // return participants count for comparing
    return participantResult;
  }

  async getParticipantsResult(): Promise<string> {
    const selector = `${this.xpath}//*[text()="Results"]/parent::*//span`;
    return waitForNumericalString(this.page, selector);
  }


}
