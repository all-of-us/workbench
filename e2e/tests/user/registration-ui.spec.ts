import BaseElement from '../../app/aou-elements/base-element';
import SelectComponent from '../../app/aou-elements/select-component';
import Textbox from '../../app/aou-elements/textbox';
import CreateAccountPage, {FIELD_LABEL, INSTITUTION_ROLE_VALUE, INSTITUTION_VALUE} from '../../app/create-account-page';
import GoogleLoginPage from '../../app/google-login';
import PuppeteerLaunch from '../../driver/puppeteer-launch';

const configs = require('../../resources/workbench-config');

// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

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


  test('Entered invalid invitation key', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    const createAccountPage = new CreateAccountPage(page);
    const headerDisplayed = await createAccountPage.waitForTextExists('Enter your Invitation Key:');
    expect(headerDisplayed).toBeTruthy();
    const keyIsNotValidError = 'Invitation Key is not Valid.';
    const errDisplayed = await createAccountPage.findText(keyIsNotValidError);
    expect(errDisplayed).toBeFalsy();

    const badInvitationKey = process.env.INVITATION_KEY + '1';
    await createAccountPage.fillOutInvitationKey(badInvitationKey);
    const found = await createAccountPage.waitForTextExists(keyIsNotValidError);
    expect(await found.jsonValue()).toBeTruthy();
    // Page should be unchanged. User can re-enter invitation key
    expect(await createAccountPage.getInvitationKeyInput()).toBeTruthy();
  });


  test('Loading Terms of Use and Privacy statement page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    // Step 1: Enter invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);

    // Step 2: Checking Accepting Terms of Service.
    const pdfPage =  await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length > 1
    }, {timeout: 30000});
    // expecting pdf document
    expect(await pdfPage.jsonValue()).toBe(true);

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
    await (await createAccountPage.getPrivacyStatementCheckbox()).check();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);
    await (await createAccountPage.getTermsOfUseCheckbox()).check();

    // verify checked
    expect(await privacyStatementCheckbox.isChecked()).toBe(true);
    expect(await termsOfUseCheckbox.isChecked()).toBe(true);
    expect(await nextButton.isCursorNotAllowed()).toEqual(false);

    // uncheck a checkbox then check NEXT button is again disabled
    await (await createAccountPage.getTermsOfUseCheckbox()).unCheck();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);
  });


  test('Loading User information page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    // Step 1: Enter invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);

    // Step 2: Accepting Terms of Service.
    await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length > 1
    }, {timeout: 30000});

    await createAccountPage.scrollToLastPdfPage();

    // check checkboxes
    await (await createAccountPage.getPrivacyStatementCheckbox()).check();
    await (await createAccountPage.getTermsOfUseCheckbox()).check();
    const agreementPageButton = await createAccountPage.getNextButton();
    await agreementPageButton.click();

    // Step 1 of 3: Enter Institution information
    const nextButton = await createAccountPage.getNextButton();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);

    const institutionSelect = new SelectComponent(page, 'Select your institution');
    await institutionSelect.select(INSTITUTION_VALUE.VANDERBILT);

    const emailAddress = await Textbox.forLabel(page, {textContains: FIELD_LABEL.INSTITUTION_EMAIL, ancestorNodeLevel: 2});
    await emailAddress.type(configs.contactEmail);

    const roleSelect = new SelectComponent(page, 'describes your role');
    await roleSelect.select(INSTITUTION_ROLE_VALUE.RESEARCH_ASSISTANT);

    await nextButton.waitUntilEnabled();
    await nextButton.focus();
    expect(await nextButton.isCursorNotAllowed()).toEqual(false);
    await nextButton.clickWithEval();

    // Step 2 of 3: Enter user information.
    expect(await createAccountPage.waitForTextExists('Create your account')).toBeTruthy();

    // verify username domain
    expect(await createAccountPage.getUsernameDomain()).toBe(configs.userEmailDomain);

    // verify all input fields are visible and editable on this page
    const allInputs = await page.$$('input');
    for (const aInput of allInputs) {
      const elem = new BaseElement(page, aInput);
      const isDisabled = await elem.isDisabled();
      expect(isDisabled).toBe(false);
      const value = await elem.getTextContent();
      expect(value).toBe(''); // empty value
      await elem.dispose();
    }

    // the NEXT button on User Information page should be disabled until all required fields are filled
    const userInforPageButton = await createAccountPage.getNextButton();
    await userInforPageButton.isDisplayed();
    const cursor = await userInforPageButton.isCursorNotAllowed();
    expect(cursor).toEqual(true);

  });


});
