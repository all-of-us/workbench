import CreateAccountPage from 'app/page/create-account-page';
import GoogleLoginPage from 'app/page/google-login';
import { getPropValue } from 'utils/element-utils';

describe('User registration tests:', () => {
  test('Can register new user', async () => {
    // Load the landing page for login.
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
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
    const userId = await createAccountPage.fillOutUserInformation();
    nextButton = createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 4: Enter demographic survey (All Survey Fields are optional)
    await createAccountPage.fillOutDemographicSurvey();

    const submitButton = createAccountPage.getSubmitButton();
    await submitButton.waitUntilEnabled();
    await submitButton.click();

    // Verify New account created successfully.

    // "Resend Instructions" link exists
    const linkSelector = '//a[text()="Resend Instructions"]';
    const resendInstrButton = await page.waitForXPath(linkSelector, { visible: true });
    expect(resendInstrButton).toBeTruthy();

    // Displayed texts checks
    const textSelector = '//*[normalize-space(text())]';
    const textElements = await page.$x(textSelector);

    const textsArray = [];
    for (const elemt of textElements) {
      textsArray.push(await getPropValue<string>(elemt, 'textContent'));
    }
    expect(textsArray).toContain('Congratulations!');
    expect(textsArray).toContain('Your All of Us research account has been created!');
    expect(textsArray).toContain(`${userId}@fake-research-aou.org`);

    const note =
      'Please note: For full access to the Research Workbench data and tools, ' +
      "you'll be required to complete the necessary registration steps.";
    expect(textsArray).toContain(note);
  });
});
