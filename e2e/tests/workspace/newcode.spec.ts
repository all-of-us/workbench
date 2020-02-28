import {Browser, Page} from 'puppeteer';
import NewClrIconLink from '../../app/aou-elements/ClrIconLink';
import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';

const Chrome = require('../../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe('Workspace', () => {

  let chromeBrowser: Browser;
  let page: Page;

  beforeAll(async () => {
    chromeBrowser = await Chrome.newBrowser();
  });

  afterAll(async () => {
    await chromeBrowser.close();
  });

  beforeEach(async () => {
    const incognitoContext = await chromeBrowser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
  });

  afterEach(async () => {
    await page.close();
    await page.waitFor(1000);
  });

  // Click CreateNewWorkspace link on Home page => Open Create Workspace page
  test('Click on Create New Workspace link on Home page', async () => {

    const anyLink = new NewClrIconLink(page);
    const l = await anyLink.withLabel(HomePage.selectors.createNewWorkspaceLink);
    console.log(l);
    const t = await anyLink.getProperty('className');
    console.log(t);
    const o = await anyLink.getAttribute('shape');
    console.log(o);
    const s = await anyLink.hasAttribute('shape');
    console.log(s);
    const v = await anyLink.isDisabled();
    console.log(v);
    const d = await anyLink.isDisabled();
    console.log(d);
  });


});
