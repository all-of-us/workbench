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

    await bypassPopup.getAllToggleInputs();

    // get status of each modules individually
    const trainingToggleStatus = await bypassPopup.getEachModuleStatus('Compliance Training');
    const eraCommToggleStatus = await bypassPopup.getEachModuleStatus('eRA Commons Linking');
    const twoFAToggleStatus = await bypassPopup.getEachModuleStatus('Two Factor Auth');
    const dUCCToggleStatus = await bypassPopup.getEachModuleStatus('Data Use Agreement');

    //get the index of the User Lockout column
    const userLockoutColIndex = await adminTable.getColumnIndex('User Lockout');

    // get the text in the User Lockout column
    const userLockout = await userAdminPage.getUserLockoutText(1, userLockoutColIndex);

    //get the index of the Status column
    const statusColIndex = await adminTable.getColumnIndex('Status');

    //get the text in the Status column to match the status on admin-user-profile page
    const status = await userAdminPage.getStatusText(1, statusColIndex);

    //switch statement to validate that the status reflects the user lockout access
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

    //verify that the save button is disabled
    expect(await userProfileInfo.getSaveButton().isCursorNotAllowed()).toBe(true);

    // get the Account Access toggle status (true or false) and the text (enabled or disabled)
    const accountAccessToggle = userProfileInfo.getAccountAccessToggle();
    const accountAccessStatus = await userProfileInfo.getAccountAccessText();

    //switch statement to validate that the Account Access text and toggle reflects the user lockout on user-admin-table.
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

    //verify that userfulName, username and FreeCreditsUsed input fields are disabled
    expect(await userProfileInfo.getNameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getUsernameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getFreeCreditsUsedInput().isCursorNotAllowed()).toBe(true);

    // get the username field placeholder to verify the email on user-admin-table page and admin-user-profile page matches
    const userNamePlaceHolder = await userProfileInfo.getUserNamePlaceHolder();
    expect(userNameEmail).toEqual(userNamePlaceHolder);

    //verify the credit limit value matches the max value in the free credits used field
    const freeCreditLimit = await userProfileInfo.getFreeCreditsLimitValue();
    const freeCreditMaxValue = await userProfileInfo.getFreeCreditMaxValue();
    expect(freeCreditLimit).toEqual(freeCreditMaxValue);

    //get the bypass access status of all modules on admin-user-profile page
    const twoFAToggleStatus2 = await userProfileInfo.getEachBypassStatus('twoFactorAuthBypassToggle');
    const trainingToggle2 = await userProfileInfo.getEachBypassStatus('complianceTrainingBypassToggle');
    const eraCommToggleStatus2 = await userProfileInfo.getEachBypassStatus('eraCommonsBypassToggle');
    const dUCCToggleStatus2 = await userProfileInfo.getEachBypassStatus('dataUseAgreementBypassToggle');

    //verify all the modules status on user-admin-table page and admin-user-profile page match
    expect(twoFAToggleStatus2).toEqual(twoFAToggleStatus);
    expect(trainingToggle2).toEqual(trainingToggleStatus);
    expect(eraCommToggleStatus2).toEqual(eraCommToggleStatus);
    expect(dUCCToggleStatus2).toEqual(dUCCToggleStatus);
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
