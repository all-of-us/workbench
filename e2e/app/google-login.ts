import { ElementHandle, Page } from 'puppeteer';
import {findButton} from './aou-elements/xpath-finder';
import BasePage from './base-page';
import HomePage from './home-page';

const configs = require('../resources/workbench-config');

export const selectors = {
  loginButton: '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In with Google")]',
  emailInput: '//input[@type="email"]',
  NextButton: '//*[text()="Next" or @value="Next"]',
  passwordInput: '//input[@type="password"]',
};


export default class GoogleLoginPage extends BasePage {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Login email input field.
   */
  async email(): Promise<ElementHandle> {
    return this.page.waitForXPath(selectors.emailInput, {visible: true});
  }

  /**
   * Login password input field.
   */
  async password(): Promise<ElementHandle> {
    return this.page.waitForXPath(selectors.passwordInput, {visible: true});
  }

  /**
   * Google login button.
   */
  async loginButton(): Promise<ElementHandle> {
    return this.page.waitForXPath(selectors.loginButton, {visible: true});
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  async enterEmail(userEmail: string) : Promise<void> {
    // Handle Google "Use another account" dialog if it exists
    const anotherAccountXpath = '//*[@role="link"]//*[text()="Use another account"]';
    const elemt1 = await Promise.race([
      this.page.waitForXPath(selectors.emailInput, {timeout: 1000}),
      this.page.waitForXPath(anotherAccountXpath, {timeout: 1000}),
    ]);

    // compare to the Use another account link
    const [link] = await this.page.$x(anotherAccountXpath);
    const isLink = await page.evaluate((e1, e2) => e1 === e2, elemt1, link);
    if (isLink) {
      console.log('islink');
      await Promise.all([
        this.page.waitForNavigation(),
        link.click(),
      ]);
    } else {
      console.log('is not islink');
    }
    const emailInput = await this.email();
    await emailInput.focus();
    await emailInput.type(userEmail);
    await this.takeScreenshot('enteredEmail');
    const html = await page.content();
    await this.writeToFile('emailPage', html);
    const nextButton = await this.page.waitForXPath(selectors.NextButton);
    await Promise.all([
      this.page.waitForNavigation(),
      nextButton.click(),
    ]);
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
    const button = await this.page.waitForXPath(selectors.NextButton, {visible: true});
    await Promise.all([
      this.page.waitForNavigation(),
      button.click(),
    ]);
  }

  /**
   * Open All-of-Us Google login page.
   */
  async load(): Promise<void> {
    const url = configs.uiBaseUrl + configs.loginUrlPath;
    try {
      await this.page.goto(url, {waitUntil: ['networkidle0', 'domcontentloaded']});
    } catch (err) {
      console.error('Google login page not found. ' + err);
      await this.takeScreenshot('GoogleLoginPageNotFound');
      throw err;
    }
  }

  /**
   * Log in All-of-Us Workbench with default username and password.
   * Short circuit: If page was redirected to Home page, Login to be skipped.
   * (credential stored in .env file)
   * @param email
   * @param paswd
   */
  async login(email?: string, paswd?: string) {
    const user = email || configs.userEmail;
    const pwd = paswd || configs.userPassword;
    await this.load(); // load the Google Sign In page
    const googleLoginButton = await this.loginButton().catch((err) => {
      console.error('Google login button not found. ' + err);
      throw err;
    });
    await Promise.all([
      this.page.waitForNavigation(),
      googleLoginButton.click(),
    ]);

    if (!user || user.trim().length === 0) {
      console.warn('Login user email: value is empty!!!')
    }
    await this.enterEmail(user);
    await this.enterPassword(pwd);
    await this.submit();

    try {
      await this.waitUntilTitleMatch('Homepage');
    } catch (e) {
      // Handle "Enter Recovery Email" prompt if found exists
      const recoverEmail = await this.page.$x('//input[@type="email" and @aria-label="Enter recovery email address"]');
      if (recoverEmail.length > 0) {
        await recoverEmail[0].type(configs.contactEmail);
        await Promise.all([
          this.page.waitForNavigation(),
          this.page.keyboard.press(String.fromCharCode(13)), // press Enter key
        ]);
      }
    }

  }

  async loginAs(email, paswd) {
    return await this.login(email, paswd);
  }

  async createAccountButton(): Promise<ElementHandle> {
    return await findButton(this.page, {text: 'Create Account'}, {visible: true});
  }

  static async logIn(page: Page): Promise<HomePage> {
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(120000);
    await page.setViewport({ width: 1920, height: 1080 }); // should match '--window-size=1920,1080' in jest-puppeteer.config.js

    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
    const home = new HomePage(page);
    await home.waitForLoad();
    return home;
  }


}
