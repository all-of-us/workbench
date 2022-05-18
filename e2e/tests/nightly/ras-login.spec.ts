import { generate2FACode, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import DataAccessRequirementsPage, { AccessModule } from 'app/page/data-access/data-access-requirements-page';
import expect from 'expect';
import Navigation, { NavLink } from 'app/component/navigation';

/**
 * NIH Researcher Auth Service (RAS) Login Test
 *
 * Important:
 *   The access test user must be in a state where they are currently failing access renewal
 *   due to an expired "profile last confirmed" date.
 *   CircleCI accomplishes this by running the puppeteer-access-test-user-setup task, which executes
 *   `./project.rb set-access-module-timestamps --profile-user ${ACCESS_TEST_USER} --ras-user ${RAS_TEST_USER}`
 *   This can also be run locally in workbench/api dir.
 */
describe('RAS Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page, config.RAS_TEST_USER);
  });

  // note that this test is "destructive" in that it brings the user to a state
  // where they cannot complete this test again, because they have completed
  // DAR and are no longer forced into renewal

  test('Sign in login.gov', async () => {
    // At this time, first page opened after sign in is still the Home page
    await Navigation.navMenu(page, NavLink.DATA_ACCESS_REQUIREMENTS);

    // Verify configs are not empty strings
    expect(config.LOGIN_GOV_2FA_SECRET.length).toBeGreaterThan(0);
    expect(config.LOGIN_GOV_USER.length).toBeGreaterThan(0);
    expect(config.LOGIN_GOV_PASSWORD.length).toBeGreaterThan(0);

    const oneTimeCode = generate2FACode(config.LOGIN_GOV_2FA_SECRET);
    const govLoginUser = config.LOGIN_GOV_USER;
    const govLoginPassword = config.LOGIN_GOV_PASSWORD;

    const dataAccessPage = new DataAccessRequirementsPage(page);
    await dataAccessPage.waitForLoad();

    const verifyIdentityButton = dataAccessPage.getModuleButton(AccessModule.RAS);
    expect(await verifyIdentityButton.exists()).toBe(true);

    // Verify partial text found in button
    const text = await verifyIdentityButton.getTextContent();
    expect(text).toMatch(/Verify your identity with Login.gov/);
    expect(text).toMatch(/ontact us if you’re having trouble completing this step/);

    // New page open up
    const newPage = await dataAccessPage.clickModuleButton(AccessModule.RAS);

    // Verify All-of-Us logo
    await newPage.waitForXPath('//img[@src="images/logos/AllofUs.png" and @class="header-logo"]', { visible: true });

    // Click login.gov button
    const loginButton = await newPage.waitForXPath(
      '//a[@href="javascript:submitSocialLogin(1)" and contains(normalize-space(.), "Login.gov")]',
      { visible: true }
    );
    await loginButton.click();

    // Type in user email and password
    const emailInput = await newPage.waitForXPath('//input[@id="user_email"]', { visible: true });
    await emailInput.type(govLoginUser);

    const passwordInput = await newPage.waitForXPath('//input[@type="password"]', { visible: true });
    await passwordInput.type(govLoginPassword);

    const signInButton = await newPage.waitForXPath('//input[@value="Sign in" and @type="submit"]', { visible: true });
    await signInButton.click();

    // Type one-time code
    const oneTimeCodeInput = await newPage.waitForXPath('//input[@id=//label[text()="One-time code"]/@for]', {
      visible: true
    });
    await oneTimeCodeInput.type(oneTimeCode);

    const submitButton = await newPage.waitForXPath('//input[@value="Submit" and @type="submit"]', { visible: true });
    await submitButton.click();

    // Automatically loads AoU Data Access Requirements page after sign in
    await new DataAccessRequirementsPage(newPage).waitForLoad(); // This is waiting for sign in to complete

    // Reloads DAR page to refresh RAS status in first page tab
    await page.bringToFront();
    await page.reload({ waitUntil: ['load', 'networkidle0'] });
    await dataAccessPage.waitForLoad();

    expect(await dataAccessPage.hasCompletedModule(AccessModule.RAS)).toBe(true);
  });
});
