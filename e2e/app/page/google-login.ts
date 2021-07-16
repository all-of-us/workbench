import { ElementHandle, Page } from 'puppeteer';
import { config } from 'resources/workbench-config';
import Button from 'app/element/button';
import {signedInIndicator} from 'app/page/authenticated-page';

export enum FieldSelector {
  LoginButton = '//*[@role="button"]/*[contains(normalize-space(text()),"Sign In")]',
  CookiePolicyLink = '//a[text()="Cookie Policy"]',
  EmailInput = '//input[@type="email"]',
  NextButton = '//*[text()="Next" or @value="Next"]',
  SubmitButton = '//*[@id="passwordNext" or @id="submit"]',
  PasswordInput = '//input[@type="password"]'
}

export default class GoogleLoginPage {
  constructor(private readonly page: Page) {}

  /**
   * Login email input field.
   */
  async email(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.EmailInput, { visible: true, timeout: 60000 });
  }

  /**
   * Login password input field.
   */
  async password(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.PasswordInput, { visible: true, timeout: 60000 });
  }

  /**
   * Google login button.
   */
  async loginButton(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.LoginButton, { visible: true, timeout: 60000 });
  }

  async cookiePolicyLink(): Promise<ElementHandle> {
    return this.page.waitForXPath(FieldSelector.CookiePolicyLink, { visible: true, timeout: 60000 });
  }

  /**
   * Enter login email and click Next button.
   * @param email
   */
  async enterEmail(userEmail: string): Promise<void> {
    // Handle Google "Use another account" modal if it exists
    const useAnotherAccountXpath = '//*[@role="link"]//*[text()="Use another account"]';
    const elemt1 = await Promise.race([
      this.page.waitForXPath(FieldSelector.EmailInput, { visible: true, timeout: 60000 }),
      this.page.waitForXPath(useAnotherAccountXpath, { visible: true, timeout: 60000 })
    ]);

    // compare to the "Use another Account" link
    const [link] = await this.page.$x(useAnotherAccountXpath);
    if (link) {
      const isLink = await this.page.evaluate((e1, e2) => e1 === e2, elemt1, link);
      if (isLink) {
        // click "Use another Account" link and wait for navigation.
        await link.click();
      }
      await link.dispose();
    }

    const emailInput = await this.email();
    await emailInput.focus();
    await emailInput.type(userEmail, { delay: 15 });
    await emailInput.dispose();

    const nextButton = await this.page.waitForXPath(FieldSelector.NextButton, { visible: true });
    await nextButton.click();
  }

  /**
   * Enter login password.
   * @param pwd
   */
  async enterPassword(pwd: string): Promise<void> {
    const input = await this.password();
    await input.focus();
    await input.type(pwd, { delay: 15 });
    await input.dispose();
  }

  /**
   * Click Next button to submit login credential.
   */
  async submit(): Promise<void> {
    const submitButton = new Button(this.page, FieldSelector.SubmitButton);
    await Promise.all([
      this.page.waitForNavigation({ waitUntil: ['networkidle0', 'load'], timeout: 2 * 60 * 1000 }), // 2 minutes
      submitButton.click()
    ]);
    await submitButton.dispose();
    await this.page.waitForSelector(signedInIndicator, { timeout: 2 * 60 * 1000 }).catch(async (err) => {
      // Two main reasons why error is throw are caused by "Enter Recovery Email" page or login captcha.
      // At this time, we can only handle "Enter Recover Email" page if it exists.
      const found = await this.fillOutRecoverEmail();
      if (!found) {
        console.error(err);
        throw err;
      }
    });
  }

  /**
   * Open All-of-Us Google login page.
   */
  async load(): Promise<void> {
    const url = `${config.LOGIN_URL_DOMAIN_NAME}${config.LOGIN_URL_PATH}`;
    const response = await this.page.goto(url, {
      waitUntil: ['networkidle0', 'domcontentloaded', 'load'],
      timeout: 2 * 60 * 1000
    });
    if (response && response.ok()) {
      return;
    }
    // Retry load Login page.
    console.warn('Retry loading Login page');
    await this.page.goto(url, { waitUntil: ['networkidle0', 'domcontentloaded', 'load'], timeout: 30 * 1000 });
  }

  /**
   * Log in All-of-Us Workbench with default username and password.
   * (credential stored in .env file)
   * @param email
   * @param paswd
   */
  async login(email?: string, paswd?: string): Promise<void> {
    const user = email || config.USER_NAME;
    const pwd = paswd || config.PASSWORD;

    if (!user || user.trim().length === 0) {
      console.warn('Login user email: value is empty!!!');
    }

    await this.load(); // Load the Google Sign In page.
    await this.loginButton().then((button) => button.click());

    await this.enterEmail(user);
    await this.page.waitForTimeout(500); // Reduces probablity of getting Google login captcha
    await this.enterPassword(pwd);
    await this.page.waitForTimeout(500);
    await this.submit();
  }

  async loginAs(email: string, paswd: string): Promise<void> {
    return this.login(email, paswd);
  }

  async clickCreateAccountButton(): Promise<void> {
    const button = Button.findByName(this.page, { name: 'Create Account' });
    await button.clickWithEval();
  }

  /**
   * Fill out "Enter Recover Email" input if found.
   * @private
   */
  private async fillOutRecoverEmail(): Promise<boolean> {
    const [recoverAccountButton] = await this.page.$x('//button[./*[.="Recover account"]]');
    if (recoverAccountButton) {
      await recoverAccountButton.click();
      await this.page.waitForTimeout(1000);
    }
    const emailInputXpath = '//input[@type="email" and @aria-label="Enter recovery email address"]';
    const [emailInput] = await this.page.$x(emailInputXpath);
    if (emailInput) {
      await emailInput.type(config.INSTITUTION_CONTACT_EMAIL);
      await Promise.all([
        this.page.waitForNavigation({ waitUntil: ['networkidle2', 'load'], timeout: 60000 }),
        this.page.keyboard.press(String.fromCharCode(13)) // Press Enter key.
      ]);
      return true;
    }
    // "Enter Recover Email" input not found.
    return false;
  }
}
