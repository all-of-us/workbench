import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import Container from 'app/container';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import { LinkText } from 'app/text-labels';
import { logger } from 'libs/logger';

const modalRootXpath = '//*[@id="popup-root"]/*[@class="ReactModalPortal"]';
const modalXpath = '//*[@role="dialog" and @aria-modal="true" and contains(@class, "after-open")]';

export default abstract class BaseModal extends Container {
  private title: string;

  protected constructor(page: Page, opts: { xpath?: string; modalIndex?: number } = { modalIndex: 1 }) {
    super(page, opts.xpath ? opts.xpath : `${modalRootXpath}[${opts.modalIndex}]${modalXpath}`);
  }

  /**
   * Method to be implemented by children classes.
   * Check whether current page has specified web elements.
   */
  abstract isLoaded(): Promise<boolean>;

  async waitForLoad(): Promise<this> {
    await this.waitUntilVisible();
    await this.isLoaded();
    logger.info(`Modal "${await this.getTitle()}" is open`);
    await waitWhileLoading(this.page);
    return this;
  }

  async getTextContent(): Promise<string[]> {
    // xpath that excludes button labels and spans
    const selector =
      this.getXpath() + '//*[normalize-space(.) and not(@role="button") and not(self::b) and not(self::i)]';
    await this.page.waitForXPath(selector, { visible: true });
    const elements: ElementHandle[] = await this.page.$x(selector);
    return fp.flow(
      fp.map(async (elem: ElementHandle) => (await getPropValue<string>(elem, 'innerText')).trim()),
      (contents) => Promise.all(contents)
    )(elements);
  }

  async getTitle(): Promise<string> {
    if (this.title === undefined) {
      try {
        this.title = (await this.getTextContent())[0];
      } catch (error) {
        this.title = '';
      }
    }
    return this.title;
  }

  waitForButton(buttonLabel: LinkText): Button {
    return Button.findByName(this.page, { normalizeSpace: buttonLabel }, this);
  }

  waitForTextbox(textboxName: string): Textbox {
    return Textbox.findByName(this.page, { name: textboxName }, this);
  }

  waitForTextarea(textareaName: string): Textarea {
    return Textarea.findByName(this.page, { name: textareaName }, this);
  }

  waitForCheckbox(checkboxName: string): Checkbox {
    return Checkbox.findByName(this.page, { name: checkboxName }, this);
  }

  /**
   * Returns true if Dialog exists on page.
   */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

  /**
   * Override waitUntilClose in parent class Container.
   * waitForXPath(this.getXpath(), { hidden: true, visible: false, timeout }) does not work consistently for modal.
   * when there are two or more visible modals and the modal to check on visibility is not the active (in front) modal.
   * Workaround: Check element's offsetWidth and offsetHeight are greater than 0.
   * @param timeout
   */
  async waitUntilClose(timeout = 2 * 60 * 1000): Promise<void> {
    const modalTitle = await this.getTitle();
    try {
      await this.page.waitForFunction(
        (xpath) => {
          const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue as HTMLElement;
          const isVisible = element && (element.offsetWidth > 0 || element.offsetHeight > 0);
          return !isVisible;
        },
        { timeout },
        this.getXpath()
      );
      logger.info(`Modal "${modalTitle}" is closed.`);
    } catch (err) {
      logger.error(`FAIL: WaitUntilClose failed for modal "${modalTitle}". Modal xpath: "${this.getXpath()}"`);
      logger.error(err.stack);
      throw new Error(err);
    }
  }
}
