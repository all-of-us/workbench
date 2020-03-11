import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import * as xpathDefaults from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label
 */
export async function findClickable(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.clickableXpath(label);
  return page.waitForXPath(selector, options);
}

/**
 * Find SELECT element with a specified label.
 * @param {string} label
 */
export async function findSelect(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 2;
  }
  const selector = `${xpathDefaults.labelXpath(textOptions)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//select`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckbox(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'checkbox';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find BUTTON element with a specified label.
 * @param label: Button label partial text
 */
export async function findButton(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.buttonXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label: Textarea label partial text
 */
export async function findTextarea(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and input element.
  // For most cases, closest parent element is two level up from label. Thus for the default value 2.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 2;
  }
  const selector = `${xpathDefaults.labelXpath(textOptions)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//textarea`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find visible text label on the page.
 * @param {string} label:
 */
export async function findLabel(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.labelXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} label
 */
export async function findTextbox(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'text';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} label
 */
export async function findRadiobutton(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'radio';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find clr-icon element with a specified label. A clr-icon is clickable link AoU app.
 * @param page
 * @param label
 */
export async function findIcon(page: Page, label: string, shape: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.clrIconXpath(label, shape);
  if (waitOptions === undefined) {
    waitOptions = {visible: true};
  }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find IMAGE element that is displayed next to a specified label.
 * @param {string} label
 */
export async function findImage(page: Page, label: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.imageXpath(label);
  if (waitOptions === undefined) {
    waitOptions = {visible: true};
  }
  return page.waitForXPath(selector, waitOptions);
}
