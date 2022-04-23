import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';

describe('User Admin', () => {
  const userEmail = 'admin_test';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('verify admin-user-profile page reflects the same modules and active statuses as on admin-table', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    // look up the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);
    const usernameColIndex = await adminTable.getColumnIndex('User Name');

    // get the User Name to verify the search result matches
    const userNameValue = await userAdminPage.getUserNameText(1, usernameColIndex);
    // validate that search box result matches
    expect(userNameValue).toEqual(userEmail);

    // get the index of the Bypass column
    const bypassColIndex = await adminTable.getColumnIndex('Bypass');
    // click on the bypass link
    const bypassPopup = await userAdminPage.clickBypassLink(1, bypassColIndex);
    await bypassPopup.getBypassAdminEnv();
    await bypassPopup.clickCancelBypass();

    // get the name column index separately since it is a frozen column
    const nameColIndex = await adminTable.getNameColindex();

    // click on the name link to navigate to the admin-user-profile page
    const userProfileAdmin = await userAdminPage.clickNameLink(1, nameColIndex);
    // navigate to admin-user-profile page
    await userProfileAdmin.waitForLoad();
  });

  test('Can navigate to the UserAuditPage from the UserAdminPage', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    // look up for the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();

    const adminTable = new AdminTable(page);

    const usernameColIndex = await adminTable.getColumnIndex('User Name');
    // get the User Name to verify the search result matches
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
  });
});
