import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import Button from '../element/button';

/**
 * Container object can be any Page, Dialog or Modal.
 */
export default abstract class Container {

  protected elementHandle: ElementHandle;

  protected constructor(protected readonly page: Page, protected selector?: {xpath: string}) {
  }

  async findElement(options?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.elementHandle = await this.page.waitForXPath(this.selector.xpath, options);
    return this.elementHandle;
  }

  async findDescendantElements(descendantSelector: {xpath: string}): Promise<ElementHandle[]> {
    return await this.elementHandle.$x(descendantSelector.xpath);
  }

  async findButton(buttonName: string): Promise<Button> {
    const locator = `(//button | //*[@role="button"])[normalize-space(text())="${buttonName}"]`
    return new Button(this.page, (await this.elementHandle.$x(locator))[0]);
  }

   /**
    * Returns a Puppeteer ElementHandle.
    */
  getElementHandle(): ElementHandle {
    return this.elementHandle.asElement();
  }

   /**
    * Returns true if the element is found in DOM.
    */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.selector.xpath)).length > 0;
  }

   /**
    * Wait for element property value equals to.
    * @param propertyName
    * @param propertyValue
    */
  async waitForProperty(propertyName: string, propertyValue: string): Promise<void> {
    await this.page.waitForFunction(xpath => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return element[propertyName] === propertyValue;
    }, {}, this.selector.xpath);
  }

  async waitUntilVisible(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.selector.xpath, {visible: true});
  }

  async waitUntilDisappear(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.selector.xpath, {hidden: true});
  }

}
