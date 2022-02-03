import UserAdminPage from 'app/page/admin-user-list-page';
import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import UserProfileInfo from 'app/page/admin-user-profile-info';

describe('User Admin', () => {
  const userEmail = 'admin_test';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.USER_ADMIN);
  });

  test('OLD TEST     Verify admin-user-profile page ui, update initialCredits and update institution', async () => {
    const userAdminPage = new UserAdminPage(page);
    await userAdminPage.waitForLoad();

    //look up the user
    await userAdminPage.searchUser(userEmail);
    await userAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);

    const usernameColIndex = await adminTable.getColumnIndex('User Name');
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

    //get the usernameEmail to match with the username on the admin-user-profile page
    const userNameEmail = await userAdminPage.getUserNameEmail(1, usernameColIndex);

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

    //verify that Name, Username and InitialCreditsUsed input fields are disabled
    expect(await userProfileInfo.getNameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getUsernameInput().isCursorNotAllowed()).toBe(true);
    expect(await userProfileInfo.getInitialCreditsUsedInput().isCursorNotAllowed()).toBe(true);

    // get the username field placeholder to verify the email on user-admin-table page and admin-user-profile page matches
    const userNamePlaceHolder = await userProfileInfo.getUserNamePlaceHolder();
    expect(userNameEmail).toEqual(userNamePlaceHolder);

    //verify the credit limit value matches the limit value in the initial credits used field
    const initialCreditLimit = await userProfileInfo.getInitialCreditsLimitValue();
    const initialCreditMaxValue = await userProfileInfo.getCreditLimitValue();
    expect(initialCreditLimit).toEqual(initialCreditMaxValue);

    // verify the admin is able to update the initial credit
    await userProfileInfo.updateInitialCredits();
    await userProfileInfo.selectInitialCredits(initialCreditLimit);
  });

  test('OOLD TEST      Verify that the user-audit page UI renders correctly', async () => {
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

  test('Can navigate to User Audit page from User Admin Profile page', async () => {

  });

  test ('Verify test user expected state in User Admin Profile page', async () => {
    // verify Researcher information, Editable fields,
    // SAVE and CANCEL buttons disabled
    // Account Enabled toggle status and label
    // Verify information in all columns for Google 2-Step Verification, era common, login.gov in table

  });

  test('Can modify user in User Admin Profile page', async () => {
    // Modify Initial credit limit: check highlights, SAVE button enabled, change back. check highlights gone, SAVE disabled.
    // Change 1 Bypass toggle: check highlight, SAVE. Status column update.
    // Change multiple Bypass toggles: check highlight, SAVE. Status column update.
    // Moke changes in multiple fields (Account access toggle, etc.), click CANCEL, verify values unchaged
    // change Account enabled toggle, check label updated, change back, label updated. change again, save, check label
    // change institution, check "Your email does not match your institution". SAVE button remain disabled
    // check BACK (admin/user) button working
  });
});

// create bug change institution, values not restored
