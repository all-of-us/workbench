import { ElementHandle, Page } from 'puppeteer';
import {config} from 'resources/workbench-config';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import Button from 'app/element/button';
import {waitForDocumentTitle} from 'utils/wait-utils';
import BaseElement from 'app/element/base-element';


export const SELECTOR = {
  loginButton: '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In")]',
  emailInput: '//input[@type="email"]',
  NextButton: '//*[text()="Next" or @value="Next"]',
  submitButton: '//*[@id="passwordNext" or @id="submit"]',
  passwordInput: '//input[@type="password"]',
};


export default class GoogleLoginPage {

  private readonly puppeteerPage: Page;

  constructor(page: Page) {
    this.puppeteerPage = page;
  }

  /**
   * Login email input field.
   */
  async email(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(SELECTOR.emailInput, {visible: true});
  }

  /**
   * Login password input field.
   */
  async password(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(SELECTOR.passwordInput, {visible: true});
  }

  /**
   * Google login button.
   */
  async loginButton(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(SELECTOR.loginButton, {visible: true, timeout: 60000});
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  async enterEmail(userEmail: string) : Promise<void> {
    // Handle Google "Use another account" dialog if it exists
    const useAnotherAccountXpath = '//*[@role="link"]//*[text()="Use another account"]';
    const elemt1 = await Promise.race([
      this.puppeteerPage.waitForXPath(SELECTOR.emailInput, {visible: true, timeout: 60000}),
      this.puppeteerPage.waitForXPath(useAnotherAccountXpath, {visible: true, timeout: 60000}),
    ]);

    // compare to the Use another account link
    const [link] = await this.puppeteerPage.$x(useAnotherAccountXpath);
    const isLink = await this.puppeteerPage.evaluate((e1, e2) => e1 === e2, elemt1, link);
    if (isLink) {
      // click " Use another Account " link
      await BaseElement.asBaseElement(this.puppeteerPage, link).clickAndWait();
    }

    const emailInput = await this.email();
    await emailInput.focus();
    await emailInput.type(userEmail);

    const nextButton = await this.puppeteerPage.waitForXPath(SELECTOR.NextButton);
    await BaseElement.asBaseElement(this.puppeteerPage, nextButton).clickAndWait();
  }

  /**
   * Enter login password.
   * @param pwd
   */
  async enterPassword(pwd: string) : Promise<void> {
    const input = await this.password();
    await input.focus();
    await input.type(pwd);
  }

  /**
   * Click Next button to submit login credential.
   */
  async submit() : Promise<void> {
    const button = await this.puppeteerPage.waitForXPath(SELECTOR.submitButton, {visible: true});
    await BaseElement.asBaseElement(this.puppeteerPage, button).clickAndWait();
  }

  /**
   * Open All-of-Us Google login page.
   */
  async load(): Promise<void> {
    const url = config.uiBaseUrl + config.loginUrlPath;
    await this.puppeteerPage.goto(url, {waitUntil: ['networkidle0', 'domcontentloaded'], timeout: 120000});
  }

  /**
   * Log in All-of-Us Workbench with default username and password.
   * (credential stored in .env file)
   * @param email
   * @param paswd
   */
  async login(email?: string, paswd?: string) {
    const user = email || config.userEmail;
    const pwd = paswd || config.userPassword;

    try {
      await this.load(); // load the Google Sign In page
      const googleLoginButton = await this.loginButton().catch((err) => {
        console.error('Google login button not found. ' + err);
        throw err;
      });
      await BaseElement.asBaseElement(this.puppeteerPage, googleLoginButton).clickAndWait();

      if (!user || user.trim().length === 0) {
        console.warn('Login user email: value is empty!!!')
      }
      await this.enterEmail(user);
      await this.puppeteerPage.waitFor(1000); // reduce chances of getting Google login recaptcha
      await this.enterPassword(pwd);
      await this.submit();
    } catch (err) {
      await takeScreenshot(this.puppeteerPage);
      await savePageToFile(this.puppeteerPage);
      throw new Error(err);
    }

    // Sometimes, user is prompted with "Enter Recovery Email" page. Handle the page if found.
    const recoverEmailXpath = '//input[@type="email" and @aria-label="Enter recovery email address"]';
    await Promise.race([
      waitForDocumentTitle(this.puppeteerPage, 'Homepage', 30000),
      this.puppeteerPage.waitForXPath(recoverEmailXpath, {visible: true, timeout: 30000})
    ]);
    const elementHandles = await this.puppeteerPage.$x(recoverEmailXpath);
    if (elementHandles.length > 0) {
      await elementHandles[0].type(config.broadInstitutionContactEmail);
      await Promise.all([
        this.puppeteerPage.waitForNavigation(),
        this.puppeteerPage.keyboard.press(String.fromCharCode(13)), // press Enter key
      ]);
      await waitForDocumentTitle(this.puppeteerPage, 'Homepage', 30000);
    }

  }

  async loginAs(email, paswd) {
    return await this.login(email, paswd);
  }

  async createAccountButton(): Promise<Button> {
    return Button.forLabel({puppeteerPage: this.puppeteerPage}, {text: 'Create Account'});
  }

}
