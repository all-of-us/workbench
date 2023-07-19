import {
  BoxModel,
  ClickOptions,
  ElementHandle,
  JSHandle,
  NavigationOptions,
  Page,
  WaitForSelectorOptions
} from 'puppeteer';
import { exists, getAttrValue, getPropValue, getStyleValue } from 'utils/element-utils';
import { logger } from 'libs/logger';
import { waitForFn, waitWhileLoading } from 'utils/waits-utils';

/**
 * BaseElement represents a web element in the DOM.
 * It implements useful methods for querying and interacting with this element.
 */
export default class BaseElement {
  static asBaseElement(page: Page, elementHandle: ElementHandle, xpath?: string): BaseElement {
    const baseElement = new BaseElement(page, xpath);
    baseElement.setElementHandle(elementHandle);
    return baseElement;
  }

  private element: ElementHandle;

  constructor(protected readonly page: Page, protected readonly xpath?: string) {}

  protected setElementHandle(element: ElementHandle): void {
    this.element = element;
  }

  /**
   * Find first element matching xpath selector.
   * If there is no element matching xpath selector, null is returned.
   * @param {WaitForSelectorOptions} waitOptions
   */
  async waitForXPath(waitOptions: WaitForSelectorOptions = { visible: true }): Promise<ElementHandle> {
    if (this.getXpath() === undefined && this.element !== undefined) return this.element.asElement();
    return this.page
      .waitForXPath(this.xpath, waitOptions)
      .then((element) => {
        return (this.element = element.asElement());
      })
      .catch((err) => {
        logger.error(`waitForXpath('${this.getXpath()}') failed`);
        logger.error(err);
        logger.error(err.stack);
        // Debugging pause
        // await jestPuppeteer.debug();
        throw new Error(err);
      });
  }

