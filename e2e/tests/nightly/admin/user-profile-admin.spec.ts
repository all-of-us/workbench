import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';
import UserProfileAdminPage from 'app/page/user-profile-admin-page';

describe('User Admin', () => {
  const userEmail = 'admin_test';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('OLD TEST     Verify admin-user-profile page ui, update initialCredits and update institution', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    // Search for user
    await userAdminPage.searchUser(userEmail);

    const adminTable = new AdminTable(page);
    const nameColumnIndex = await adminTable.getNameColindex();
    // Click name to navigate to User Profile Admin page
    await userAdminPage.clickUserName(1, nameColumnIndex);

    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
    const newPage = await newTarget.page();

    await new UserProfileInfo(newPage).waitForLoad();

    // Redirects to User Profile Admin page. Go to url users-tmp page
    const tmpUrl = replaceWithTmpUrl(newPage.url());
    await newPage.goto(tmpUrl, { waitUntil: ['load', 'networkidle0'] });
    await userAdminPage.waitForLoad();

    const userProfileAdminPage = new UserProfileAdminPage(newPage);
    await userProfileAdminPage.waitForLoad();

    // Verify SAVE button and CANCEL are disabled
    expect(await userProfileAdminPage.getSaveButton().isCursorNotAllowed()).toBe(true);
    expect(await userProfileAdminPage.getCancelButton().isCursorNotAllowed()).toBe(true);

    // Check Account access toggle text label before and after toggle
    const accountAccessSwitch = userProfileAdminPage.getAccountAccessSwitch();
    let label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account enabled');

    await accountAccessSwitch.turnOff();
    label = await accountAccessSwitch.getLabel();
    expect(label).toEqual('Account disabled');

    // Verify Institution
    const verifiedInstitution = userProfileAdminPage.getVerifiedInstitution();
    const institutionName = await verifiedInstitution.getSelectedValue();
    expect(institutionName).toEqual('Admin testing');

    // Verify Contact Email
    const contactEmail = userProfileAdminPage.getContactEmail();
    const email = await contactEmail.getValue();
    expect(email).toEqual('testing@vumc.org');

    // Verify User Name
    const userName = await userProfileAdminPage.getUserName();
    expect(userName).toEqual(`${userEmail}${config.EMAIL_DOMAIN_NAME}`);

    // Verify Data Access Tiers is Registered Tier only
    const accessTiers = await userProfileAdminPage.getDataAccessTiers();
    expect(accessTiers).toEqual('Registered Tier');
    expect(accessTiers).not.toContain('Controlled Tier');
  });

  xtest('OOLD TEST      Verify that the user-audit page UI renders correctly', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    //look up for the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();

    const adminTable = new AdminTable(page);

    const usernameColIndex = await adminTable.getColumnIndex('User Name');
    //get the User Name to verify the search result matches
    const userNameValue = await userAdminPage.getUserNameText(1, usernameColIndex);

    // click the audit link of the user
    const auditColIndex = await adminTable.getColumnIndex('Audit');

    const userAuditPage = await userAdminPage.clickAuditLink(1, auditColIndex);
    await userAuditPage.waitForLoad();

    userAuditPage.getAuditButton();
    userAuditPage.getDownloadSqlButton();

    const userNameAuditPage = await userAuditPage.getUsernameValue();
    expect(userNameAuditPage).toEqual(userNameValue);

    await userAuditPage.clickUserAdminLink();
    const userProfileInfo = new UserProfileInfo(page);
    await userProfileInfo.waitForLoad();
  });

  xtest('Can navigate to User Audit page from User Admin Profile page', async () => {
    // blank
  });

  xtest('Verify test user expected state in User Admin Profile page', async () => {
    // verify Researcher information, Editable fields,
    // SAVE and CANCEL buttons disabled
    // Account Enabled toggle status and label
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

function replaceWithTmpUrl(url: string): string {
  return url.replace('users', 'users-tmp');
}

// create bug change institution, values not restored
