import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import * as widgetxpath from './xpath-defaults';

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label
 */
export async function findClickable(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clickableXpath(label);
  return page.waitForXPath(selector, options);
}

/**
 * Find SELECT element with a specified label.
 * @param {string} label
 */
export async function findSelect(page: Page, opts?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (opts.ancestorNodeLevel === undefined) {
    opts.ancestorNodeLevel = 2;
  }
  const selector = `${widgetxpath.labelXpath(opts)}/ancestor::node()[${opts.ancestorNodeLevel}]//select`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckbox(page: Page, opts?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (opts.ancestorNodeLevel === undefined) {
    opts.ancestorNodeLevel = 1;
  }
  opts.inputType = 'checkbox';
  const selector = `${widgetxpath.inputXpath(opts)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find BUTTON element with a specified label.
 * @param label: Button label partial text
 */
export async function findButton(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.buttonXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label: Textarea label partial text
 */
export async function findTextarea(page: Page, opts?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (opts.ancestorNodeLevel === undefined) {
    opts.ancestorNodeLevel = 2;
  }
  const selector = `${widgetxpath.labelXpath(opts)}/ancestor::node()[${opts.ancestorNodeLevel}]//textarea`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find visible text label on the page.
 * @param {string} label:
 */
export async function findLabel(page: Page, textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.labelXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} label
 */
export async function findTextbox(page: Page, opts?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (opts.ancestorNodeLevel === undefined) {
    opts.ancestorNodeLevel = 1;
  }
  opts.inputType = 'text';
  const selector = `${widgetxpath.inputXpath(opts)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} label
 */
export async function findRadiobutton(page: Page, opts?: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  if (opts.ancestorNodeLevel === undefined) {
    opts.ancestorNodeLevel = 1;
  }
  opts.inputType = 'radio';
  const selector = `${widgetxpath.inputXpath(opts)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find clr-icon element with a specified label. A clr-icon is clickable link AoU app.
 * @param page
 * @param label
 */
export async function findIcon(page: Page, label: string, shape: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = widgetxpath.clrIconXpath(label, shape);
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
