import {Browser, Page} from 'puppeteer';
import NewClrIconLink from '../app/aou-elements/ClrIconLink';
import GoogleLoginPage from '../app/GoogleLoginPage';
import {FIELD_LABEL} from '../app/HomePage';
import NaviBar, {LINK} from '../app/page-mixin/NaviBar';
import ProfilePage from '../app/ProfilePage';

const Chrome = require('../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe.skip('aou-elements', () => {

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

  /**
   * This is not a Puppeteer test for AoU application. It is for framework functions testing.
   * If you make any change in aou-elements classes, you want to run this test to verify changes.
   */
  test('Click on Create New Workspace link on Home page', async () => {

    const anyLink = new NewClrIconLink(page);
    const linkHandle = await anyLink.withLabel(FIELD_LABEL.CREATE_NEW_WORKSPACE, 'plus-circle');
    expect(linkHandle).toBeTruthy();
    const classname = await anyLink.getProperty('className');
    expect(classname).toBe('is-solid');
    const shape = await anyLink.getAttribute('shape');
    expect(shape).toBe('plus-circle');
    const hasShape = await anyLink.hasAttribute('shape');
    expect(hasShape).toBe(true);
    const disabled = await anyLink.isDisabled();
    expect(disabled).toBe(false);
    const cursor = await anyLink.getComputedStyle('cursor');
    expect(cursor).toBe('pointer');
    expect(await anyLink.isVisible()).toBe(true);

    await anyLink.dispose();
    expect(await anyLink.isVisible()).toBe(false);

    await NaviBar.go(page, LINK.PROFILE);
    const profilePage = new ProfilePage(page);
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
    expect(fname).toMatch(new RegExp(/^[a-zA-Z]+$/));
    expect(lname).toMatch(new RegExp(/^[a-zA-Z]+$/));
    expect(lname).not.toEqual(fname); // check widgetxpath.textBoxXpath finds the right last and first name textbox

  });


});
