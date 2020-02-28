import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';
import launchBrowser from '../../driver/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe('Home', () => {
  let browser;
  let page;

  beforeAll(async () => {
    browser = await launchBrowser();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
  });

  afterEach(async () => {
    await page.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  test('Homepage is the landing page after Sign In', async () => {

    const urls = [];
    await page.setRequestInterception(true);
    page.on('request', request => {
      console.log(request.method());
      if (request.method() === 'GET' && (request.resourceType() === 'xhr' || request.resourceType() === 'fetch')) {
        urls.push(request.url());
      }
      request.continue();
    });

    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();

    const home = new HomePage(page);
    await home.waitForReady();
    const title = await page.title();
    expect(title).toMatch('Homepage');

    console.log(urls);
  });


});

/* Check 
[
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/config',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/profile',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/status-alert',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/workspaces',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/cdrVersions',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/workspaces/user-recent-workspaces',
      'https://api-dot-all-of-us-workbench-test.appspot.com/v1/workspaces/user-recent-resources'
]
 */