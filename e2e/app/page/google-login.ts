import { ElementHandle, Page } from 'puppeteer';
import {config} from 'resources/workbench-config';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';
import Button from 'app/element/button';

export enum FieldSelector {
  LoginButton= '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In")]',
  CookiePolicyLink = '//a[text()="Cookie Policy"]',
  EmailInput = '//input[@type="email"]',
  NextButton = '//*[text()="Next" or @value="Next"]',
  SubmitButton = '//*[@id="passwordNext" or @id="submit"]',
  PasswordInput = '//input[@type="password"]',
}


export default class GoogleLoginPage {

  constructor(private readonly page: Page) {
  }

  /**
   * Login email input field.
   */
  async email(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.EmailInput, {visible: true});
  }

  /**
   * Login password input field.
   */
  async password(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.PasswordInput, {visible: true});
  }

  /**
   * Google login button.
   */
  async loginButton(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.LoginButton, {visible: true, timeout: 60000});
  }

  async cookiePolicyLink(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.CookiePolicyLink, {visible: true, timeout: 60000});
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  async enterEmail(userEmail: string) : Promise<void> {
    // Handle Google "Use another account" dialog if it exists
    const useAnotherAccountXpath = '//*[@role="link"]//*[text()="Use another account"]';
    const elemt1 = await Promise.race([
      this.page.waitForXPath(FieldSelector.EmailInput, {visible: true, timeout: 60000}),
      this.page.waitForXPath(useAnotherAccountXpath, {visible: true, timeout: 60000}),
    ]);

    // compare to the Use another account link
    const [link] = await this.page.$x(useAnotherAccountXpath);
    const isLink = await this.page.evaluate((e1, e2) => e1 === e2, elemt1, link);
    if (isLink) {
      // click " Use another Account " link
      await (BaseElement.asBaseElement(this.page, link)).clickAndWait();
    }

    const emailInput = await this.email();
    await emailInput.focus();
    await emailInput.type(userEmail);

    const nextButton = await this.page.waitForXPath(FieldSelector.NextButton);
    await (BaseElement.asBaseElement(this.page, nextButton)).clickAndWait();
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
    const button = await this.page.waitForXPath(FieldSelector.SubmitButton, {visible: true});
    await (Button.asBaseElement(this.page, button)).clickAndWait();
  }

  /**
   * Open All-of-Us Google login page.
   */
  async load(): Promise<void> {
    const url = config.uiBaseUrl + config.loginUrlPath;
    await this.page.goto(url, {waitUntil: ['networkidle0', 'domcontentloaded', 'load'], timeout: 180000});
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
      await (BaseElement.asBaseElement(this.page, googleLoginButton)).clickAndWait();

      if (!user || user.trim().length === 0) {
        console.warn('Login user email: value is empty!!!')
      }
      await this.enterEmail(user);
      await this.page.waitFor(1000); // to reduce probablity of getting Google login recaptcha
      await this.enterPassword(pwd);
      await this.submit();
    } catch (err) {
      await takeScreenshot(this.page);
      await savePageToFile(this.page);
      throw new Error(err);
    }

    // Sometimes, user is prompted with "Enter Recovery Email" page. Handle the page if found.
    const recoverEmailXpath = '//input[@type="email" and @aria-label="Enter recovery email address"]';
    await Promise.race([
      waitForDocumentTitle(this.page, 'Homepage'),
      this.page.waitForXPath(recoverEmailXpath, {visible: true})
    ]);
    const elementHandles = await this.page.$x(recoverEmailXpath);
    if (elementHandles.length > 0) {
      await elementHandles[0].type(config.broadInstitutionContactEmail);
      await Promise.all([
        this.page.waitForNavigation(),
        this.page.keyboard.press(String.fromCharCode(13)), // press Enter key
      ]);
      await waitForDocumentTitle(this.page, 'Homepage');
    }

  }

  async loginAs(email, paswd) {
    return this.login(email, paswd);
  }

  async clickCreateAccountButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: 'Create Account'});
    await button.clickWithEval();
  }
}
