import {Page} from 'puppeteer';
import ClrIconLink from 'app/element/clr-icon-link';
import {ElementType} from 'app/xpath-options';
import Dialog from './dialog';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " crit-modal-container ")]';

export enum PhysicalMeasurementsCriteria {
  BMI = 'BMI',
  HEIGHT = 'Height',
  WEIGHT = 'Weight',
  WAIST_CIRCUMFERENCE = 'Waist Circumference',
  HIP_CIRCUMFERENCE = 'Hip Circumference',
  HEART_RATE = 'Heart Rate',
  BLOOD_PRESSURE = 'Blood Pressure',
  PREGNANT_ENROLLMENT = 'Pregnant at enrollment',
  WHEELCHAIR_USER = 'Wheelchair user at enrollment',
  BP_HYPOTENSIVE = 'Hypotensive (Systolic <= 90 / Diastolic <= 60)', // Blood Pressure
  BP_NORMAL = 'Normal (Systolic <= 120 / Diastolic <= 80)', // Blood Pressure
  BP_PREHYPERTENSIVE = 'Pre-Hypertensive (Systolic 121-139 / Diastolic 81-89)', // Blood Pressure
  BP_HYPERTENSIVE = 'Hypertensive (Systolic >= 140 / Diastolic >= 90)', // Blood Pressure
  HR_NO_IRREGULARITY = 'No-irregularity detected', // Heart Rhythm Status
  HR_IRREGULARITY = 'Irregularity detected', // Heart Rhythm Status
}


export default class CreateCriteriaModal extends Dialog {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async waitForPhysicalMeasurementCriteriaLink(criteriaType: PhysicalMeasurementsCriteria): Promise<ClrIconLink> {
    return ClrIconLink.forLabel(this.page, {type: ElementType.Icon, name: criteriaType, iconShape: 'slider'}, this);
  }

}
