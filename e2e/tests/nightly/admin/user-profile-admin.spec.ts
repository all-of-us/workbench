import UserAdminPage from 'app/page/admin-user-list-page';
import { parseForNumbericalString, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';
import UserProfileAdminPage from 'app/page/user-profile-admin-page';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { Institution, InstitutionRole } from 'app/text-labels';
import { getPropValue, getStyleValue } from 'utils/element-utils';
import Button from 'app/element/button';
import Cell from 'app/component/cell';
import UserAuditPage from 'app/page/admin-user-audit-page';
import { isBlank } from 'utils/str-utils';
import fp from 'lodash/fp';

describe('User Profile Admin', () => {
  enum TableColumns {
    ACCESS_MODULE = 'Access Module',
    STATUS = 'Status',
    LAST_COMPLETED_ON = 'Last completed on',
    EXPIRES_ON = 'Expires on',
    REQUIRED_FOR_TIER = 'Required for tier access',
    BYPASS = 'Bypass'
  }

  const accessStatusTableColumns = [
    TableColumns.ACCESS_MODULE,
    TableColumns.STATUS,
    TableColumns.LAST_COMPLETED_ON,
    TableColumns.EXPIRES_ON,
    TableColumns.REQUIRED_FOR_TIER,
    TableColumns.BYPASS
  ];

  enum AccessModules {
    GOOGLE_VERIFICATION = 'Google 2-Step Verification',
    ERA = 'Connect your eRA Commons* account',
    RT_TRAINING = 'Registered Tier training',
    CT_TRAINING = 'Controlled Tier training',
    DUCC = 'Sign Data User Code of Conduct',
    LOGIN_GOV = 'Verify your identity with Login.gov',
    UPDATE_PROFILE = 'Update your profile',
    PUBLICATION = 'Report any publications'
  }

  const accessModules = [
    AccessModules.GOOGLE_VERIFICATION,
    AccessModules.ERA,
    AccessModules.RT_TRAINING,
    AccessModules.CT_TRAINING,
    AccessModules.DUCC,
    AccessModules.LOGIN_GOV,
    AccessModules.UPDATE_PROFILE,
    AccessModules.PUBLICATION
  ];

  const testUserEmail = 'admin_test';
  let adminTab: Page;

  beforeEach(async () => {
    adminTab = await searchTestUser(page, testUserEmail);
  });

  test('Verify Researcher Information renders correctly', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // SAVE and CANCEL buttons should be disabled
    const saveButton = userProfileAdminPage.getSaveButton();
    const cancelButton = userProfileAdminPage.getCancelButton();
    await verifyButtonEnabled(cancelButton, false);
    await verifyButtonEnabled(saveButton, false);

    // Verify Verified Institution
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const institutionName = await verifiedInstitution.getSelectedValue();
    expect(institutionName).toEqual('Admin testing');

    // Verify Institution dropdown is not empty. Checks existence of some Select options.
    let options = await verifiedInstitution.getAllOptionTexts();
    expect(options).toEqual(
      expect.arrayContaining([Institution.Broad, Institution.Google, Institution.Verily, Institution.Vanderbilt])
    );

    // Verify Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const email = await contactEmail.getValue();
    expect(email).toEqual('testing@vumc.org');

    // Get Initial Credits Limit
    const creditLimit = userProfileAdminPage.getInitialCreditLimit();
    const credits = parseInt(parseForNumbericalString(await creditLimit.getSelectedValue())[0]);

    // Verify Initial Credits Limit dropdown is not empty. Checks existence of some Select options.
    options = await creditLimit.getAllOptionTexts();
    expect(options).toEqual(expect.arrayContaining(['$300.00', '$500.00', '$800.00', '$1000.00']));

    // Get Institution Role
    const institutionRole = userProfileAdminPage.getInstitutionalRole();
    const role = await institutionRole.getSelectedValue();
    expect(isBlank(role)).toBeFalsy();
    // Verify Institution Role dropdown is not empty. Checks existence of some Select options.
    options = await institutionRole.getAllOptionTexts();
    expect(options).toEqual(
      expect.arrayContaining([
        InstitutionRole.UndergraduteStudent,
        InstitutionRole.Teacher,
        InstitutionRole.ResearchAssistant
      ])
    );

    // Researcher Information block
    // Verify Name
    const name = await userProfileAdminPage.getName();
    expect(name).toEqual('Admin Testing');

    // Verify User Name
    const userName = await userProfileAdminPage.getUserName();
    expect(userName).toEqual(`${testUserEmail}${config.EMAIL_DOMAIN_NAME}`);

    // Verify Credits Used
    const [usedAmount, creditsLimitAmount] = await userProfileAdminPage.getInitialCreditsUsed();
    expect(usedAmount).toEqual(0);
    expect(creditsLimitAmount).toEqual(credits); // Matches Initial Credit Limit selected value
  });

  test('Verify Access Status table render correctly', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // Verify column headers displayed correctly
    const accountAccessTable = userProfileAdminPage.getAccessStatusTable();
    const columnNames = await accountAccessTable.getColumnNames();
    expect(columnNames.sort()).toEqual(accessStatusTableColumns.sort());
    expect(columnNames.length).toEqual(accessStatusTableColumns.length);

    // Verify Access module names displayed correctly
    const moduleNames = await accountAccessTable.getRowValues(1);
    expect(moduleNames.sort()).toEqual(accessModules.sort());
    expect(moduleNames.length).toEqual(accessModules.length);

    // check badges
    // $x('//*[@id="Registered-user"]')
    // $x('//*[@id="Controlled-user"]')
  });

  test('Verify CANCEL and SAVE buttons work correctly', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // Verify Account access toggle text label update after toggle
    const accountAccessSwitch = userProfileAdminPage.getAccountAccessSwitch();
    let label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    const saveButton = userProfileAdminPage.getSaveButton();
    const cancelButton = userProfileAdminPage.getCancelButton();
    await verifyButtonEnabled(cancelButton, false);
    await verifyButtonEnabled(saveButton, false);

    // Turn off Account Access toggle
    await accountAccessSwitch.turnOff();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account disabled');

    // Undo toggle change
    await accountAccessSwitch.turnOn();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    await verifyButtonEnabled(cancelButton, false);
    await verifyButtonEnabled(saveButton, false);

    // Change Initial Credits Limit to $400
    const initialCreditLimit = userProfileAdminPage.getInitialCreditLimit();
    const oldCreditLimit = await initialCreditLimit.getSelectedValue();
    await initialCreditLimit.select('$400.00');

    // Verify background color in Select has changed
    const [styleElement] = await (await initialCreditLimit.asElement()).$x('./preceding-sibling::style');
    const styleText = await getPropValue<string>(styleElement, 'innerText');
    expect(styleText).toContain('background-color: #F8C954;');

    await verifyButtonEnabled(cancelButton, true);
    await verifyButtonEnabled(saveButton, true);

    // Change Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const oldContactEmail = await contactEmail.getValue();
    // Input background color is white before change
    let backgroundColor = await getStyleValue<string>(
      adminTab,
      await contactEmail.asElementHandle(),
      'background-color'
    );
    expect(backgroundColor).toEqual('rgb(255, 255, 255)');

    // Type in modified email
    const invalidContactEmail = 'mod-' + oldContactEmail;
    await contactEmail.type(invalidContactEmail);

    // Input background color changed
    backgroundColor = await getStyleValue<string>(adminTab, await contactEmail.asElementHandle(), 'background-color');
    expect(backgroundColor).toEqual('rgb(248, 201, 84)');

    // Find email error
    const emailErrorMsg = await userProfileAdminPage.getEmailErrorMessage();
    expect(emailErrorMsg).toContain('The institution has authorized access only to select members');

    await verifyInstitutionAgreementPage(adminTab);
    await adminTab.bringToFront();

    await verifyButtonEnabled(cancelButton, true);
    // SAVE buttons is disabled due to error in page
    await verifyButtonEnabled(saveButton, false);

    // Undo all changes
    await cancelButton.click();
    await cancelButton.waitUntilDisabled();

    // Change Verified Institution that cause domain mismatch
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const oldInstitution = await verifiedInstitution.getSelectedValue();
    await verifiedInstitution.select('Broad Institute');

    // Check error when Verified Institution and Contact Email domains do not match
    await waitForText(adminTab, 'Your email does not match your institution');

    await verifyButtonEnabled(cancelButton, true);
    // SAVE buttons is disabled due to error in page
    await verifyButtonEnabled(saveButton, false);

    // After click CANCEL button, unsaved changes are automatically discarded
    await cancelButton.click();
    await cancelButton.waitUntilDisabled();

    const newCreditLimit = await initialCreditLimit.getSelectedValue();
    expect(newCreditLimit).toEqual(oldCreditLimit);

    const newContactEmail = await contactEmail.getValue();
    expect(newContactEmail).toEqual(oldContactEmail);

    const newVerifiedInstitution = await verifiedInstitution.getSelectedValue();
    expect(newVerifiedInstitution).toEqual(oldInstitution);
  });

  test('Can go to User Audit page', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // Check AUDIT link is working
    await userProfileAdminPage.getAuditLink().click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
    const newPage2 = await newTarget.page();
    await new UserAuditPage(newPage2).waitForLoad();

    await adminTab.bringToFront();
    await userProfileAdminPage.waitForLoad();

    // Check BACK (admin/user) link is working
    await userProfileAdminPage.getBackLink().click();
    await new UserAdminPage(adminTab).waitForLoad();
  });

  test('Can bypass Google 2-Step Verification module', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    const accessStatusTable = await userProfileAdminPage.getAccessStatusTable();

    // Last completed on is a dash
    const lastCompletedOnCell = await accessStatusTable.getCellByValue(
      AccessModules.GOOGLE_VERIFICATION,
      TableColumns.LAST_COMPLETED_ON
    );
    const completedOn = await lastCompletedOnCell.getCellValue();
    expect(completedOn).toEqual('-');

    // Expires on is NEVER for Google 2-step verification module
    const expiresOnCell = await accessStatusTable.getCellByValue(
      AccessModules.GOOGLE_VERIFICATION,
      TableColumns.EXPIRES_ON
    );
    const expiresOn = await expiresOnCell.getCellValue();
    expect(expiresOn).toEqual('Never');

    // Change Bypass for Google 2-step verification module
    const statusCell = await accessStatusTable.getCellByValue(AccessModules.GOOGLE_VERIFICATION, TableColumns.STATUS);
    const oldStatus = await statusCell.getCellValue();

    const bypassSwitch = await userProfileAdminPage.getBypassSwitchForRow(AccessModules.GOOGLE_VERIFICATION);

    if (oldStatus === 'Bypassed') {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      expect(await bypassSwitch.isOn()).toBe(true);
      await bypassSwitch.turnOff();
    } else {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('No data access');
      expect(await bypassSwitch.isOn()).toBe(false);
      await bypassSwitch.turnOn();
    }

    const bypassSwitchCell = await accessStatusTable.getCellByValue(AccessModules.GOOGLE_VERIFICATION, 'Bypass');
    await verifyToggleSwitchNewBackgroundColor(adminTab, bypassSwitchCell);

    const saveButton = userProfileAdminPage.getSaveButton();
    await verifyButtonEnabled(saveButton, true);
    await saveButton.click();
    await userProfileAdminPage.waitForLoad();

    const newStatus = await statusCell.getCellValue();
    if (oldStatus === 'Bypassed') {
      expect(newStatus).toBe('Incomplete');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('No data access');
    } else {
      expect(newStatus).toBe('Bypassed');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    }

    if (newStatus === 'Incomplete') {
      await bypassSwitch.turnOn();
      await saveButton.click();
      await userProfileAdminPage.waitForLoad();
    }
  });

  test('Can bypass Controlled Tier training module', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    const accessStatusTable = await userProfileAdminPage.getAccessStatusTable();
    const expiresOnCell = await accessStatusTable.getCellByValue(AccessModules.CT_TRAINING, TableColumns.EXPIRES_ON);
    const expiresOn = await expiresOnCell.getCellValue();
    expect(expiresOn).toEqual('-');

    // Change Bypass for Google 2-step verification module
    const statusCell = await accessStatusTable.getCellByValue(AccessModules.CT_TRAINING, TableColumns.STATUS);
    const oldStatus = await statusCell.getCellValue();

    const bypassSwitch = await userProfileAdminPage.getBypassSwitchForRow(AccessModules.CT_TRAINING);
    if (oldStatus === 'Bypassed') {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      await bypassSwitch.turnOff();
    } else {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      await bypassSwitch.turnOn();
    }

    const bypassSwitchCell = await accessStatusTable.getCellByValue(AccessModules.CT_TRAINING, 'Bypass');
    await verifyToggleSwitchNewBackgroundColor(adminTab, bypassSwitchCell);

    const saveButton = userProfileAdminPage.getSaveButton();
    await verifyButtonEnabled(saveButton, true);
    await saveButton.click();
    await userProfileAdminPage.waitForLoad();

    const newStatus = await statusCell.getCellValue();
    if (oldStatus === 'Bypassed') {
      expect(newStatus).toBe('Incomplete');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    } else {
      expect(newStatus).toBe('Bypassed');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    }

    if (newStatus === 'Incomplete') {
      await bypassSwitch.turnOn();
      await saveButton.click();
      await userProfileAdminPage.waitForLoad();
    }
  });

  test('Can modify editable information', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // Modify Institution Role
    const institutionRole = userProfileAdminPage.getInstitutionalRole();
    const oldRole = await institutionRole.getSelectedValue();

    let options = (await institutionRole.getAllOptionTexts()).filter((role) => role !== oldRole);
    const randRole = fp.shuffle(options)[0];
    await institutionRole.select(randRole);

    // Modify Initial Credit Limit
    const initialCreditLimit = userProfileAdminPage.getInitialCreditLimit();
    const oldCreditLimit = await initialCreditLimit.getSelectedValue();

    options = (await initialCreditLimit.getAllOptionTexts()).filter((role) => role !== oldCreditLimit);
    const randCreditLimit = fp.shuffle(options)[0];
    await initialCreditLimit.select(randCreditLimit);

    // Initial Credit Limit remains unchanged before save
    const creditLimitNumber = parseInt(parseForNumbericalString(await initialCreditLimit.getSelectedValue())[0]);
    expect(isNaN(creditLimitNumber)).toBeFalsy();
    expect(creditLimitNumber).not.toEqual(parseInt(parseForNumbericalString(oldCreditLimit)[0]));

    // Save and verify changes
    const saveButton = userProfileAdminPage.getSaveButton();
    await saveButton.click();
    await waitWhileLoading(adminTab);
    await saveButton.waitUntilDisabled();

    const newRole = await institutionRole.getSelectedValue();
    expect(newRole).toEqual(randRole);

    const newCreditLimit = await initialCreditLimit.getSelectedValue();
    expect(newCreditLimit).toEqual(randCreditLimit);
  });
});

async function verifyButtonEnabled(button: Button, enabled: boolean): Promise<void> {
  expect(await button.isCursorNotAllowed()).toBe(!enabled);
}

async function verifyToggleSwitchNewBackgroundColor(page: Page, cell: Cell): Promise<void> {
  const divXpath = `${cell.getXpath()}/div`;

  const background = await getStyleValue<string>(page, await page.waitForXPath(divXpath), 'background-color');

  expect(background).toEqual('rgb(248, 201, 84)');
}

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

async function verifyInstitutionAgreementPage(page: Page): Promise<void> {
  const userProfileAdminPage = new UserProfileAdminPage(page);
  await userProfileAdminPage.getEmailErrorMessageLink().then((link) => {
    link.click();
  });
  const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
  const newPage = await newTarget.page();
  // SUBMIT REQUEST button exists
  await newPage.waitForXPath('//a[text()="SUBMIT REQUEST"]', { visible: true });
  const pageTitle = await newPage.title();
  expect(pageTitle).toEqual('Institutional Agreements – All of Us Research Hub');
  await newPage.close();
}

function replaceWithTmpUrl(url: string): string {
  return url.replace('users', 'users-tmp');
}
