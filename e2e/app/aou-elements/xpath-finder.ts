import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import * as widgetxpath from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label Label text
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
export async function findSelect(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.selectXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckBox(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.checkboxXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find BUTTON element with a specified label.
 * @param label Button label partial text
 */
export async function findButton(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selectr = widgetxpath.buttonXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selectr, options);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label Textarea label partial text
 */
export async function findTextArea(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textareaXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find visible texts on the page.
 * @param {string} searchText Text to look for on page
 */
export async function findText(page: Page, searchText: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textXpath(searchText);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find IMAGE element that is displayed next to a specified label.
 * @param {string} label Label text
 */
export async function findImage(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.imageXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} label Input label text
 */
export async function findTextBox(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textboxXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} label text
 */
export async function findRadioButton(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.radioButtonXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find plus-circle icon element with a specified label.
 * @param {string} label text
 */
export async function findPlusCircleIcon(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.plusCircleIconXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find clr-icon element. A clr-icon is a link in AoU app.
 * @param page
 * @param label
 */
export async function findClrIcon(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clrIconXpath(label);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}
