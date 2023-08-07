import { generate2FACode, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import DataAccessRequirementsPage, { AccessModule } from 'app/page/data-access/data-access-requirements-page';
import Navigation, { NavLink } from 'app/component/navigation';
import HomePage from 'app/page/home-page';
import expect from 'expect';
import { waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';

/**
 * NIH Researcher Auth Service (RAS) Login Test
 *
 * Important:
 *   Running this test requires setting RAS module completion_time to NULL in DB for this test user.
 *
 *   CircleCI accomplishes this by running the puppeteer-access-test-user-setup task, which executes
 *   `./project.rb set-access-module-timestamps --profile-user ${ACCESS_TEST_USER} --ras-user ${RAS_TEST_USER}`
 *   This can also be run locally in workbench/api dir.
 */
describe.skip('RAS Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page, config.RAS_TEST_USER, { waitForLoad: false });
  });

  test('Sign in login.gov', async () => {
    // To run this test on localhost, set following environment variables in Terminal.
    // Get secret values from Google bucket:
    //  `gsutil cp gs://all-of-us-workbench-test-credentials/ras-secrets.txt .`

    // Verify env variables are not empty strings
    expect(config.LOGIN_GOV_2FA_SECRET.length).toBeGreaterThan(0);
    expect(config.LOGIN_GOV_USER.length).toBeGreaterThan(0);
    expect(config.LOGIN_GOV_PASSWORD.length).toBeGreaterThan(0);

    const oneTimeCode = generate2FACode(config.LOGIN_GOV_2FA_SECRET);
    const govLoginUser = config.LOGIN_GOV_USER;
    const govLoginPassword = config.LOGIN_GOV_PASSWORD;

    // First page opened after sign in could be the Home page or Data Access Requirements page.
    // Because this test runs after a DB change, but we don't know exactly when the system has yet had time
    // to perform access-sync. Therefore they will not be automatically redirected to DAR.
    // We can still test that DAR shows the user's lack of RAS completion.

    const homePage = new HomePage(page);
    await homePage.isSignedIn();
    await waitWhileLoading(page);

    if (await homePage.exists()) {
      await Navigation.navMenu(page, NavLink.DATA_ACCESS_REQUIREMENTS);
    }
    // else, the page is Data Access Requirements page

    const dataAccessPage = new DataAccessRequirementsPage(page);
    await dataAccessPage.waitForLoad();

    expect(await dataAccessPage.findModule(AccessModule.RAS).hasCompletedModule()).toBe(false);

    const verifyIdentityButton = dataAccessPage.findModule(AccessModule.RAS).getClickableText();
    expect(await verifyIdentityButton.exists()).toBe(true);

    // Verify partial text found in button
    const text = await verifyIdentityButton.getTextContent();
    expect(text).toMatch(/Verify your identity with Login.gov/);
    expect(text).toMatch(/ontact us if you’re having trouble completing this step/);

    // New page open up
    const newPage = await dataAccessPage.clickModule(AccessModule.RAS);
    await newPage.waitForTimeout(1000);

    // Verify page text
    await newPage.waitForXPath('//img[@src="images/logos/AllofUs.png" and @class="header-logo"]', { visible: true });
    await newPage.waitForXPath('//h2[.="NIH Researcher Auth Service (RAS)"]', { visible: true });

    // Click login.gov button
    const loginButton = new Button(
      newPage,
      '//a[@href="javascript:submitSocialLogin(1)" and contains(normalize-space(.), "Login.gov")]'
    );
    await loginButton.click();

    // Type in user email and password
    const emailInput = await newPage.waitForXPath('//input[@id="user_email"]', { visible: true });
    await emailInput.type(govLoginUser);

    const passwordInput = await newPage.waitForXPath('//input[@type="password"]', { visible: true });
    await passwordInput.type(govLoginPassword);

    const signInButton = await newPage.waitForXPath('//button[contains(text(), "Sign in") and @type="submit"]', {
      visible: true
    });
    await signInButton.click();

    // Type one-time code
    const oneTimeCodeInput = await newPage.waitForXPath('//input[@id=//label[text()="One-time code"]/@for]', {
      visible: true
    });
    await oneTimeCodeInput.type(oneTimeCode);

    const submitButton = await newPage.waitForXPath('//button[contains(text(), "Submit") and @type="submit"]', {
      visible: true
    });
    await submitButton.click();

    // The test user needs to periodically re-consent.  This won't happen every test run.

    try {
      const maybeAgreeAndContinueButton = await newPage.waitForXPath(
        '////button[contains(text(),"Agree and continue") and @type="submit"]',
        {
          visible: true
        }
      );
      await maybeAgreeAndContinueButton.click();
    } catch (err) {
      // Blank
    }
    try {
      const maybeGrantButton = await newPage.waitForXPath('//input[@value="Grant" and @type="submit"]', {
        visible: true
      });
      await maybeGrantButton.click();
    } catch (err) {
      // Blank
    }

    // Automatically loads AoU Data Access Requirements page after sign in
    await new DataAccessRequirementsPage(newPage).waitForLoad(); // This is waiting for sign in to complete

    // Reloads DAR page to refresh RAS status in first page tab
    await page.bringToFront();
    await page.reload({ waitUntil: ['load', 'networkidle0'] });
    await dataAccessPage.waitForLoad();

    expect(await dataAccessPage.findModule(AccessModule.RAS).hasCompletedModule()).toBe(true);
  });
});
