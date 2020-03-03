import WebElement from '../../app/aou-elements/WebElement';
import {CreateAccountPage, INSTITUTION_AFFILIATION} from '../../app/CreateAccountPage';
import GoogleLoginPage from '../../app/GoogleLoginPage';
import {findText} from '../../driver/element-util';
import PuppeteerLaunch from '../../driver/puppeteer-launch';
import {waitUntilFindTexts} from '../../driver/waitFuncs';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/workbench-config');

describe('User registration tests:', () => {

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

    const headerDisplayed = await waitUntilFindTexts(page, header);
    expect(headerDisplayed).toBeTruthy();
    const errDisplayed = await findText(page, keyIsNotValidError);
    expect(errDisplayed).toBeFalsy();

    const badInvitationKey = process.env.INVITATION_KEY + '1';
    await createAccountPage.fillOutInvitationKey(badInvitationKey);
    const found = await waitUntilFindTexts(page, keyIsNotValidError);
    expect(await found.jsonValue()).toBeTruthy();
    // Page should be unchanged. User can re-enter invitation key
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

    // Step 2: Checking Accepting Terms of Service.
    const pdfPageCount =  await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length === 9
    }, {timeout: 30000});
    // expecting 9 pages in pdf document
    expect(await pdfPageCount.jsonValue()).toBe(true);

    // Before user read all pdf pages, checkboxes are unchecked and disabled
    const privacyStatementCheckbox = await createAccountPage.getPrivacyStatementCheckbox();
    expect(privacyStatementCheckbox).toBeTruthy();
    expect(await privacyStatementCheckbox.isDisabled()).toBe(true);
    expect(await privacyStatementCheckbox.isChecked()).toBe(false);

    const termsOfUseCheckbox = await createAccountPage.getTermsOfUseCheckbox();
    expect(termsOfUseCheckbox).toBeTruthy();
    expect(await termsOfUseCheckbox.isDisabled()).toBe(true);
    expect(await termsOfUseCheckbox.isChecked()).toBe(false);

    const nextButton = await createAccountPage.getNextButton();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);

    // scroll to last pdf file will enables checkboxes
    await createAccountPage.scrollToLastPdfPage();
    expect(await privacyStatementCheckbox.isDisabled()).toBe(false);
    expect(await termsOfUseCheckbox.isDisabled()).toBe(false);

    // check both checkboxes
    await (await createAccountPage.getPrivacyStatementLabel()).click();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);
    await (await createAccountPage.getTermsOfUseLabel()).click();

    // verify checked
    expect(await privacyStatementCheckbox.isChecked()).toBe(true);
    expect(await termsOfUseCheckbox.isChecked()).toBe(true);
    expect(await nextButton.isCursorNotAllowed()).toEqual(false);

    // uncheck a checkbox then check NEXT button is again disabled
    await (await createAccountPage.getTermsOfUseLabel()).click();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);

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
    await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length === 9
    }, {timeout: 30000});

    await createAccountPage.scrollToLastPdfPage();
    // check on checkboxes
    await (await createAccountPage.getPrivacyStatementLabel()).click();
    await (await createAccountPage.getTermsOfUseLabel()).click();
    const agreementPageButton = await createAccountPage.getNextButton();
    await agreementPageButton.click();

    // Step 3: Enter user information. Should be on Create your account: Step 1 of 2 page
    expect(await waitUntilFindTexts(page, 'Create your account')).toBeTruthy();

    // the NEXT button on User Information page should be disabled until all required fields are filled
    const userInforPageButton = await createAccountPage.getNextButton();
    const cursor = await userInforPageButton.isCursorNotAllowed();
    expect(cursor).toEqual(true);

    // verify username domain
    expect(await createAccountPage.getUsernameDomain()).toBe(configs.userEmailDomain);

    const radioButtonYesSelected = await (await createAccountPage.areYouAffiliatedRadioButton(true)).isSelected();
    expect(radioButtonYesSelected).toBe(true);
    let radioButtonNoSelected = await (await createAccountPage.areYouAffiliatedRadioButton(false)).isSelected();
    expect(radioButtonNoSelected).toBe(false);
    // select No radiobutton
    await (await createAccountPage.areYouAffiliatedRadioButton(false)).select();
    radioButtonNoSelected = await (await createAccountPage.areYouAffiliatedRadioButton(false)).isSelected();
    expect(radioButtonNoSelected).toBe(true);
    await createAccountPage.selectInstitution(INSTITUTION_AFFILIATION.INDUSTRY);

    // verify all input fields are visible and editable on this page
    const allInputs = await page.$$('input', { visible: true });
    for (const aInput of allInputs) {
      const elem = new WebElement(page, aInput);
      const isDisabled = await elem.isDisabled();
      expect(isDisabled).toBe(false);
      await elem.dispose();
    }

  });


});
