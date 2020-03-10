import {ClickOptions, ElementHandle, WaitForSelectorOptions} from 'puppeteer';

export interface BaseElementInterface {

  withCss(aCssSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle>

  withXpath(aXpathSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle>

  getProperty(propertyName: string): Promise<unknown>;

  getAttribute(attributeName: string): Promise<unknown>;

  isVisible(): Promise<boolean>;

  hasAttribute(attr: string): Promise<boolean>;

  isDisabled(): Promise<boolean>;

  click(options?: ClickOptions): Promise<void>;

  type(text: string, options?: { delay: number }): Promise<void>;

  focus(): Promise<void>;

  pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void>;

  getTextContent(): Promise<string>;

  getComputedStyle(styleName: string): Promise<unknown>;
}
