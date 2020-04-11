import BaseElement from 'app/aou-elements/base-element';
import CreateAccountPage from 'app/create-account-page';
import GoogleLoginPage from 'app/google-login';

const configs = require('resources/workbench-config');


describe('User registration tests:', () => {

  beforeEach(async () => {
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(120000);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });


  test('Loading Terms of Use and Privacy statement page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    const createAccountPage = new CreateAccountPage(page);
    // Step 1: Checking Accepting Terms of Service.
    const pdfPage =  await createAccountPage.getPdfPage();
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

    const createAccountPage = new CreateAccountPage(page);
    // Step 1 of 3: Accepting Terms of Service.
    await createAccountPage.scrollToLastPdfPage();

    // check checkboxes
    await (await createAccountPage.getPrivacyStatementCheckbox()).check();
    await (await createAccountPage.getTermsOfUseCheckbox()).check();
    const agreementPageButton = await createAccountPage.getNextButton();
    await agreementPageButton.clickWithEval();

    // Step 2 of 3: Enter Institution information
    const nextButton = await createAccountPage.getNextButton();
    expect(await nextButton.isCursorNotAllowed()).toEqual(true);

    await createAccountPage.fillOutInstitution();

    await nextButton.waitUntilEnabled();
    expect(await nextButton.isCursorNotAllowed()).toEqual(false);
    await nextButton.clickWithEval();

    // Step 3 of 3: Enter user information.
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
