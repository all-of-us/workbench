import { ClickOptions, ElementHandle, Page, WaitForSelectorOptions } from 'puppeteer';
import Container from 'app/container';
import { getAttrValue, getPropValue } from 'utils/element-utils';
import { logger } from 'libs/logger';

/**
 * BaseElement represents a web element in the DOM.
 * It implements useful methods for querying and interacting with this element.
 */
export default class BaseElement extends Container {
  static asBaseElement(page: Page, elementHandle: ElementHandle, xpath?: string): BaseElement {
    const baseElement = new BaseElement(page, xpath);
    baseElement.setElementHandle(elementHandle);
    return baseElement;
  }

  private element: ElementHandle;

  constructor(protected readonly page: Page, protected readonly xpath?: string) {
    super(page, xpath);
  }

  protected setElementHandle(element: ElementHandle): void {
    this.element = element;
  }

  /**
   * Find first element matching xpath selector.
   * If there is no element matching xpath selector, null is returned.
   * @param {WaitForSelectorOptions} waitOptions
   */
  async waitForXPath(waitOptions: WaitForSelectorOptions = { visible: true }): Promise<ElementHandle> {
    if (this.xpath === undefined && this.element !== undefined) return this.element.asElement();
    return this.page
      .waitForXPath(this.xpath, waitOptions)
      .then((elemt) => (this.element = elemt.asElement()))
      .catch((err) => {
        logger.error(`waitForXpath('${this.xpath}') failed`);
        logger.error(err);
        logger.error(err.stack);
        // Debugging pause
        // await jestPuppeteer.debug();
        throw new Error(err);
      });
  }

  /**
   * Find all elements matching xpath selector.
   */
  async findAllElements(): Promise<ElementHandle[]> {
    return this.page.$x(this.xpath);
  }

  /**
   * Find descendant elements matching xpath selector.
   * @param {string} descendantXpath Be sure to begin xpath with a dot. e.g. ".//div".
   */
  async findDescendant(descendantXpath: string): Promise<ElementHandle[]> {
    return this.asElementHandle().then((elemt) => {
      return elemt.$x(descendantXpath);
    });
  }

  /**
   * Finds the value of a property for this element.
   *
   * Alternative:
   *  const handle = await page.evaluateHandle((elem, prop) => {
   *    return elem[prop];
   *  }, element, property);
   *  return (await handle.jsonValue()).toString();
   */
  async getProperty<T>(propertyName: string): Promise<T> {
    return this.asElementHandle().then((element) => {
      return getPropValue<T>(element, propertyName);
    });
  }

  /**
   * Finds the value of an attribute
   * @param attribute name
   */
  async getAttribute(attributeName: string): Promise<string> {
    return this.asElementHandle().then((element) => {
      return getAttrValue(this.page, element, attributeName);
    });
  }

  /**
   * Does attribute exists for this element?
   *
   * @param attribute name
   */
  async hasAttribute(attributeName: string): Promise<boolean> {
    return this.getAttribute(attributeName).then((value) => {
      return value !== null;
    });
  }

  /**
   * Is element disabled or readonly?
   * Disabled means element has `disabled` attribute but without a value.
   */
  async isDisabled(): Promise<boolean> {
    return this.getProperty<boolean>('disabled');
  }

  /**
   * <pre>
   *  Check if the element is visible
   * </pre>
   * @param {Page} page
   * @param {ElementHandle} element
   */
  async isVisible(): Promise<boolean> {
    return this.asElementHandle()
      .then((elemt) => {
        return elemt.boxModel();
      })
      .then((box) => {
        return box !== null;
      });
  }

