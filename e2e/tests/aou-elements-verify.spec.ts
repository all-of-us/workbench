import {Browser, Page} from 'puppeteer';
import NewClrIconLink from '../app/aou-elements/ClrIconLink';
import GoogleLoginPage from '../app/GoogleLoginPage';
import HomePage, {FIELD_LABEL} from '../app/HomePage';
import PageNavigation, {LINK} from '../app/page-mixin/PageNavigation';
import ProfilePage from '../app/ProfilePage';
import WorkspaceCard from '../app/WorkspaceCard';
import WorkspacesPage from '../app/WorkspacesPage';
import launchBrowser from '../driver/puppeteer-launch';

const configs = require('../resources/workbench-config');

jest.setTimeout(60 * 1000);

describe.skip('aou-elements', () => {

  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await launchBrowser();
  });

  afterAll(async () => {
    await browser.close();
  });

  beforeEach(async () => {
    const incognitoContext = await browser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
  });

  afterEach(async () => {
    await page.close();
    await page.waitFor(1000);
  });

  test('Workspace card', async () => {
    const home = new HomePage(page);
    await home.waitForReady();

    await WorkspaceCard.getAllCards(page);
    await WorkspaceCard.getAnyCard(page);

    await PageNavigation.goTo(page, LINK.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForReady();

    const n = 'aoutest-70-1583167646';
    const myCard = await WorkspaceCard.findCard(page, n);
    const myCardName = await myCard.getResourceCardName();
    expect(myCardName).toEqual(n);
    expect(await myCard.getEllipsisIcon()).toBeTruthy();

    const linTexts = await myCard.getPopupLinkTextsArray();
    expect(linTexts).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
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

    await PageNavigation.goTo(page, LINK.PROFILE);
    const profilePage = new ProfilePage(page);
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
    expect(fname).toMatch(new RegExp(/^[a-zA-Z]+$/));
    expect(lname).toMatch(new RegExp(/^[a-zA-Z]+$/));
    expect(lname).not.toEqual(fname); // check widgetxpath.textBoxXpath finds the right last and first name textbox

  });


});
