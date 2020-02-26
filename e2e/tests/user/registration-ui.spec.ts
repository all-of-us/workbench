import CreateAccountPage from '../../app/create-account-page';
import WebElement from '../../app/elements/web-element';
import GoogleLoginPage from '../../app/google-login';
import {findText, getCursorValue} from '../../driver/element-util';
import {waitForText, waitUntilFindTexts} from '../../driver/waitFuncs';
import PuppeteerLaunch from '../../driver/puppeteer-launch';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe('User registration tests:', () => {

  const url = configs.uiBaseUrl + configs.workspacesUrlPath;
  let browser;
  let page;

  beforeAll(async () => {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await page.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  test('Entered non-empty invalid invitation key', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.goto();

    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    const createAccountPage = new CreateAccountPage(page);

    const keyIsNotValidError = 'Invitation Key is not Valid.';
    const header = 'Enter your Invitation Key:';

    const headerDisplayed = await waitForText(page, 'h2', header);
    expect(headerDisplayed).toBeTruthy();

    const errDisplayed = await findText(page, keyIsNotValidError);
    expect(errDisplayed).toBeFalsy();

    const badInvitationKey = process.env.INVITATION_KEY + '1'; // append a number to turn good key to invalid key
    await createAccountPage.fillOutInvitationKey(badInvitationKey);

    const found = await waitUntilFindTexts(page, keyIsNotValidError);
    expect(await found.jsonValue()).toBeTruthy();
    // Page should be unchanged
    expect(await createAccountPage.getInvitationKeyInput()).toBeTruthy();
  });

  test('Loading Terms of Use and Privacy statement page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.goto();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    // Step 1: Enter invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);
    await page.waitFor(1000);

    // Step 2: Accepting Terms of Service.
    const pdfPageCount =  await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length === 9
    }, {timeout: 30000});
    // expecting 9 pages in pdf document
    expect(await pdfPageCount.jsonValue()).toBe(true);

    // Before user read all pdf pages, checkboxes are unchecked and disabled
    const privacyStatementCheckbox = await createAccountPage.getPrivacyStatementCheckbox();
    expect(privacyStatementCheckbox).toBeTruthy();
    expect(await (new WebElement(privacyStatementCheckbox).getAttr('disabled'))).toBe('');
    expect(await (new WebElement(privacyStatementCheckbox).getProp('checked'))).toBe(false);

    const termsOfUseCheckbox = await createAccountPage.getTermsOfUseCheckbox();
    expect(termsOfUseCheckbox).toBeTruthy();
    expect(await (new WebElement(termsOfUseCheckbox).getAttr('disabled'))).toBe('');
    expect(await (new WebElement(termsOfUseCheckbox).getProp('checked'))).toBe(false);

    const nextButton = await createAccountPage.getNextButton();
    // Next button should be disabled
    const cursor = await getCursorValue(page, nextButton);
    expect(cursor).toEqual('not-allowed');

    // scroll to last pdf file will enables checkboxes
    await createAccountPage.scrollToLastPdfPage();

    expect(await (new WebElement(privacyStatementCheckbox).getAttr('disabled'))).toBeNull();
    expect(await (new WebElement(termsOfUseCheckbox).getAttr('disabled'))).toBeNull();

    // check on checkboxes
    await (await createAccountPage.getPrivacyStatementLabel()).click();
    await (await createAccountPage.getTermsOfUseLabel()).click();

    // verify checked
    expect(await (new WebElement(privacyStatementCheckbox).getProp('checked'))).toBe(true);
    expect(await (new WebElement(termsOfUseCheckbox).getProp('checked'))).toBe(true);
  });

  test('Loading User information page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.goto();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    // Step 1: Enter invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);

    // Step 2: Accepting Terms of Service.
    const pdfPageCount =  await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length === 9
    }, {timeout: 30000});

    // Before user read all pdf pages, checkboxes are unchecked and disabled
    const privacyStatementCheckbox = await createAccountPage.getPrivacyStatementCheckbox();
    const termsOfUseCheckbox = await createAccountPage.getTermsOfUseCheckbox();
    const agreementPageButton = await createAccountPage.getNextButton();

    await createAccountPage.scrollToLastPdfPage();
    // check on checkboxes
    await (await createAccountPage.getPrivacyStatementLabel()).click();
    await (await createAccountPage.getTermsOfUseLabel()).click();
    await agreementPageButton.click();

    // Step 3: Enter user information. Should be on Create your account: Step 1 of 2 page
    expect(await waitUntilFindTexts(page, 'Create your account')).toBeTruthy();

    // the NEXT button on User Information page should be disabled until all required fields are filled
    const userInforPageButton = await createAccountPage.getNextButton();
    const cursor = await getCursorValue(page, userInforPageButton);
    expect(cursor).toEqual('not-allowed');

    // verify username domain
    expect(await createAccountPage.getUsernameDomain()).toBe(configs.userEmailDomain);

    // verify all input fields are visible and editable on this page
    const allInputs = await page.$$('input', { visible: true });
    for (const aInput of allInputs) {
      const isDisabled = await (new WebElement(aInput)).getAttr('disabled');
      expect(isDisabled).toBeNull();
    }

  });

});
