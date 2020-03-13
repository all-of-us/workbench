import Button from '../../app/aou-elements/button';
import {SideNavLink} from '../../app/authenticated-page';
import HomePage from '../../app/home-page';
import ProfilePage from '../../app/profile-page';
import WorkspacesPage from '../../app/workspaces-page';
import {signIn} from '../app';
import {waitForExists} from '../../driver/xpath-util';

export const HELP_DESK = {
  ASK_QUESTION: 'Ask a question about the Researcher Workbench',
  REPORT_DATA_PRIVACY_CONCERN: 'Report a data privacy concern',
  TELL_US_ABOUT_PUBLICATION: 'Tell us about an upcoming publication',
  REQUEST_ADDITIONAL_BILLING_CREDITS: 'Request additional billing credits',
};

// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe('Navigation', () => {

  let page;

  beforeEach(async () => {
    page = await context.newPage();
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  test('Check app side-nav work', async () => {
    const homePage = new HomePage(page);
    // Select Profile link
    await homePage.navTo(SideNavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();
    expect(await profilePage.isLoaded()).toBe(true);

    // check user name in dropdown matches names on Profile page
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
    await homePage.openSideNav();
    const displayedUsername = await homePage.getUserName();
    expect(displayedUsername).toBe(`${fname} ${lname}`);

    // Select Your Workspaces link
    await homePage.navTo(SideNavLink.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();
    expect(await workspacesPage.isLoaded()).toBe(true);

    // Select Home link
    await homePage.navTo(SideNavLink.HOME);
    expect(await homePage.isLoaded()).toBe(true);
  });

  test('User can see the Contact Us form', async () => {
    const homePage = new HomePage(page);
    // Select Contact Us
    await homePage.navTo(SideNavLink.CONTACT_US);

    const iframeHandle = await page.waitForSelector('iframe[title="Find more information here"]');
    const newIframe: any = await iframeHandle.contentFrame();

    const askQuestionAboutButton = await Button.forLabel(newIframe, {text: HELP_DESK.ASK_QUESTION});
    expect(await askQuestionAboutButton.isVisible()).toBe(true);

    const reportConcernButton = await Button.forLabel(newIframe, {text: HELP_DESK.REPORT_DATA_PRIVACY_CONCERN});
    expect(await reportConcernButton.isVisible()).toBe(true);
    await reportConcernButton.dispose();

    const tellAboutPublicationButton = await Button.forLabel(newIframe, {text: HELP_DESK.TELL_US_ABOUT_PUBLICATION});
    expect(await tellAboutPublicationButton.isVisible()).toBe(true);
    await tellAboutPublicationButton.dispose();

    const requestBillingCreditsButton = await Button.forLabel(newIframe, {text: HELP_DESK.REQUEST_ADDITIONAL_BILLING_CREDITS});
    expect(await requestBillingCreditsButton.isVisible()).toBe(true);
    await requestBillingCreditsButton.dispose();

    const minimizeButton = await newIframe.$('button[aria-label="Minimize widget"]');
    await minimizeButton.click();

    expect(await askQuestionAboutButton.isVisible()).toBe(false);
    await askQuestionAboutButton.dispose();
  });

  test('User can Sign Out', async () => {
    const homePage = new HomePage(page);
    // Select Sign Out link
    await homePage.navTo(SideNavLink.SIGN_OUT);
    await waitForExists(page, '//*[text()="Redirect Notice"]');
    const href = await page.evaluate(() => location.href);
    expect(href).toEqual(expect.stringMatching(/(\/|%2F)login$/));
  });

});
