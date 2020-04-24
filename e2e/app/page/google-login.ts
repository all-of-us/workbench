import { ElementHandle, Page } from 'puppeteer';
import {findButton} from 'app/element/xpath-finder';
import BasePage from 'app/page/base-page';
import HomePage from 'app/page/home-page';
import {config} from 'resources/workbench-config';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';


export const SELECTOR = {
  loginButton: '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In")]',
  emailInput: '//input[@type="email"]',
  NextButton: '//*[text()="Next" or @value="Next"]',
  submitButton: '//*[@id="passwordNext" or @id="submit"]',
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
    return this.page.waitForXPath(SELECTOR.emailInput, {visible: true});
  }

  /**
   * Login password input field.
   */
  async password(): Promise<ElementHandle> {
    return this.page.waitForXPath(SELECTOR.passwordInput, {visible: true});
  }

  /**
   * Google login button.
   */
  async loginButton(): Promise<ElementHandle> {
    return this.page.waitForXPath(SELECTOR.loginButton, {visible: true, timeout: 60000});
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  async enterEmail(userEmail: string) : Promise<void> {
    // Handle Google "Use another account" dialog if it exists
    const useAnotherAccountXpath = '//*[@role="link"]//*[text()="Use another account"]';
    const elemt1 = await Promise.race([
      this.page.waitForXPath(SELECTOR.emailInput, {visible: true, timeout: 60000}),
      this.page.waitForXPath(useAnotherAccountXpath, {visible: true, timeout: 60000}),
    ]);

    // compare to the Use another account link
    const [link] = await this.page.$x(useAnotherAccountXpath);
    const isLink = await this.page.evaluate((e1, e2) => e1 === e2, elemt1, link);
    if (isLink) {
      // click " Use another Account " link
      await this.clickAndWait(link);
    }

    const emailInput = await this.email();
    await emailInput.focus();
    await emailInput.type(userEmail);

    const nextButton = await this.page.waitForXPath(SELECTOR.NextButton);
    await this.clickAndWait(nextButton);
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
    const button = await this.page.waitForXPath(SELECTOR.submitButton, {visible: true});
    await this.clickAndWait(button);
  }

  /**
   * Open All-of-Us Google login page.
   */
  async load(): Promise<void> {
    const url = config.uiBaseUrl + config.loginUrlPath;
    try {
      await this.page.goto(url, {waitUntil: ['networkidle0', 'domcontentloaded'], timeout: 0});
    } catch (err) {
      console.error('Google login page not found. ' + err);
      await takeScreenshot(this.page, 'GoogleLoginPageNotFound');
      throw err;
    }
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
      await this.clickAndWait(googleLoginButton);

      if (!user || user.trim().length === 0) {
        console.warn('Login user email: value is empty!!!')
      }
      await this.enterEmail(user);
      await this.enterPassword(pwd);
      await this.submit();
    } catch (err) {
      await takeScreenshot(this.page, 'FailedLoginPage');
      await savePageToFile(this.page, 'FailedLoginPage');
      throw err;
    }

    try {
      await this.waitUntilTitleMatch('Homepage');
    } catch (e) {
      // Handle "Enter Recovery Email" prompt if found exists
      const recoverEmail = await this.page.$x('//input[@type="email" and @aria-label="Enter recovery email address"]');
      if (recoverEmail.length > 0) {
        await recoverEmail[0].type(config.contactEmail);
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
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
    const home = new HomePage(page);
    await home.waitForLoad();
    return home;
  }


}
