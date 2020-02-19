import {ElementHandle} from "puppeteer";
import AuthenticatedPage from "./mixin/authenticatedpage";
import BasePage from "./mixin/basepage";

export default class CreateAccountPage extends BasePage {
   public async getInvitationKeyInput(): Promise<ElementHandle> {
      return await this.puppeteerPage.waitForSelector("#invitationKey");
   }

   public async getSubmitButton(): Promise<ElementHandle> {
      return await this.puppeteerPage.waitForXPath("//*[@role='button'][(text()='Next')]", {visible:true})
   }

   public async scrollToLastPdfPage(): Promise<ElementHandle> {
      const selectr = ".react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page";
      const pdfPage = await this.puppeteerPage.waitForSelector(".react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page");
      const [response] = await Promise.all([
         this.puppeteerPage.waitForNavigation({waitUntil: 'domcontentloaded'}),
         this.puppeteerPage.click(selectr, {delay: 20}),
      ]);
      await this.puppeteerPage.evaluate(el => el.scrollIntoView(), pdfPage);
      return pdfPage;
   }

}
