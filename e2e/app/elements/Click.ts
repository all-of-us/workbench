import {Page} from "puppeteer";

export default class Click {

   private readonly page: Page;

   constructor(aPage: Page) {
      this.page = aPage;
   }

   public async xpath(xpathSelector: string) {
      await this.page.evaluate((selector) => {
         const node: any = document.evaluate(
            selector,
            document,
            null,
            XPathResult.FIRST_ORDERED_NODE_TYPE,
            null
         ).singleNodeValue;
         document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
         node.click();
      }, xpathSelector);
   }

   public async css(cssSelector: string) {
      await this.page.evaluate((selector) => {
         const node: any = document.querySelector(selector);
         node.click();
      }, cssSelector);
   }

}