  getXpath(): string {
    return this.xpath;
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
    return this.asElementHandle().then((element) => {
      return element.$x(descendantXpath);
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
   * @param attributeName name
   */
  async getAttribute(attributeName: string): Promise<string> {
    return this.asElementHandle().then((element) => {
      return getAttrValue(this.page, element, attributeName);
    });
  }

  /**
   * Does attribute exists for this element?
   *
   * @param attributeName name
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
   *  Check both boxModel and style for visibility to determine whether element is visible.
   * </pre>
   */
  async isVisible(): Promise<boolean> {
    const isDisplayed = async (element: ElementHandle): Promise<boolean> => {
      const jsHandle: JSHandle = await this.page
        .evaluateHandle((elem) => {
          const style = window.getComputedStyle(elem);
          return style && style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
        }, element)
        .catch(() => {
          return null;
        });
      return (await jsHandle.jsonValue()) as boolean;
    };

    const boxModel = async (element: ElementHandle): Promise<BoxModel | null> => {
      const boxModel = await element.boxModel();
      return boxModel;
    };

    return this.asElementHandle()
      .then(async (element) => {
        return (await isDisplayed(element)) && (await boxModel(element)) !== null;
      })
      .catch(() => false);
  }

  async click(options?: ClickOptions): Promise<void> {
    await this.asElementHandle().then(async (element) => {
      // Click workaround: Wait for (x,y) to stop changing within specified time
      const startTime = Date.now();
      let previousX: number;
      let previousY: number;
      while (Date.now() - startTime < 30 * 1000) {
        const visible = await this.isVisible();
        if (!visible) {
          // Scrolls element into viewport if needed.
          await element.hover();
          await element.focus();
          await this.page.waitForTimeout(1000);
        }
        const boundingBox = await element.boundingBox();
        const boxModel = await element.boxModel();
        if (boundingBox !== null && boxModel !== null) {
          const x = boundingBox.x + boundingBox.width / 2;
          const y = boundingBox.y + boundingBox.height / 2;
          if (previousX !== undefined && previousY !== undefined) {
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
        // Half second sleep before getting values of (x, y) again.
        await this.page.waitForTimeout(500);
      }
      // End the test if previousX or previousY is still undefined after waiting.
      if (previousX === undefined || previousY === undefined) {
        throw new Error(`Failed to click element because it is not visible. xpath: ${this.getXpath()}`);
      }
      await element.click(options);
    });
  }

  /**
   * Clear existing value in textbox then type new text value.
   * @param newValue The text string.
   * @param options The typing options.
   */
  async type(newValue: string, { delay = 10, confidence = 2 } = {}): Promise<this> {
    if (newValue === undefined) {
      throw new Error('type() function parameter "newValue" is undefined.');
    }

    const clearAndType = async (txt: string): Promise<string> => {
      await this.clear();
      await this.asElementHandle().then((handle: ElementHandle) => handle.type(txt, { delay }));
      await this.pressTab();
      return this.getProperty<string>('value');
    };

    let maxRetries = 4;
    let confidenceCounter = 0;
    const typeAndCheck = async () => {
      let actualValue = await this.getProperty<string>('value');
      if (actualValue !== newValue) {
        actualValue = await clearAndType(newValue);
      }
      if (actualValue === newValue) {
        confidenceCounter++;
        if (confidenceCounter >= confidence) {
          return; // success
        }
      } else {
        confidenceCounter = confidenceCounter > 0 ? confidenceCounter-- : 0;
      }
      if (maxRetries <= 0) {
        throw new Error(`Failed to type "${newValue}". Actual text: "${actualValue}"`);
      }
      maxRetries--;
      await this.page.waitForTimeout(1000).then(typeAndCheck); // 1 second pause and retry type
    };
    await waitForFn(() => this.isVisible());
    await typeAndCheck();
    return this;
  }

  async pressKeyboard(key: string, options?: { text?: string; delay?: number }): Promise<void> {
    return this.asElementHandle().then((element) => {
      return element.press(key, options);
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
   * Clear value in textbox. Retries up to 3 times.
   */
  async clear(): Promise<void> {
    const getTextLength = async (): Promise<number> => {
      const text = await this.getProperty<string>('value');
      return text.trim().length;
    };

    const deleteText = async (element: ElementHandle): Promise<void> => {
      await element.focus();
      await element.hover();
      await element.click({ clickCount: 3 });
      await this.page.keyboard.press('Backspace');
      await this.page.waitForTimeout(100);
    };

    let maxRetries = 3;
    const clearAndCheck = async (element: ElementHandle) => {
      await deleteText(element);
      const textLength = await getTextLength();
      if (textLength === 0) {
        return; // success
      }
      if (maxRetries <= 0) {
        throw new Error('Failed to clear text."');
      }
      maxRetries--;
      await this.page.waitForTimeout(1000).then(() => clearAndCheck(element)); // 1 second pause and retry clear
    };

    const text = await getTextLength();
    if (text === 0) {
      return; // No text to clear.
    }
    const element = await this.asElementHandle();
    await clearAndCheck(element);
  }

  async clearTextInput(): Promise<void> {
    return this.asElementHandle().then((element) => {
      return this.page.evaluate(
        (elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
            window.HTMLInputElement.prototype,
            'value'
          ).set;
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
  async focus(timeout?: number): Promise<void> {
    const element = await this.asElementHandle({ visible: true, timeout });
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

  /**
   * Determine if cursor is disabled (== " not-allowed ") by checking style 'cursor' value.
   */
  async isCursorNotAllowed(): Promise<boolean> {
    const element = await this.asElementHandle();
    const cursor = await getStyleValue<string>(this.page, element, 'cursor');
    return cursor && cursor === 'not-allowed';
  }

  async dispose(): Promise<void> {
    return this.element.dispose();
  }

  // try this method when click() is not working
  async clickWithEval(): Promise<void> {
    const element = await this.asElementHandle();
    await element.focus();
    await this.page.evaluate((elem: ElementHandle) => elem.click(), element);
    await this.page.waitForTimeout(500);
  }

  /**
   * Click on element then wait for page navigation to finish.
   */
  async clickAndWait(
    navOptions: NavigationOptions = { waitUntil: ['load', 'networkidle0'], timeout: 2 * 60 * 1000 }
  ): Promise<void> {
    const navigationPromise = this.page.waitForNavigation(navOptions);
    await this.click({ delay: 10 });
    await navigationPromise.catch((err) => {
      // Log error but DON'T fail the test! Puppeteer waitForNavigation has issues.
      logger.error(`Encountered error when click (${this.xpath}) and wait for navigation to complete.\n${err.stack}`);
    });
    await waitWhileLoading(this.page);
  }

  /**
   * Paste texts in textarea instead type one char at a time. Very fast.
   * @param text
   */
  async paste(text: string): Promise<void> {
    return this.asElementHandle().then((element: ElementHandle) => {
      return this.page.evaluate(
        (elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
            window.HTMLTextAreaElement.prototype,
            'value'
          ).set;
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
  async asElementHandle(waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
    return this.waitForXPath(waitOptions);
  }

  async exists(timeout = 2000): Promise<boolean> {
    return exists(this.page, this.getXpath(), { timeout });
  }

  /**
   * Wait until button is clickable (enabled).
   * @param {string} xpathSelector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  async waitUntilEnabled(xpathSelector?: string): Promise<void> {
    const selector = xpathSelector || this.getXpath();
    await this.page.waitForXPath(selector, { visible: true });
    await this.page
      .waitForFunction(
        (xpath) => {
          const elemt = document.evaluate(
            xpath,
            document,
            null,
            XPathResult.FIRST_ORDERED_NODE_TYPE,
            null
          ).singleNodeValue;
          const style = window.getComputedStyle(elemt as Element);
          const propValue = style.getPropertyValue('cursor');
          return propValue === 'pointer';
        },
        {},
        selector
      )
      .catch((err) => {
        logger.error(`waitUntilEnabled() failed: xpath=${selector}`);
        logger.error(err);
        throw new Error(err);
      });
  }

  /**
   * Wait until button is not clickable (disabled).
   * @param {string} xpathSelector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  async waitUntilDisabled(xpathSelector?: string): Promise<void> {
    const selector = xpathSelector || this.getXpath();
    await this.page.waitForXPath(selector, { visible: true });
    await this.page
      .waitForFunction(
        (xpath) => {
          const node = document.evaluate(
            xpath,
            document,
            null,
            XPathResult.FIRST_ORDERED_NODE_TYPE,
            null
          ).singleNodeValue;
          const style = window.getComputedStyle(node as Element);
          const cursor = style.getPropertyValue('cursor');
          return cursor === 'not-allowed';
        },
        {},
        selector
      )
      .catch((err) => {
        logger.error(`FAIL waitUntilDisabled(): xpath=${selector}`);
        logger.error(err);
        throw new Error(err);
      });
  }

  async expectEnabled(enabled: boolean): Promise<void> {
    expect(await this.isCursorNotAllowed()).toBe(!enabled);
  }
}
