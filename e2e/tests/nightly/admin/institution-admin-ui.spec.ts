import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import InstitutionAdminPage from 'app/page/admin-institution-list-page';
import InstitutionEditPage from 'app/page/admin-institution-edit-page';

describe('Institution Admin', () => {
  const testInstituteName = 'Admin testing';
  const emailAddress = 'testing@vumc.org';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_ACCESS_TOKEN_FILE);
    await navigation.navMenu(page, NavLink.INSTITUTION_ADMIN);
  });

  test('check Institution Admin UI and institute edit page', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();

    //Verify table column names match.
    const columns = [
      'Institution Type',
      'Data access tiers',
      'User Email Instruction'
    ];

    const adminTable = new AdminTable(page);
    const columnNames = await adminTable.getColumnNames();
    console.log(columnNames);
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    //click on the Institution Name link
    await institutionAdminPage.clickInstitutionNameLink(testInstituteName);
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    const instituteName = await institutionEditPage.getInstituteNameValue();
    expect(instituteName).toBe(testInstituteName);
    expect(await institutionEditPage.getInstitutionTypeValue()).toBe('Other');
    const acceptedCTAEmail = institutionEditPage.getCTAcceptedEmailInput();
    // remove Email from  CTA Accepted Email Addresses textarea
    await acceptedCTAEmail.clear();
    // save button is disabled and error message is displayed
    await institutionEditPage.waitForSaveButton(false);
    // make a change, causing the Save button to activate
    await acceptedCTAEmail.type(emailAddress);
    // save button is enabled and no error message is displayed
    await institutionEditPage.waitForSaveButton(true);
    institutionEditPage.getCancelButton();
  });

  test.only('add new institute page UI check', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    await institutionAdminPage.getCreateNewInstituteBtn().click();
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    await institutionEditPage.getAddNewInstituteFields();
    expect(await institutionEditPage.getRTeRAtoggle().isChecked()).toBe(true);
    institutionEditPage.getRTDomainsDropdown();
    expect(await institutionEditPage.getCTeRAtoggle().isDisabled()).toBe(true);
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(false);
    await institutionEditPage.clickCTEnabledtoggle();
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(true);
    //verify if the Select type dropdown and Accepted Email Domains textarea now display
    await institutionEditPage.isDivWithLabelPresent("A user is considered part of this institution and eligible");
    await institutionEditPage.isDivWithLabelPresent("Accepted Email Domains");
    
    // const ctAccessEmailOptions = institutionEditPage.getCTDomainsDropdown();
    // const selectTypeOptions = await ctAccessEmailOptions.getAllOptionTexts();
    // expect(selectTypeOptions).toEqual(expect.arrayContaining(['Email address is at one of the following domains:', 'Individual email address is listed below:']));

    // select from dropdown option- Individual email address is listed below:
    await institutionEditPage.selectCTEmailOption('Individual email address is listed below:');
    //verify if the Accepted Email Addresses textarea now displays
    await institutionEditPage.isDivWithLabelPresent("Accepted Email Addresses");
    // verify that the ADD button is disabled
    expect(await institutionEditPage.getAddButton().isCursorNotAllowed()).toBe(true);
  });

  test.skip('verify user is not-bypassed and status is disable', async () => {
    //verify that the User Lockout column is displaying DISABLE (default state)
    //verify that the status column is displaying Active (default status)
    //verify that the admin is able to update the free credits
  });
});
