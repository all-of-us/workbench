import { ElementHandle, Page } from 'puppeteer';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileAdminPage from 'app/page/admin/user-profile-admin-page';
import UserAdminTablePage from 'app/page/admin/user-admin-table-page';
import { AccessTierDisplayNames, Institution, InstitutionRole } from 'app/text-labels';
import Cell, { CellContent } from 'app/component/cell';
import UserAuditPage from 'app/page/admin/admin-user-audit-page';
import { getPropValue, getStyleValue } from 'utils/element-utils';
import { isBlank } from 'utils/str-utils';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { asyncFilter, parseForNumericalStrings, signInWithAccessToken } from 'utils/test-utils';
import fp from 'lodash/fp';

/**
 * Expected state of test user:
 *
 * username = admin_test
 * contact email = testing@vumc.org
 * name = Admin Testing
 * credits used = 0
 * institution = Admin testing
 * PROFILE and PUBLICATION access modules are Current
 * PROFILE and PUBLICATION access modules expire on May 12, 2031
 * all other modules are bypassed
 *
 * Admin testing institution requires ERA for both tiers
 *
 * These institutions also exist: Broad, Google, Verily, Vanderbilt
 *
 */

describe.skip('User Profile Admin page', () => {
  enum TableColumns {
    ACCESS_MODULE = 'Access module',
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

  const CHANGED_BACKGROUND_COLOR = 'rgb(248, 201, 84)';
  const BACKGROUND_COLOR = 'rgb(255, 255, 255)';
  const PROP_BACKGROUND_COLOR = 'background-color: #F8C954;';
  const testUserEmail = 'admin_test';
  let adminTab: Page;

  beforeEach(async () => {
    adminTab = await searchTestUser(page, testUserEmail);
  });

  test('Verify Researcher Information and Edit Information render correctly', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // SAVE and CANCEL buttons should be disabled
    const saveButton = userProfileAdminPage.getSaveButton();
    const cancelButton = userProfileAdminPage.getCancelButton();
    await cancelButton.expectEnabled(false);
    await saveButton.expectEnabled(false);

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
    const credits = parseInt(parseForNumericalStrings(await creditLimit.getSelectedValue())[0]);

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
        InstitutionRole.UndergraduateStudent,
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

    // Verify Access module names displayed correctly
    const moduleNames = await accountAccessTable.getRowValues(1);
    expect(moduleNames.sort()).toEqual(accessModules.sort());

    // Verify Tier Badges displayed correctly
    // note: assumes ERA required for both tiers
    for (const accessModule of accessModules) {
      const cell = await accountAccessTable.findCellByRowValue(
        TableColumns.ACCESS_MODULE,
        accessModule,
        TableColumns.REQUIRED_FOR_TIER
      );
      expect(await hasTierBadge(cell, AccessTierDisplayNames.Controlled)).toBe(true);
      accessModule === AccessModules.CT_TRAINING
        ? expect(await hasTierBadge(cell, AccessTierDisplayNames.Registered)).toBe(false)
        : expect(await hasTierBadge(cell, AccessTierDisplayNames.Registered)).toBe(true);
    }

    // Verify Status displayed correctly
    for (const accessModule of accessModules) {
      const cell = await accountAccessTable.findCellByRowValue(
        TableColumns.ACCESS_MODULE,
        accessModule,
        TableColumns.STATUS
      );
      accessModule === AccessModules.UPDATE_PROFILE || accessModule === AccessModules.PUBLICATION
        ? expect(await cell.getText()).toBe('Current')
        : expect(await cell.getText()).toBe('Bypassed');
    }

    // Verify values in "Expires on" column
    for (const accessModule of accessModules) {
      const cell = await accountAccessTable.findCellByRowValue(
        TableColumns.ACCESS_MODULE,
        accessModule,
        TableColumns.EXPIRES_ON
      );
      switch (accessModule) {
        case AccessModules.GOOGLE_VERIFICATION:
        case AccessModules.ERA:
        case AccessModules.LOGIN_GOV:
          // Match Never
          expect(await cell.getText()).toEqual('Never');
          break;
        case AccessModules.UPDATE_PROFILE:
        case AccessModules.PUBLICATION:
          // Match date
          expect(await cell.getText()).toEqual('May 12, 2031');
          break;
        default:
          expect(await cell.getText()).toEqual('-');
          break;
      }
    }
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
    await cancelButton.expectEnabled(false);
    await saveButton.expectEnabled(false);

    // Turn off Account Access toggle
    await accountAccessSwitch.toggleOff();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account disabled');

    await cancelButton.expectEnabled(true);
    await saveButton.expectEnabled(true);

    // Undo toggle change
    await accountAccessSwitch.toggleOn();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    await cancelButton.expectEnabled(false);
    await saveButton.expectEnabled(false);

    // Change Initial Credits Limit to $400
    const initialCreditLimit = userProfileAdminPage.getInitialCreditLimit();
    const oldCreditLimit = await initialCreditLimit.getSelectedValue();

    const creditLimitsOptions = (await initialCreditLimit.getAllOptionTexts()).filter(
      (role) => role !== oldCreditLimit
    );
    await initialCreditLimit.select(fp.shuffle(creditLimitsOptions)[0]);

    // Verify background color in Select has changed
    const [styleElement] = await (await initialCreditLimit.asElement()).$x('./preceding-sibling::style');
    const styleText = await getPropValue<string>(styleElement, 'innerText');
    expect(styleText).toContain(PROP_BACKGROUND_COLOR);

    await cancelButton.expectEnabled(true);
    await saveButton.expectEnabled(true);

    // Change Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const oldContactEmail = await contactEmail.getValue();
    // Input background color is white before change
    let backgroundColor = await getStyleValue<string>(
      adminTab,
      await contactEmail.asElementHandle(),
      'background-color'
    );
    expect(backgroundColor).toEqual(BACKGROUND_COLOR);

    // Type in modified email
    const invalidContactEmail = 'mod-' + oldContactEmail;
    await contactEmail.type(invalidContactEmail);

    // Input background color changed
    backgroundColor = await getStyleValue<string>(adminTab, await contactEmail.asElementHandle(), 'background-color');
    expect(backgroundColor).toEqual(CHANGED_BACKGROUND_COLOR);

    // Find email error because modified email address is not a user in institution "Admin testing".
    // Institution matches exact email addresses.
    const emailErrorMsg = await userProfileAdminPage.getEmailErrorMessage();
    expect(emailErrorMsg).toContain('The institution has authorized access only to select members');

    await verifyInstitutionAgreementPage(adminTab);
    await adminTab.bringToFront();

    await cancelButton.expectEnabled(true);
    // SAVE buttons is disabled due to error in page
    await saveButton.expectEnabled(false);

    // Undo all changes
    await cancelButton.click();
    await cancelButton.waitUntilDisabled();

    // Change Verified Institution that cause domain mismatch
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const oldInstitution = await verifiedInstitution.getSelectedValue();
    await verifiedInstitution.select('Broad Institute');

    // Check error when Verified Institution and Contact Email domains do not match
    await waitForText(adminTab, 'Your email does not match your institution');

    await cancelButton.expectEnabled(true);
    // SAVE buttons is disabled due to an error in page
    await saveButton.expectEnabled(false);

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

  test('Can navigate to the UserAuditPage from the UserProfileAdminPage', async () => {
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    // Check AUDIT link is working
    await userProfileAdminPage.getAuditLink().click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
    const userAuditPage = await newTarget.page();
    await new UserAuditPage(userAuditPage).waitForLoad();

    await adminTab.bringToFront();
    await userProfileAdminPage.waitForLoad();

    // Check BACK (admin/user) link is working
    await userProfileAdminPage.getBackLink().click();
    await new UserAdminTablePage(adminTab).waitForLoad();
  });

  test('Can bypass Google 2-Step Verification module', async () => {
    // Chose Google 2-step Verification module arbitrarily
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    const accessStatusTable = userProfileAdminPage.getAccessStatusTable();

    // Last completed on is a dash
    const lastCompletedOnCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.GOOGLE_VERIFICATION,
      TableColumns.LAST_COMPLETED_ON
    );
    const completedOn = await lastCompletedOnCell.getText();
    expect(completedOn).toEqual('-');

    // Change Bypass for Google 2-step verification module
    const statusCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.GOOGLE_VERIFICATION,
      TableColumns.STATUS
    );
    const oldStatus = await statusCell.getText();

    const bypassSwitch = await userProfileAdminPage.getBypassSwitchForRow(AccessModules.GOOGLE_VERIFICATION);

    if (oldStatus === 'Bypassed') {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      expect(await bypassSwitch.isOn()).toBe(true);
      await bypassSwitch.toggleOff();
    } else {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('No data access');
      expect(await bypassSwitch.isOn()).toBe(false);
      await bypassSwitch.toggleOn();
    }

    const bypassSwitchCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.GOOGLE_VERIFICATION,
      'Bypass'
    );
    await verifyToggleSwitchNewBackgroundColor(adminTab, bypassSwitchCell);

    const saveButton = userProfileAdminPage.getSaveButton();
    await saveButton.expectEnabled(true);
    await saveButton.click();
    await userProfileAdminPage.waitForLoad();

    const newStatus = await statusCell.getText();
    if (oldStatus === 'Bypassed') {
      expect(newStatus).toBe('Incomplete');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('No data access');
    } else {
      expect(newStatus).toBe('Bypassed');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    }

    // cleanup: end this test with the user in the bypassed state
    if (newStatus === 'Incomplete') {
      await bypassSwitch.toggleOn();
      await saveButton.click();
      await userProfileAdminPage.waitForLoad();
    }
  });

  test('Can bypass Controlled Tier training module', async () => {
    // Chose Controlled Tier training arbitrarily
    const userProfileAdminPage = new UserProfileAdminPage(adminTab);
    await userProfileAdminPage.waitForLoad();

    const accessStatusTable = userProfileAdminPage.getAccessStatusTable();
    const expiresOnCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.CT_TRAINING,
      TableColumns.EXPIRES_ON
    );
    const expiresOn = await expiresOnCell.getText();
    expect(expiresOn).toEqual('-');

    // Change Bypass for Controlled Tier training module
    const statusCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.CT_TRAINING,
      TableColumns.STATUS
    );
    const oldStatus = await statusCell.getText();

    const bypassSwitch = await userProfileAdminPage.getBypassSwitchForRow(AccessModules.CT_TRAINING);
    if (oldStatus === 'Bypassed') {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      await bypassSwitch.toggleOff();
    } else {
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
      await bypassSwitch.toggleOn();
    }

    const bypassSwitchCell = await accessStatusTable.findCellByRowValue(
      TableColumns.ACCESS_MODULE,
      AccessModules.CT_TRAINING,
      'Bypass'
    );
    await verifyToggleSwitchNewBackgroundColor(adminTab, bypassSwitchCell);

    const saveButton = userProfileAdminPage.getSaveButton();
    await saveButton.expectEnabled(true);
    await saveButton.click();
    await userProfileAdminPage.waitForLoad();

    const newStatus = await statusCell.getText();
    if (oldStatus === 'Bypassed') {
      expect(newStatus).toBe('Incomplete');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    } else {
      expect(newStatus).toBe('Bypassed');
      expect(await userProfileAdminPage.getDataAccessTiers()).toEqual('Registered Tier');
    }

    // cleanup: end this test with the user in the bypassed state
    if (newStatus === 'Incomplete') {
      await bypassSwitch.toggleOn();
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

    // Select a different institution role:
    // Role is not the one already selected and not Other (which requires entering other text)
    let options = (await institutionRole.getAllOptionTexts()).filter(
      (role) => role !== oldRole && role !== 'Other (free text)'
    );
    const randRole = fp.shuffle(options)[0];
    await institutionRole.select(randRole);

    // Modify Initial Credit Limit
    const initialCreditLimit = userProfileAdminPage.getInitialCreditLimit();
    const oldCreditLimit = await initialCreditLimit.getSelectedValue();

    options = (await initialCreditLimit.getAllOptionTexts()).filter((role) => role !== oldCreditLimit);
    const randCreditLimit = fp.shuffle(options)[0];
    await initialCreditLimit.select(randCreditLimit);

    // Initial Credit Limit remains unchanged before save
    const creditLimitNumber = parseInt(parseForNumericalStrings(await initialCreditLimit.getSelectedValue())[0]);
    expect(isNaN(creditLimitNumber)).toBeFalsy();
    expect(creditLimitNumber).not.toEqual(parseInt(parseForNumericalStrings(oldCreditLimit)[0]));

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

  async function verifyToggleSwitchNewBackgroundColor(page: Page, cell: Cell): Promise<void> {
    const divXpath = `${cell.getXpath()}/div`;
    const background = await getStyleValue<string>(page, await page.waitForXPath(divXpath), 'background-color');
    expect(background).toEqual(CHANGED_BACKGROUND_COLOR);
  }

  async function searchTestUser(page: Page, userName: string): Promise<Page> {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.USER_ADMIN);

    const userAdminPage = new UserAdminTablePage(page);
    await userAdminPage.waitForLoad();

    // Search for user
    await userAdminPage.searchUser(userName);

    const adminTable = new AdminTable(page);
    const nameColumnIndex = await adminTable.getNameColIndex();
    // Click name to navigate to User Profile Admin page
    await userAdminPage.clickUserName(1, nameColumnIndex);

    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
    const newPage = await newTarget.page();

    await new UserProfileAdminPage(newPage).waitForLoad();

    return newPage;
  }

  async function verifyInstitutionAgreementPage(page: Page): Promise<void> {
    const userProfileAdminPage = new UserProfileAdminPage(page);
    await userProfileAdminPage.getEmailErrorMessageLink().then((link) => link.click());
    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
    const newPage = await newTarget.page();
    // SUBMIT A REQUEST button exists
    await newPage.waitForXPath('//*[./*[text()="Need an Agreement?"]]/a[text()="SUBMIT A REQUEST"]', { visible: true });
    const pageTitle = await newPage.title();
    expect(pageTitle).toEqual('Institutional Agreements – All of Us Research Hub');
    await newPage.close();
  }

  async function hasTierBadge(cell: Cell, tier: AccessTierDisplayNames): Promise<boolean> {
    const elements: ElementHandle[] = await cell.getContent(CellContent.SVG);
    const svg: ElementHandle[] = await asyncFilter(
      elements,
      async (svg: ElementHandle) => tier === (await getPropValue<string>(svg, 'textContent'))
    );
    return svg.length > 0;
  }
});