  /**
   * Check both boxModel and style for visibility.
   */
  async isDisplayed(): Promise<boolean> {
    const elemt = await this.asElementHandle();
    const isVisibleHandle = await this.page.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style && style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
    }, elemt);
    const jValue = await isVisibleHandle.jsonValue();
    const boxModelValue = await elemt.boxModel();
    return jValue && boxModelValue !== null;
  }

  async click(options?: ClickOptions): Promise<void> {
    return this.asElementHandle().then(async (element) => {
      // Click workaround: Wait for x,y to stop changing within specified time
      const startTime = Date.now();
      let previousX: number;
      let previousY: number;
      while (Date.now() - startTime < 30 * 1000) {
        const viewport = await element.isIntersectingViewport();
        if (viewport) {
          const box = await element.boundingBox();
          const x = box.x + box.width / 2;
          const y = box.y + box.height / 2;
          if (previousX !== undefined && previousY !== undefined) {
            // tslint:disable:triple-equals
            if (
              parseFloat(previousX.toFixed(7)) === parseFloat(x.toFixed(7)) &&
              parseFloat(previousY.toFixed(7)) === parseFloat(y.toFixed(7))
            ) {
              break;
            }
          }
          previousX = x;
          previousY = y;
        }
        await element.hover();
        await this.page.waitForTimeout(200);
      }
      return element.click(options);
    });
  }

  /**
   * Clear existing value in textbox then type new text value.
   * @param textValue The text string.
   * @param options The typing options.
   */
  async type(textValue: string, options: { delay?: number } = {}): Promise<this> {
    const { delay = 10 } = options;

    const clearAndType = async (txt: string): Promise<string> => {
      await this.clear();
      await this.asElementHandle().then((handle: ElementHandle) => handle.type(txt, { delay }));
      return this.getProperty<string>('value');
    };

    let maxRetries = 2;
    const typeAndCheck = async () => {
      const actualValue = await clearAndType(textValue);
      if (actualValue === textValue) {
        await this.pressTab();
        return; // success
      }
      if (maxRetries <= 0) {
        throw new Error(`BaseElement.type("${textValue}") failed. Actual text: "${actualValue}"`);
      }
      maxRetries--;
      await this.page.waitForTimeout(1000).then(typeAndCheck); // one second pause and retry type
    };

    await typeAndCheck();
    return this;
  }

  async pressKeyboard(key: string, options?: { text?: string; delay?: number }): Promise<void> {
    return this.asElementHandle().then((elemt) => {
      return elemt.press(key, options);
    });
  }

  async pressReturn(): Promise<void> {
    return this.pressKeyboard(String.fromCharCode(13));
  }

  /**
   * Press keyboard "tab".
   */
  async pressTab(): Promise<void> {
    return this.pressKeyboard('Tab', { delay: 100 });
  }

  /**
   * Clear value in textbox.
   */
  async clear(options: ClickOptions = { clickCount: 3 }): Promise<void> {
    const elemt = await this.asElementHandle();
    await elemt.focus();
    await elemt.hover();
    await elemt.click(options);
    await this.page.keyboard.press('Backspace');
  }

  async clearTextInput(): Promise<void> {
    return this.asElementHandle().then((element) => {
      return this.page.evaluate(
        (elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')
            .set;
          nativeInputValueSetter.call(elemt, textValue);
          const event = new Event('input', { bubbles: true });
          elemt.dispatchEvent(event);
        },
        element,
        ''
      );
    });
  }

  /**
   * Calling focus() and hover() together.
   */
  async focus(): Promise<void> {
    const element = await this.asElementHandle();
    await Promise.all([element.focus(), element.hover()]);
  }

  /**
   * <pre>
   * Get the textContent property value for a element.
   * </pre>
   */
  async getTextContent(): Promise<string> {
    return this.asElementHandle().then((elemt) => {
      return this.page.evaluate(
        (element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''),
        elemt
      );
    });
  }

  /**
   * Get the value of property 'value' for this element.
   * Alternative: await page.evaluate(elem => elem.value, element);
   */
  async getValue(): Promise<string> {
    return this.getProperty<string>('value');
  }

  async getComputedStyle(styleName: string): Promise<string> {
    const handle = await this.asElementHandle();
    const attrStyle = await handle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style;
    }, this.element);
    const propValue = await attrStyle.getProperty(styleName);
    return (await propValue.jsonValue()).toString();
  }

  /**
   * Determine if cursor is disabled (== " not-allowed ") by checking style 'cursor' value.
   */
  async isCursorNotAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return !!cursor && cursor === 'not-allowed';
  }

  /**
   * Finds visible element's bounding box size.
   */
  async getSize(): Promise<{ width: number; height: number }> {
    const box = await this.asElementHandle().then((elemt) => {
      return elemt.boundingBox();
    });
    if (box === null) {
      // if element is not visible, returns size of (0, 0).
      return { width: 0, height: 0 };
    }
    const { width, height } = box;
    return { width, height };
  }

  async dispose(): Promise<void> {
    return this.element.dispose();
  }

  // try this method when click() is not working
  async clickWithEval(): Promise<void> {
    const element = await this.asElementHandle();
    await this.page.evaluate((elem) => elem.click(), element);
  }

  /**
   * Click on element then wait for page navigation to finish.
   */
  async clickAndWait(timeout: number = 2 * 60 * 1000): Promise<void> {
    await Promise.all([
      this.page.waitForNavigation({
        waitUntil: ['load', 'domcontentloaded', 'networkidle0'],
        timeout
      }),
      this.click({ delay: 10 })
    ]).catch((err) => {
      logger.error('clickAndWait() failed.');
      logger.error(err);
      logger.error(err.stack);
      throw new Error(err);
    });
  }

  /**
   * Paste texts in textarea instead type one char at a time. Very fast.
   * @param text
   */
  async paste(text: string): Promise<void> {
    return this.asElementHandle().then((element) => {
      return this.page.evaluate(
        (elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value')
            .set;
          nativeInputValueSetter.call(elemt, textValue);
          const event = new Event('input', { bubbles: true });
          elemt.dispatchEvent(event);
        },
        element,
        text
      );
    });
  }

  /**
   * Clear texts in textarea.
   */
  async clearTextArea(): Promise<void> {
    return this.paste('');
  }

  /**
   * Returns ElementHandle.
   */
  async asElementHandle(): Promise<ElementHandle> {
    return this.waitForXPath();
  }
}
