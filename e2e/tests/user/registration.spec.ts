import {ElementHandle} from "puppeteer";
import CreateAccountPage from "../../app/create-account-page";
import GoogleLoginPage, {selectors} from '../../app/google-login';
import {waitUntilTitleMatch} from "../../driver/waitFuncs";
import PuppeteerLaunch from '../../services/puppeteer-launch';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe('User registration tests:', () => {

  let browser;
  let incognitoContext;
  let page;

  beforeAll(async () =>  {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    incognitoContext = await browser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await incognitoContext.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  // TODO add verification
  test('Can register new user', async () => {
    // Load the landing page for login.
    const url = configs.uiBaseUrl + configs.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);

    await loginPage.goto();

    // Click the create account button to start new-user-registration flow.


    const createAccountButton = await loginPage.createAccountButton();
    expect(createAccountButton).toBeTruthy();
    await createAccountButton.click();

    // Step 1: invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.getInvitationKeyInput()
       .then(invitationKeyInput => invitationKeyInput.click())
       .then(() => createAccountPage.getSubmitButton())
       .then(submitButton => submitButton.click());

    // add

    // Step 2: terms of service.
    await createAccountPage.scrollToLastPdfPage();
    await page.waitFor(30000);

    // Step 3: Core user registration.

    // Step 4: Demographic survey.

    // Step 5: landing page.

  });

});
