import * as chromePaths from 'chrome-paths';
import * as Puppeteer from 'puppeteer-core';
import GoogleLoginPage from '../pages/google-login';

const configs = require('../config/config');

export default class ChromeBrowser {

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
    headless: configs.isHeadless,
    devtools: false,
    slowMo: 50,
    executablePath: chromePaths.chrome,
    defaultViewport: null,
    args: this.chromeSwitches
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
    console.log("isHeadless: " + configs.isHeadless);
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

}

module.exports = new ChromeBrowser();
