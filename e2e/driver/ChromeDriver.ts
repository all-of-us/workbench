import * as Puppeteer from 'puppeteer';
import GoogleLoginPage from '../app/GoogleLoginPage';
import HomePage from '../app/HomePage';

export default class ChromeDriver {

  public static userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36';

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
    headless: !process.env.PUPPETEER_HEADLESS,
    devtools: false,
    slowMo: 10,
    defaultViewport: null,
    args: this.chromeSwitches,
    ignoreDefaultArgs: ['--disable-extensions'],
  };

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
    await this.page.setUserAgent(ChromeDriver.userAgent);
    // value 0 means disable timeout
    await this.page.setDefaultNavigationTimeout(60000); // timeout for all page navigation functions
    await this.page.setDefaultTimeout(15000); // timeout for all async waitFor functions
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
    return this.logInWithDefaultUser();
  }

  public async logInWithDefaultUser():  Promise<Puppeteer.Page>{
    const loginPage = new GoogleLoginPage(this.page);
    await loginPage.login();
    await (new HomePage(this.page)).waitForReady();
    return this.page;
  }

}

module.exports = new ChromeDriver();
