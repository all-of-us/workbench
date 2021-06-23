import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';
//import UserAuditPage from 'app/page/admin-user-audit-page';


describe('Admin', () => {
  const userEmail = 'admin_test@fake-research-aou.org';
  // const userName = "admin_test";

  beforeEach(async () => {
    await signInWithAccessToken(page);
    //await navigation.navMenu(page, NavLink.ADMIN);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('verify user bypassed and status is active', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    //look up for the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();

    //Verify table column names match.
    const columns = [
      'Status',
      'Institution',
      'Registration date',
      'User name',
      'Contact Email',
      'User Lockout',
      'First Sign-in',
      '2FA',
      'Training',
      'eRA Commons',
      'DUCC',
      'Bypass',
      'Audit'
    ];

    const adminTable = new AdminTable(page);
    const columnNames = await adminTable.getColumnNames();
    console.log(columnNames);
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    // get the index of the Bypass column
    const bypassColIndex = await adminTable.getColumnIndex('Bypass');

    //click on the bypass link
    const bypassLinkModal = await userAdminPage.clickBypassLink(1, bypassColIndex);
    const toggleText = await bypassLinkModal.getAllToggleTexts();
    console.log(toggleText);

    // verify all the 4 modules toggle are checked
    await bypassLinkModal.verifyAllModulesBypassed();

    const user1UserLockout = 'Disable';
    //get the index of the user lockout column
    const userLockoutColIndex = await adminTable.getColumnIndex('User Lockout');
    const userLockoutforUser1 = await userAdminPage.getUserLockoutText(1, userLockoutColIndex);

    // expect userlockout text to be DISABLE
    expect(userLockoutforUser1).toEqual(user1UserLockout);

    const user1Status = 'Active';
    const statusColIndex = await adminTable.getColumnIndex('Status');
    //verify that the status col has status as active and user lockout displays ENABLE
    const userStatusForUser1 = await userAdminPage.getStatusText(1, statusColIndex);
    //expect status to be active
    expect(userStatusForUser1).toEqual(user1Status);
    const nameColIndex = await adminTable.getNameColindex();

    //click on the name link to navigate to the user profile page
    console.log(nameColIndex);
    await userAdminPage.clickNameLink(1, nameColIndex);

    //opens User Admin Profile Info page in a new tab
    const userProfileInfo = new UserProfileInfo(page);
    await userProfileInfo.waitForLoad();

    // // expect(title).toBe('User Admin | All of Us Researcher Workbench');

    //makes the previous tab active tab
    await page.bringToFront();
    const auditColIndex = await adminTable.getColumnIndex('Audit');
    console.log(`auditColIndex: ${auditColIndex}`);

    await userAdminPage.clickAuditLink(1, auditColIndex);


    await page.bringToFront();

  });

});
