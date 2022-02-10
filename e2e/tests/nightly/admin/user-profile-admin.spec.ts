import UserAdminPage from 'app/page/admin-user-list-page';
import { parseForNumbericalString, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';
import UserProfileAdminPage from 'app/page/user-profile-admin-page';
import { Page } from 'puppeteer';
//import faker from 'faker';
import { waitForText } from 'utils/waits-utils';

describe('User Admin', () => {
  const userEmail = 'admin_test';
  let newPage: Page;

  beforeEach(async () => {
    newPage = await searchTestUser(page, userEmail);
  });

  test('Verify User Profile Admin page render correctly for test user', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(newPage);
    await userProfileAdminPage.waitForLoad();

    // SAVE and CANCEL buttons are disabled
    expect(await userProfileAdminPage.getSaveButton().isCursorNotAllowed()).toBe(true);
    expect(await userProfileAdminPage.getCancelButton().isCursorNotAllowed()).toBe(true);

    // verify Researcher information, Editable fields.
    // Edit Information block
    // Verify Institution
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const institutionName = await verifiedInstitution.getSelectedValue();
    expect(institutionName).toEqual('Admin testing');

    // Verify Institution dropdown is not empty. Checks existence of few Select options.
    let options = await verifiedInstitution.getAllOptionTexts();
    expect(options.length).toBeGreaterThan(1);
    expect(options).toEqual(
      expect.arrayContaining(['Broad Institute', 'Google', 'Verily LLC', 'Vanderbilt University Medical Center'])
    );

    // Verify Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const email = await contactEmail.getValue();
    expect(email).toEqual('testing@vumc.org');

    // Get Initial Credits Limit
    const creditLimit = userProfileAdminPage.getInitialCreditLimit();
    const credits = parseInt(parseForNumbericalString(await creditLimit.getSelectedValue())[0]);

    // Verify Initial Credits Limit dropdown is not empty. Checks existence of few Select options.
    options = await creditLimit.getAllOptionTexts();
    expect(options.length).toBeGreaterThan(1);
    expect(options).toEqual(expect.arrayContaining(['$300.00', '$500.00', '$800.00', '$1000.00']));

    // Get Institution Role
    const institutionRole = userProfileAdminPage.getInstitutionalRole();
    const role = await institutionRole.getSelectedValue();
    options = await institutionRole.getAllOptionTexts();
    expect(options.length).toBeGreaterThan(1);
    expect(options).toEqual(
      expect.arrayContaining([
        role,
        'Student',
        'Undergraduate (Bachelor level) student',
        'Teacher/Instructor/Professor',
        'Research Assistant (pre-doctoral)'
      ])
    );
    /// Above array might exist

    // Researcher Information block
    // Verify Name
    const name = await userProfileAdminPage.getName();
    expect(name).toEqual('Admin Testing');

    // Verify User Name
    const userName = await userProfileAdminPage.getUserName();
    expect(userName).toEqual(`${userEmail}${config.EMAIL_DOMAIN_NAME}`);

    // Verify Data Access Tiers is Registered Tier only
    const accessTiers = await userProfileAdminPage.getDataAccessTiers();
    expect(accessTiers).toEqual('Registered Tier');
    expect(accessTiers).not.toContain('Controlled Tier');

    // Verify Credits Used
    const [usedAmount, creditsLimitAmount] = await userProfileAdminPage.getInitialCreditsUsed();
    expect(usedAmount).toEqual(0);
    expect(creditsLimitAmount).toEqual(credits); // Match value in Initial Credit Limit Select dropdown

    // Verify expected columns in Account Access table
    const accountAccessTable = userProfileAdminPage.getAccessStatusTable();
    const columnNames = await accountAccessTable.getColumnNames();
    expect(columnNames).toEqual(
      expect.arrayContaining([
        'Access Module',
        'Status',
        'Last completed on',
        'Expires on',
        'Required for tier access',
        'Bypass'
      ])
    );
    // above create array. check array has them not others

    // Verify Access Module names in Column #1
    const accessModuleNames = await accountAccessTable.getRowValues(1);
    expect(accessModuleNames).toEqual(
      expect.arrayContaining([
        'Google 2-Step Verification',
        'Registered Tier training',
        'Controlled Tier training',
        'Sign Data User Code of Conduct',
        'Verify your identity with Login.gov',
        'Update your profile',
        'Report any publications'
      ])
    );
  });

  test('Verify CANCEL and SAVE buttons work correctly in User Profile Admin page', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(newPage);
    await userProfileAdminPage.waitForLoad();

    // Verify Account access toggle text label update after toggle
    const accountAccessSwitch = userProfileAdminPage.getAccountAccessSwitch();
    let label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    await accountAccessSwitch.turnOff();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account disabled');

    // CANCEL button is enabled
    const cancelButton = userProfileAdminPage.getCancelButton();
    expect(await cancelButton.isCursorNotAllowed()).toBe(false);
    // SAVE button is enabled
    const saveButton = userProfileAdminPage.getSaveButton();
    expect(await saveButton.isCursorNotAllowed()).toBe(false);

    await accountAccessSwitch.turnOn();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    // CANCEL button is disabled
    expect(await cancelButton.isCursorNotAllowed()).toBe(true);
    // SAVE button is disabled
    expect(await saveButton.isCursorNotAllowed()).toBe(true);

    // Change Initial Credits Limit to $400
    const initialCreditLimit = userProfileAdminPage.getInitialCreditLimit();
    const originalCreditLimit = await initialCreditLimit.getSelectedValue();

    await initialCreditLimit.select('$400.00');
    expect(await initialCreditLimit.getSelectedValue()).toEqual('$400.00');

    /*
    // Change Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const originalContactEmail = await contactEmail.getTextContent();

    await contactEmail.type(`${faker.name.firstName()}@gmail.com`);
*/

    // Change Verified Institution
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const originalInstitution = await verifiedInstitution.getSelectedValue();
    await verifiedInstitution.select('Broad Institute');

    // edit code to fall in same parent node
    // Check error when Verified Institution and Contact Email domains do not match
    await waitForText(newPage, 'Your email does not match your institution');

    // CANCEL button is enabled
    expect(await cancelButton.isCursorNotAllowed()).toBe(false);
    // SAVE buttons is disabled due to error in page
    expect(await saveButton.isCursorNotAllowed()).toBe(true);

    // After click CANCEL button, unsaved changes are automatically discarded
    await cancelButton.click();
    await cancelButton.waitUntilDisabled();

    const newCreditLimit = await initialCreditLimit.getSelectedValue();
    expect(newCreditLimit).toEqual(originalCreditLimit);
    /*
    const newContactEmail = await contactEmail.getTextContent();
    expect(newContactEmail).toEqual(originalContactEmail);
*/
    const newVerifiedInstitution = await verifiedInstitution.getSelectedValue();
    expect(newVerifiedInstitution).toEqual(originalInstitution);
  });

  xtest('Can navigate to User Audit page from User Admin Profile page', async () => {
    // blank
  });

  xtest('Verify test user expected state in User Admin Profile page', async () => {
    // Verify information in all columns for Google 2-Step Verification, era common, login.gov in table
  });

  xtest('Can modify user in User Admin Profile page', async () => {
    // Modify Initial credit limit: check highlights, SAVE button enabled, change back. check highlights gone, SAVE disabled.
    // Change 1 Bypass toggle: check highlight, SAVE. Status column update.
    // Change multiple Bypass toggles: check highlight, SAVE. Status column update.
    // Moke changes in multiple fields (Account access toggle, etc.), click CANCEL, verify values unchaged
    // change Account enabled toggle, check label updated, change back, label updated. change again, save, check label
    // change institution, check "Your email does not match your institution". SAVE button remain disabled
    // check BACK (admin/user) button working
  });
});

async function searchTestUser(page: Page, userName: string): Promise<Page> {
  await signInWithAccessToken(page, config.ADMIN_TEST_USER);
  await navigation.navMenu(page, NavLink.USER_ADMIN);

  const userAdminPage = new UserAdminPage(page);
  await userAdminPage.waitForLoad();

  // Search for user
  await userAdminPage.searchUser(userName);

  const adminTable = new AdminTable(page);
  const nameColumnIndex = await adminTable.getNameColindex();
  // Click name to navigate to User Profile Admin page
  await userAdminPage.clickUserName(1, nameColumnIndex);

  const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
  const newPage = await newTarget.page();

  await new UserProfileInfo(newPage).waitForLoad();

  // Go to url users-tmp page
  const tmpUrl = replaceWithTmpUrl(newPage.url());
  await newPage.goto(tmpUrl, { waitUntil: ['load', 'networkidle0'] });

  return newPage;
}

function replaceWithTmpUrl(url: string): string {
  return url.replace('users', 'users-tmp');
}

// create bug change institution, values not restored
