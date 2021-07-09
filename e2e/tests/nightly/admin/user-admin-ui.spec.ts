
import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';

describe('Admin', () => {
  const userEmail = 'admin_test';

  beforeEach(async () => {
    await signInWithAccessToken(page);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('verify admin-table and admin-user-profile page reflect the same modules and active status', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    //look up the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();

    //Verify table column names match.
    const columns = [
      'Status',
      'Institution',
      'User Name',
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
    expect(columnNames.sort()).toEqual(columns.sort());

    const usernameColIndex = await adminTable.getColumnIndex('User Name');

    //get the User Name to verify the search result matches
    const userNameValue = await userAdminPage.getUserNameText(1, usernameColIndex);

    //get the usernameEmail to match with the username on the admin-user-profile page
    const userNameEmail = await userAdminPage.getUserNameEmail(1, usernameColIndex);

    //validate that search box result matches
    expect(userNameValue).toEqual(userEmail);

    //get the index of the Bypass column
    const bypassColIndex = await adminTable.getColumnIndex('Bypass');

    //click on the bypass link
    const bypassPopup = await userAdminPage.clickBypassLink(1, bypassColIndex);

    // Verify toggle option names match.
    const toggleOptions = ['Compliance Training', 'eRA Commons Linking', 'Two Factor Auth', 'Data Use Agreement'];

    const toggleTexts = await bypassPopup.getAllToggleTexts();
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

    //get the status text to match the status on admin-user-profile page
    const status = await userAdminPage.getStatusText(1, statusColIndex);

    //switch statement to validate that the status reflects the user lockout
    switch (userLockout) {
      case 'DISABLE':
        expect(status).toEqual('Active');
        break;
      case 'ENABLE':
        expect(status).toEqual('Disabled');
        break;
    }

    //get the name column index separately since it is a frozen column
    const nameColIndex = await adminTable.getNameColindex();

    //click on the name link to navigate to the admin-user-profile page
    const userProfileInfo = await userAdminPage.clickNameLink(1, nameColIndex);

    //navigate to admin-user-profile page
    await userProfileInfo.waitForLoad();

    const accountAccessToggle = userProfileInfo.getAccountAccessToggle();

    const accountAccessStatus = await userProfileInfo.getAccountAccessText();

    //switch statement to validate that the Account Access reflects the user lockout
    switch (userLockout) {
      case 'DISABLE':
        expect(accountAccessStatus).toEqual('Enabled');
        expect(await accountAccessToggle.isChecked()).toBe(true);
        break;
      case 'ENABLE':
        expect(accountAccessStatus).toEqual('Disabled');
        expect(await accountAccessToggle.isChecked()).toBe(false);
        break;
    }

    //verify that the save button is disabled
    expect(await userProfileInfo.getSaveButton().isCursorNotAllowed()).toBe(true);

    //verifying that all expected input fields are disabled
    expect(await userProfileInfo.getNameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getUsernameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getFreeCreditsUsedInput().isCursorNotAllowed()).toBe(true);

    const userNamePlaceHolder = await userProfileInfo.getUserNamePlaceHolder();

    //verify the email on user-admin-table page and admin-user-profile page matches
    expect(userNameEmail).toEqual(userNamePlaceHolder);

    const freeCreditLimit = await userProfileInfo.getFreeCreditsLimitValue();
    const freeCreditMaxValue = await userProfileInfo.getFreeCreditMaxValue();

    //verify the credit limit value matches the max value in the free credits used field
    expect(freeCreditLimit).toEqual(freeCreditMaxValue);

    const trainingBypassToggle2 = userProfileInfo.getTrainingBypassToggle();
    expect(await trainingBypassToggle2.isChecked()).toBe(true);

    const eraCommBypassToggle2 = userProfileInfo.getEraCommBypassToggle();
    expect(await eraCommBypassToggle2.isChecked()).toBe(true);

    const twoFABypassToggle2 = userProfileInfo.getTwoFABypassToggle();
    expect(await twoFABypassToggle2.isChecked()).toBe(true);

    const dUCCBypassToggle2 = userProfileInfo.getDUCCBypassToggle();
    expect(await dUCCBypassToggle2.isChecked()).toBe(true);

  });


  test('Verify that the user-audit page UI renders correctly', async () => {
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
});
