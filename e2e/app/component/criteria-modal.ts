import {ElementHandle} from 'puppeteer';
import {contains} from 'utils/str-utils';
import Button from 'app/element/button';
import {clrIconXpath} from 'app/element/xpath-defaults';
import Container from './container';
import SelectMenu from './select-menu';


export enum ButtonLabel {
  Cancel = 'Cancel',
  Calculate = 'Calculate',
  AddThis = 'ADD THIS',
  Finish = 'Finish',
}

export enum CriteriaType {
  BMI = 'BMI',
  HEIGHT = 'Height',
  WEIGHT = 'Weight',
  WAIST_CIRCUMFERENCE = 'Waist Circumference',
  HIP_CIRCUMFERENCE = 'Hip Circumference',
}

export enum CalculationType {
  ANY_VALUE = 'Any value',
  EQUALS = 'Equals',
  GREATER_EQUAL_TO = 'Greater than or Equal to',
  LESS_EQUAL_TO = 'Less than or Equal to',
  BETWEEN = 'Between',
}


export default class CriteriaModal extends Container {

  constructor(page, selector?) {
    selector = selector ||  {xpath: '//*[contains(@class,"crit-modal-container")]'};
    super(page, selector);
  }

  async isVisible(): Promise<boolean> {
    await this.findElement();
    const classNames = await (await this.elementHandle.getProperty('className')).jsonValue();
    return contains(classNames.toString(), 'show');
  }

  async selectCalculatetableCriteria(criteriaType: CriteriaType, calcType: CalculationType, filterValue: number): Promise<void> {
    // modal screen 1 of 2: select a criteria
    const link = await this.criteriaLink(criteriaType);
    await link.click();

    // modal screen 2 of 2:
    const selectMenu = new SelectMenu(this.page, {nodeLevel: 2});
    await selectMenu.select(calcType);

    await this.page.waitForXPath('.//input[@type="number"]');
    const numberField = (await this.elementHandle.$x('.//input[@type="number"]'))[0];
    await numberField.type('30'); // BMI 30 or more is obesity
    await this.clickCalculateButton();
    const results = await this.getResults();
    console.log(`Select ${criteriaType} => ${calcType} ${filterValue}: Calcuated number of participants are: ${results}`);
    await this.clickAddThisButton();

    // Find criteria in Selected Criteria Content Box
    const selector = `.//small[@class="name" and contains(normalize-space(),"${this.selectedCriteriaString(criteriaType, calcType, filterValue)}")]`;
    const selectedCriteriaElement = await this.findDescendantElements({xpath: selector});
    await selectedCriteriaElement[0].hover();
  }

  selectedCriteriaString(criteriaType: CriteriaType, calcType: CalculationType, filterValue: number): string {
    let formattedString: string;
    switch (calcType) {
    case CalculationType.ANY_VALUE:
      return `${criteriaType} (${this.convertToMathSign(calcType)})`;
    }
    switch (criteriaType) {
    case CriteriaType.BMI:
      formattedString = `${criteriaType} (${this.convertToMathSign(calcType)} ${filterValue})`;
      break;
    case CriteriaType.HEIGHT:
      formattedString = `${criteriaType} (${this.convertToMathSign(calcType)} ${filterValue}cm)`;
      break;
    case CriteriaType.HIP_CIRCUMFERENCE:
      formattedString = `${criteriaType} (${this.convertToMathSign(calcType)} ${filterValue}cm)`;
      break;
    case CriteriaType.WAIST_CIRCUMFERENCE:
      formattedString = `${criteriaType} (${this.convertToMathSign(calcType)} ${filterValue}cm)`;
      break;
    case CriteriaType.WEIGHT:
      formattedString = `${criteriaType} (${this.convertToMathSign(calcType)} ${filterValue}kg)`;
      break;
    }
    return formattedString;
  }

  async criteriaLink(criteriaType: CriteriaType): Promise<ElementHandle> {
    const iconXpath = clrIconXpath({text: criteriaType}, 'slider');
    // modal is the parent container, appending iconXpath to modal selector.
    const selector = this.selector.xpath + iconXpath;
    return await this.page.waitForXPath(selector, {visible: true});
  }

  async clickCalculateButton() {
    const calcButton = await Button.forLabel(this.page, {normalizeSpace: 'Calculate'} );
    await calcButton.waitUntilEnabled();
    await calcButton.click();
    // await this.waitUntilNoSpinner(); // Need to move waitUntilNoSpinner() to page-wait.ts
  }

  async clickAddThisButton() {
    const calcButton = await Button.forLabel(this.page, {normalizeSpace: 'ADD THIS'} );
    await calcButton.waitUntilEnabled();
    await calcButton.click();
  }

  async clickFinishButton()  {
    // './/button[normalize-space(text())="Finish"]'
    const finishButton = await Button.forLabel(this.page, {normalizeSpace: 'Finish'} );
    await finishButton.waitUntilEnabled();
    await finishButton.click();
  }

  async clickCancelButton()  {
    const finishButton = await Button.forLabel(this.page, {normalizeSpace: 'Cancel'} );
    await finishButton.click();
  }

  async getResults(): Promise<number> {
    const selector = './/*[text()="Results"]/parent::*//span';
    await this.getElementHandle().$x(selector);
    const matchesArray =  await this.page.waitForFunction( xpath => {
      const element = document.evaluate(
         xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return element.textContent.match(/^\d+$/); // Match only numbers: one or more numbers
    }, {}, selector);
    const results = await matchesArray.jsonValue();
    return Number(results);
  }


  // @ts-ignore
  private modalXpath(includeVisibility: boolean = false) {
    if (includeVisibility) {
      return './/*[contains(@class,"crit-modal-container") and contains(@class,"show")]';
    }
    return './/*[contains(@class,"crit-modal-container")]';
  }

  private convertToMathSign(calcType: CalculationType) {
    let sign: string;
    switch (calcType) {
    case CalculationType.EQUALS:
      sign = '=';
      break;
    case CalculationType.GREATER_EQUAL_TO:
      sign = '>=';
      break;
    case CalculationType.LESS_EQUAL_TO:
      sign = '<=';
      break;
    case CalculationType.ANY_VALUE:
      sign = 'Any value';
      break;
    default:
      throw new Error(`${calcType} case is NOT handled.`);
    }
    return sign;
  }

}
