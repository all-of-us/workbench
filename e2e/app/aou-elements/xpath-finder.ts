import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import * as widgetxpath from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label
 */
export async function findClickable(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clickableXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find SELECT element with a specified label.
 * @param {string} label
 */
export async function findSelect(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.selectXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckBox(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.checkBoxXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find BUTTON element with a specified label.
 * @param label: Button label partial text
 */
export async function findButton(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selectr = widgetxpath.buttonXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selectr, waitOptions);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label: Textarea label partial text
 */
export async function findTextArea(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textAreaXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find visible texts on the page.
 * @param {string} searchText Text to look for on page
 */
export async function findText(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find visible text label on the page.
 * @param {string} label:
 */
export async function findLabel(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.labelXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find IMAGE element that is displayed next to a specified label.
 * @param {string} label
 */
export async function findImage(page: Page, label: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.imageXpath(label);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} label
 */
export async function findTextBox(page: Page, textOptions?: TextOptions, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textBoxXpath(textOptions);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} label
 */
export async function findRadioButton(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.radioButtonXpath(textOptions);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions)
}

/**
 * Find clr-icon element with a specified label. A clr-icon is clickable link AoU app.
 * @param page
 * @param label
 */
export async function findIcon(page: Page, label: string, shape: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.iconXpath(label, shape);
  if (waitOptions === undefined) { waitOptions = {visible: true}; }
  return page.waitForXPath(selector, waitOptions)
}
