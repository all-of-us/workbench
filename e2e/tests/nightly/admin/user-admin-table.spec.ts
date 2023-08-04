import UserAdminTablePage from 'app/page/admin/user-admin-table-page';
import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';

describe.skip('User Admin Table', () => {
  const userEmail = 'admin_test';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('verify user-admin-table can access the bypass toggles and navigate to user-admin-profile', async () => {
    const userAdminPage = new UserAdminTablePage(page);
    await userAdminPage.waitForLoad();

    //look up the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);
    const usernameColIndex = await adminTable.getColumnIndex('Username');

    //get the Username to verify the search result matches
    const usernameValue = await userAdminPage.getUsernameText(1, usernameColIndex);
    //validate that search box result matches
    expect(usernameValue).toEqual(userEmail);

    //get the index of the Bypass column
    const bypassColIndex = await adminTable.getColumnIndex('Access Module Bypass');
    //click on the bypass link
    const bypassPopup = await userAdminPage.clickBypassLink(1, bypassColIndex);
    await bypassPopup.getBypassAdminEnv();
    await bypassPopup.clickCancelBypass();

    //get the name column index separately since it is a frozen column
    const nameColIndex = await adminTable.getNameColIndex();

    //click on the name link to navigate to the admin-user-profile page
    const userProfileAdmin = await userAdminPage.clickNameLink(1, nameColIndex);
    //navigate to admin-user-profile page
    await userProfileAdmin.waitForLoad();
  });
});
