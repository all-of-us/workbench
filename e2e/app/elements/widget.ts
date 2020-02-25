import {Page} from 'puppeteer';
import * as widgetxpath from './xpath-defaults';

// UI widgets finder
export default class Widget {
  public puppeteerPage: Page;

  constructor(page: Page) {
    this.puppeteerPage = page;
  }

   /**
    * Find a LINK or BUTTON element with a specified label.
    * @param {string} label Label text
    */
  public async findClickable(label: string)  {
    const selectr = widgetxpath.clickable(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find CHECKBOX element with a specified label.
    * @param {string} label
    */
  public async findCheckbox(label: string) {
    const selectr = widgetxpath.checkbox(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find BUTTON element with a specified label.
    * @param label Button label partial text
    */
  public async findButton(label: string) {
    const selectr = widgetxpath.button(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find TEXTAREA element with a specified label.
    * @param {string} label Textarea label partial text
    */
  public async findTextarea(label: string) {
    const selectr = widgetxpath.textarea(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find visible texts on the page.
    * @param {string} searchText Text to look for on page
    */
  public async findText(searchText: string) {
    const selectr = widgetxpath.textString(searchText);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find IMAGE element that is displayed next to a specified label.
    * @param {string} label Label text
    */
  public async findImage(label: string) {
    const selectr = widgetxpath.image(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true});
  }

   /**
    * Find INPUT element with a specified label.
    * @param {string} label Input label text
    */
  public async findInput(label: string) {
    const selectr = widgetxpath.textInput(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true})
  }

  /**
   * Find RADIO element with a specified label.
   * @param {string} label text
   */
  public async findRadio(label: string) {
    const selectr = widgetxpath.radio(label);
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true})
  }

}
