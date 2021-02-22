import * as fp from 'lodash/fp';
import {ElementHandle, Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';
import {waitWhileLoading} from 'utils/waits-utils';
import Container from 'app/container';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import {LinkText} from 'app/text-labels';

const defaultXpath = '//*[@id="popup-root"]//*[@role="dialog" and contains(@class, "after-open")]';

export default abstract class BaseModal extends Container {

   protected constructor(page: Page, xpath: string = defaultXpath) {
      super(page, xpath);
   }

   /**
    * Method to be implemented by children classes.
    * Check whether current page has specified web elements.
    */
   abstract isLoaded(): Promise<boolean>

   async waitForLoad(): Promise<this> {
      await this.waitUntilVisible();
      await waitWhileLoading(this.page);
      await this.isLoaded();
      await this.page.waitForTimeout(1000);
      return this;
   }

   async getTextContent(): Promise<string[]> {
      // xpath that excludes button labels and spans
      const selector = `${this.getXpath()}//*[normalize-space(text()) and not(@role="button")]`;
      await this.waitUntilVisible();
      await this.page.waitForXPath(selector, {visible: true});
      const elements: ElementHandle[] = await this.page.$x(selector);
      return fp.flow(
         fp.map(async (elem: ElementHandle) => (await getPropValue<string>(elem, 'textContent')).trim()),
         contents => Promise.all(contents)
      )(elements);
   }

   /**
    * Click a button.
    * @param {string} buttonLabel The button text label.
    * @param waitOptions Wait for navigation or/and modal close after click button.
    */
   async clickButton(buttonLabel: LinkText,
                     waitOptions: { waitForNav?: boolean, waitForClose?: boolean, timeout?: number } = {}): Promise<void> {
      const {waitForNav = false, waitForClose = false, timeout} = waitOptions;
      const button = await this.waitForButton(buttonLabel);
      await button.waitUntilEnabled();
      await button.focus();
      const handle = await button.asElementHandle();
      await handle.hover();
      if (waitForNav) {
         const navPromise = this.page.waitForNavigation({waitUntil: ['load', 'networkidle0']});
         await button.click({delay: 10});
         await waitWhileLoading(this.page);
         await navPromise;
      } else {
         await button.click({delay: 10});
         await waitWhileLoading(this.page);
      }
      if (waitForClose) {
         await this.waitUntilClose(timeout);
      }
   }

   async waitForButton(buttonLabel: LinkText): Promise<Button> {
      return Button.findByName(this.page, {normalizeSpace: buttonLabel}, this);
   }

   async waitForTextbox(textboxName: string): Promise<Textbox> {
      return Textbox.findByName(this.page, {name: textboxName}, this);
   }

   async waitForTextarea(textareaName: string): Promise<Textarea> {
      return Textarea.findByName(this.page, {name: textareaName}, this);
   }

   async waitForCheckbox(checkboxName: string): Promise<Checkbox> {
      return Checkbox.findByName(this.page, {name: checkboxName}, this);
   }

   async waitUntilClose(timeout: number = 60000): Promise<void> {
      await this.page.waitForXPath(this.xpath, {hidden: true, timeout});
   }

   /**
    * Returns true if Dialog exists on page.
    */
   async exists(): Promise<boolean> {
      return (await this.page.$x(this.xpath)).length > 0;
   }

}