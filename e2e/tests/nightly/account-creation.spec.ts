import CreateAccountPage from 'app/page/create-account-page';
import GoogleLoginPage from 'app/page/google-login';

describe.skip('Account creation tests:', () => {
  //TODO: Update test to include filling out the demographics survey and final account creation.
  test('can complete steps 1-3 of account creation', async () => {
    // Load the landing page for login.
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start the new user account creation flow.
    await loginPage.clickCreateAccountButton();

    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.isLoaded();

    // Step 1: Terms of Service.
    await createAccountPage.acceptTermsOfUseAgreement();
    let nextButton = createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 2: Enter institution affiliation details
    await createAccountPage.fillOutInstitution();
    nextButton = createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.clickWithEval();

    // Step 3: Enter user information
    await createAccountPage.fillOutUserInformation();
    nextButton = createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();
  });
});
