import {ClickOptions} from 'puppeteer';

export interface ElementInterface {

  getProperty(propertyName: string): Promise<unknown>;

  getAttribute(attributeName: string): Promise<unknown>;

  exists(): boolean;

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
