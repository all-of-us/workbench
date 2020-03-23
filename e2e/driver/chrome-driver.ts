import * as Puppeteer from 'puppeteer';
import GoogleLoginPage from '../app/google-login';
import HomePage from '../app/home-page';
import {getChromeFlags} from './chrome-startup-flags';

export default class ChromeDriver {

  static userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36';

  launchOpts = {
    headless: !process.env.PUPPETEER_HEADLESS,
    devtools: false,
    slowMo: 10,
    defaultViewport: null,
    args: getChromeFlags(),
    ignoreDefaultArgs: true,
  };

  browser: Puppeteer.Browser;
  page;
  private readonly incognito: boolean;

  constructor(incognito?: boolean) {
    this.incognito = incognito || true;
  }

  async newBrowser(opts?): Promise<Puppeteer.Browser> {
    const launchOptions = opts || this.launchOpts;
    return this.browser = await Puppeteer.launch(launchOptions);
  }

  async newPage(aBrowser?: Puppeteer.Browser): Promise<Puppeteer.Page> {
    this.browser = aBrowser || await this.newBrowser();
    if (this.incognito) {
      const incognitoContext = await this.browser.createIncognitoBrowserContext();
      this.page = await incognitoContext.newPage();
    } else {
      this.page = await this.browser.newPage();
    }
    await this.page.setUserAgent(ChromeDriver.userAgent);
    // value 0 means disable timeout
    await this.page.setDefaultNavigationTimeout(60000); // timeout for all page navigation functions
    await this.page.setDefaultTimeout(15000); // timeout for all async waitFor functions
    return this.page;
  }

  async closePage() {
    return await this.page.close();
  }

  async teardown() {
     // should Sign Out before close browser?
    await this.browser.close();
  }

  async setup():  Promise<Puppeteer.Page>{
    await this.newPage();
    return this.logInWithDefaultUser();
  }

  async logInWithDefaultUser():  Promise<Puppeteer.Page>{
    const loginPage = new GoogleLoginPage(this.page);
    await loginPage.login();
    await (new HomePage(this.page)).waitForLoad();
    return this.page;
  }

}

module.exports = new ChromeDriver();
