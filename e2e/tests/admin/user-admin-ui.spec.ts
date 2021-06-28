import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';


describe('Admin', () => {
  const userEmail = 'admin_test@fake-research-aou.org';
  // const userName = "admin_test";

  beforeEach(async () => {
    await signInWithAccessToken(page);
    //await navigation.navMenu(page, NavLink.ADMIN);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('verify user(admin_test) displays  bypassed modules and active status', async () => {
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
    
    expect(columnNames.sort()).toEqual(columns.sort());

    const usernameColIndex = await adminTable.getColumnIndex('User name');

    //verify that the status col has status as active and user lockout displays ENABLE
    const userNameValue = await userAdminPage.getUserNameText(1, usernameColIndex);

    // validate that search box result 
    expect(userNameValue).toEqual(userEmail);

    // get the index of the Bypass column
    const bypassColIndex = await adminTable.getColumnIndex('Bypass');

    //click on the bypass link
    const bypassPopup = await userAdminPage.clickBypassLink(1, bypassColIndex);

    // Verify toggle option names match.
    const toggleOptions = [
      'Compliance Training',
      'eRA Commons Linking',
      'Two Factor Auth',
      'Data Use Agreement',
    ];
    
    const toggleTexts = await bypassPopup.getAllToggleTexts();
    console.log(toggleTexts);

    expect(toggleTexts.sort()).toEqual(toggleOptions.sort());

    const trainingBypassToggle = bypassPopup.getTrainingBypassToggle();
    expect(await trainingBypassToggle.isChecked()).toBe(true);

    const eraCommBypassToggle = bypassPopup.getEraCommBypassToggle();
    expect(await eraCommBypassToggle.isChecked()).toBe(true);

    const twoFABypassToggle = bypassPopup.getTwoFABypassToggle();
    expect(await twoFABypassToggle.isChecked()).toBe(true);

    const dUCCBypassToggle = bypassPopup.getDUCCBypassToggle();
    expect(await dUCCBypassToggle.isChecked()).toBe(true);
    

    //get the index of the user lockout column
    const userLockoutColIndex = await adminTable.getColumnIndex('User Lockout');
    await userAdminPage.getUserLockoutText(1, userLockoutColIndex);
    const userLockout = await userAdminPage.getUserLockoutText(1, userLockoutColIndex);

    const statusColIndex = await adminTable.getColumnIndex('Status');

    //verify that the status col has status as active and user lockout displays ENABLE
    const status = await userAdminPage.getStatusText(1, statusColIndex);

    // switch statement to validate that the status reflects the user lockout 
    switch(userLockout) {
      case 'DISABLE':  
        expect(status).toEqual('Active');
        break;
      case 'ENABLE':  
      expect(status).toEqual('Disabled');
        break;
    }

    //the name column index found separately since it is a frozen column
    const nameColIndex = await adminTable.getNameColindex(); 

    //click on the name link to navigate to the user profile page
    const userProfileInfo = await userAdminPage.clickNameLink(1, nameColIndex);
    await userProfileInfo.waitForLoad();
    await page.bringToFront();

    // click the audit link of the user
    const auditColIndex = await adminTable.getColumnIndex('Audit');
    console.log(`auditColIndex: ${auditColIndex}`);

    const userAuditPage = await userAdminPage.clickAuditLink(1, auditColIndex);
     await userAuditPage.waitForLoad();
   
  
  });

});
