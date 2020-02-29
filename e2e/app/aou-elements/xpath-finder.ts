import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import * as widgetxpath from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} name
 */
export async function findClickable(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clickableXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find SELECT element with a specified label.
 * @param {string} name
 */
export async function findSelect(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.selectXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} name
 */
export async function findCheckBox(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.checkboxXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find BUTTON element with a specified label.
 * @param name: Button label partial text
 */
export async function findButton(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selectr = widgetxpath.buttonXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selectr, options);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} name: Textarea label partial text
 */
export async function findTextArea(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textareaXpath(name);
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
 * @param {string} name
 */
export async function findImage(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.imageXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} name
 */
export async function findTextBox(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.textboxXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} name
 */
export async function findRadioButton(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.radioButtonXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find plus-circle icon element with a specified label.
 * @param {string} name
 */
export async function findPlusCircleIcon(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.plusCircleIconXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}

/**
 * Find clr-icon element. A clr-icon is a link in AoU app.
 * @param page
 * @param name
 */
export async function findClrIcon(page: Page, name: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clrIconXpath(name);
  if (options === undefined) { options = {visible: true}; }
  return page.waitForXPath(selector, options)
}
