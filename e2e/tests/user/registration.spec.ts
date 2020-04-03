import CreateAccountPage from '../../app/create-account-page';
import GoogleLoginPage from '../../app/google-login';


describe('User registration tests:', () => {

  beforeEach(async () => {
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(120000);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });


  test.skip('Can register new user', async () => {
    // Load the landing page for login.
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    const createAccountPage = new CreateAccountPage(page);

    // Step 1: Terms of Service.
    await createAccountPage.acceptTermsOfUseAgreement();
    let nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 2: Enter institution affiliation details
    await createAccountPage.fillOutInstitution();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.clickWithEval();

    // Step 3: Enter user information
    await createAccountPage.fillOutUserInformation();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 4: Enter demographic survey (All Survey Fields are optional)
    await createAccountPage.fillOutDemographicSurvey();

    // TODO uncomment after disable recaptcha
    // const submitButton = await createAccountPage.getSubmitButton();
    // await submitButton.click();

    // Step 5: New account created successfully page.
    // await createAccountPage.waitForTextExists('Congratulations!');
    // await createAccountPage.waitForTextExists('Your new research workbench account');

    // const resendButton = await findClickable(page, 'Resend Instructions');
    // expect(resendButton).toBeTruthy();
  });

});
