import * as Puppeteer from 'puppeteer';
import GoogleLoginPage from '../app/google-login';

const configs = require('../resources/config.js');

export default class ChromeDriver {

  public chromeSwitches = [
    '--no-sandbox',
    '--disable-setuid-sandbox',
    '--disable-dev-shm-usage',
    '--disable-gpu',
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
  ];

  public launchOpts = {
    headless: configs.isHeadless === true,
    devtools: false,
    slowMo: 10,
    defaultViewport: null,
    args: this.chromeSwitches,
    ignoreDefaultArgs: ['--disable-extensions'],
  };

  public userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36';

  public browser: Puppeteer.Browser;
  public page;
  private readonly incognito: boolean;

  constructor(incognito?: boolean) {
    this.incognito = incognito || true;
  }

  public async newBrowser(opts?): Promise<Puppeteer.Browser> {
    const launchOptions = opts || this.launchOpts;
    return this.browser = await Puppeteer.launch(launchOptions);
  }

  public async newPage(browser?: Puppeteer.Browser): Promise<Puppeteer.Page> {
    const br = browser || await this.newBrowser();
    if (this.incognito) {
      const incognitoContext = await br.createIncognitoBrowserContext();
      this.page = await incognitoContext.newPage();
    } else {
      this.page = await br.newPage();
    }
    await this.page.setUserAgent(this.userAgent);
    await this.page.setDefaultNavigationTimeout(60000);
    return this.page;
  }

  public async closePage() {
    return await this.page.close();
  }

  public async teardown() {
     // should Sign Out before close browser?
    await this.browser.close();
  }

  public async setup():  Promise<Puppeteer.Page>{
    await this.newPage();
    const loginPage = new GoogleLoginPage(this.page);
    await loginPage.login();
    return this.page;
  }

  public async logIn():  Promise<Puppeteer.Page>{
    const loginPage = new GoogleLoginPage(this.page);
    await loginPage.login();
    return this.page;
  }

}

module.exports = new ChromeDriver();
