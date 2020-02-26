import {ElementHandle, Page} from 'puppeteer';
import * as widgetxpath from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label Label text
 */
export async function findClickable(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.clickableXpath(label);
  return page.waitForXPath(selector, {visible: true});
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckbox(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.checkboxXpath(label);
  return page.waitForXPath(selector, {visible: true});
}

/**
 * Find BUTTON element with a specified label.
 * @param label Button label partial text
 */
export async function findButton(page: Page, label: string): Promise<ElementHandle> {
  const selectr = widgetxpath.buttonXpath(label);
  return page.waitForXPath(selectr, {visible: true});
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label Textarea label partial text
 */
export async function findTextarea(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.textareaXpath(label);
  return page.waitForXPath(selector, {visible: true});
}

/**
 * Find visible texts on the page.
 * @param {string} searchText Text to look for on page
 */
export async function findText(page: Page, searchText: string): Promise<ElementHandle> {
  const selector = widgetxpath.textXpath(searchText);
  return page.waitForXPath(selector, {visible: true});
}

/**
 * Find IMAGE element that is displayed next to a specified label.
 * @param {string} label Label text
 */
export async function findImage(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.imageXpath(label);
  return page.waitForXPath(selector, {visible: true});
}

/**
 * Find INPUT element with a specified label.
 * @param {string} label Input label text
 */
export async function findTextbox(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.textboxXpath(label);
  return page.waitForXPath(selector, {visible: true})
}

/**
 * Find RADIO element with a specified label.
 * @param {string} label text
 */
export async function findRadioButton(page: Page, label: string): Promise<ElementHandle> {
  const selector = widgetxpath.radioButtonXpath(label);
  return page.waitForXPath(selector, {visible: true})
}
