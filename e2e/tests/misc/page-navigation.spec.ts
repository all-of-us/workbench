import Button from '../../app/aou-elements/Button';
import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';
import PageNavigation, {LINK} from '../../app/page-mixin/PageNavigation';
import ProfilePage from '../../app/ProfilePage';
import WorkspacesPage from '../../app/WorkspacesPage';
import launchBrowser from '../../driver/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../resources/workbench-config');

export const HELP_DESK = {
  ASK_QUESTION: 'Ask a question about the Researcher Workbench',
  REPORT_DATA_PRIVACY_CONCERN: 'Report a data privacy concern',
  TELL_US_ABOUT_PUBLICATION: 'Tell us about an upcoming publication',
  REQUEST_ADDITIONAL_BILLING_CREDITS: 'Request additional billing credits',
};

describe('Navigation', () => {

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
    await page.waitFor(1000);
  });

  afterAll(async () => {
    await browser.close();
  });


  test('App navigation links work', async () => {

    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();

    const homePage = new HomePage(page);
    await homePage.waitForReady();
    expect(await homePage.isLoaded()).toBe(true);

    // Select Profile link
    await PageNavigation.goTo(page, LINK.PROFILE);
    const profilePage = new ProfilePage(page);
    await profilePage.waitForReady();
    expect(await profilePage.isLoaded()).toBe(true);

    // check user name in dropdown matches names on Profile page
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
    await PageNavigation.openDropdown(page);
    const displayedUsername = await PageNavigation.getUserName(page);
    expect(displayedUsername).toBe(`${fname} ${lname}`);

    // Select Your Workspaces link
    await PageNavigation.goTo(page, LINK.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForReady();
    expect(await workspacesPage.isLoaded()).toBe(true);

    // Select Home link
    await PageNavigation.goTo(page, LINK.HOME);
    await homePage.waitForReady();
    expect(await homePage.isLoaded()).toBe(true);
    
  });

  test('Check Contact Us form', async () => {

    const home = new HomePage(page);
    await home.goToURL();

    const iframeTitle = 'Find more information here';
    let iframeHandle = await page.$(`iframe[title='${iframeTitle}']`);

    // Select Contact Us
    await PageNavigation.goTo(page, LINK.CONTACT_US);

    iframeHandle = await page.waitForSelector(`iframe[title='${iframeTitle}']`);
    const newIframe = await iframeHandle.contentFrame();

    const askQuestionAboutButton = new Button(newIframe);
    await askQuestionAboutButton.withLabel({text: HELP_DESK.ASK_QUESTION});
    expect(await askQuestionAboutButton.isVisible()).toBe(true);

    const reportConcernButton = new Button(newIframe);
    await reportConcernButton.withLabel({text: HELP_DESK.REPORT_DATA_PRIVACY_CONCERN});
    expect(await reportConcernButton.isVisible()).toBe(true);
    await reportConcernButton.dispose();

    const tellAboutPublicationButton = new Button(newIframe);
    await tellAboutPublicationButton.withLabel({text: HELP_DESK.TELL_US_ABOUT_PUBLICATION});
    expect(await tellAboutPublicationButton.isVisible()).toBe(true);
    await tellAboutPublicationButton.dispose();

    const requestBillingCreditsButton = new Button(newIframe);
    await requestBillingCreditsButton.withLabel({text: HELP_DESK.REQUEST_ADDITIONAL_BILLING_CREDITS});
    expect(await requestBillingCreditsButton.isVisible()).toBe(true);
    await requestBillingCreditsButton.dispose();

    const minimizeButton = await newIframe.$('button[aria-label=\'Minimize widget\']');
    await minimizeButton.click();

    expect(await askQuestionAboutButton.isVisible()).toBe(false);
    await askQuestionAboutButton.dispose();

  });

  test('Sign Out', async () => {

    const home = new HomePage(page);
    await home.goToURL();

    // Select Sign Out link
    await PageNavigation.goTo(page, LINK.SIGN_OUT);
    expect(await page.url()).toContain('/login');

  });

});
