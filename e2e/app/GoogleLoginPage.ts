import { ElementHandle, Page } from 'puppeteer';
import { waitForNavigation } from '../driver/page-wait';

const configs = require('../resources/config');

export const selectors = {
  loginButton: '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In with Google")]',
  emailInput: '//input[@type="email"]',
  emailNextButton: '//*[@id="identifierNext"]//*[normalize-space(text())="Next"]',
  passwordInput: '//input[@type="password"]',
  passwordNextButton: '//*[@role="button"][@id="passwordNext"]//*[normalize-space(text())="Next"]',
};

export default class GoogleLoginPage {

  public page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Login email input field.
   */
  get email(): Promise<ElementHandle> {
    return this.page.waitForXPath(selectors.emailInput, {visible: true});
  }

  /**
   * Login password input field.
   */
  get password(): Promise<ElementHandle> {
    return this.page.waitForXPath(selectors.passwordInput, {visible: true});
  }

  /**
   * Google login button.
   */
  get loginButton(): Promise<ElementHandle<Element>> {
    return this.page.waitForXPath(selectors.loginButton, {visible: true});
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  public async enterEmail(email: string) : Promise<void> {

    const screenshotFile = `enterEmail.png`;
    await this.page.screenshot({path: screenshotFile, fullPage: true});

    const input = await this.email;
    await input.focus();
    await input.type(email);

    const nextButton = await this.page.waitForXPath(selectors.emailNextButton);
    await Promise.all([
      this.page.waitForNavigation(),
      nextButton.click()
    ]);
  }

  /**
   * Enter login password.
   * @param pwd
   */
  public async enterPassword(pwd: string) : Promise<void> {
    const input = await this.password;
    await input.focus();
    await input.type(pwd);
  }

  /**
   * Click Next button to submit login credential.
   */
  public async submit() : Promise<void> {
    const naviPromise = waitForNavigation(this.page);
    const button = await this.page.waitForXPath(selectors.passwordNextButton);
    await button.click();
    await naviPromise;
  }

  /**
   * Open All-of-Us Google login page.
   */
  public async goto(): Promise<void> {
    await this.page.goto(configs.uiBaseUrl + configs.loginUrlPath, {waitUntil: 'networkidle0'});
  }

  /**
   * Log in All-of-Us Workbench with default username and password.
   * (credential stored in .env file)
   * @param email
   * @param paswd
   */
  public async login(email?: string, paswd?: string) {
    const user = email || configs.userEmail;
    const pwd = paswd || configs.userPassword;

    await this.goto();

    const naviPromise = this.page.waitForNavigation({waitUntil: 'networkidle0'});
    const googleButton = await this.loginButton;
    await googleButton.click();
    await naviPromise;

    if (!user || user.trim().length === 0) {
      console.warn('Login user email: value is empty!!!')
    }

    await this.enterEmail(user);
    await this.enterPassword(pwd);
    await this.submit();
  }

  public async loginAs(email, paswd) {
    return await this.login(email, paswd);
  }

  public async createAccountButton(): Promise<ElementHandle> {
    return await this.page.waitForXPath('//*[@role=\'button\'][(text()=\'Create Account\')]', {visible:true})
  }

}
