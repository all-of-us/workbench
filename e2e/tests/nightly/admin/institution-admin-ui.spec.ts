import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import InstitutionAdminPage from 'app/page/admin-institution-list-page';
import InstitutionEditPage from 'app/page/admin-institution-edit-page';

describe('Institution Admin', () => {
  const testInstituteName = 'Admin testing';
  //const emailAddress = 'testing@vumc.org';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_ACCESS_TOKEN_FILE);
    await navigation.navMenu(page, NavLink.INSTITUTION_ADMIN);
  });

  test('check the Institution Admin page UI and test-Institute edit page', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();

    //Verify table column names match.
    const columns = ['Institution Type', 'Data access tiers', 'User Email Instruction'];

    const adminTable = new AdminTable(page);
    const columnNames = await adminTable.getColumnNames();
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    //click on the Institution Name link
    await institutionAdminPage.clickInstitutionNameLink(testInstituteName);
    //navigate to institute edit page
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    // verify the fields
    // await institutionEditPage.waitForSaveButton(false);
    const instituteName = await institutionEditPage.getInstituteNameValue();
    expect(instituteName).toBe(testInstituteName);
    expect(await institutionEditPage.getInstitutionTypeValue()).toBe('Other');
    expect(await institutionEditPage.getRTeRAtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(true);
    institutionEditPage.getCancelButton();
    await institutionEditPage.clickCancelButton();
    await institutionAdminPage.waitForLoad();
  });

  test.only('add new institute page UI check', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    await institutionAdminPage.getCreateNewInstituteBtn().click();
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    await institutionEditPage.getAddNewInstituteFields();
    expect(await institutionEditPage.getRTeRAtoggle().isChecked()).toBe(true);
    institutionEditPage.getRTEmailDropdown();
    expect(await institutionEditPage.getCTeRAtoggle().isDisabled()).toBe(true);
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(false);
    await institutionEditPage.clickCTEnabledtoggle();
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(true);
    institutionEditPage.getCTEmailDropdown();
    //verify if the Accepted Email Domains textarea now display
    await institutionEditPage.isCTEmailDomainPresent();
    // select from dropdown option- Individual email address is listed below:
    await institutionEditPage.selectCTEmailOption('Individual email address is listed below:');
    //verify if the Accepted Email Addresses textarea now displays
    await institutionEditPage.isCTEmailAddressPresent();

    // verify that the ADD button is disabled
    expect(await institutionEditPage.getAddButton().isCursorNotAllowed()).toBe(true);
    institutionEditPage.getCancelButton();
    await institutionEditPage.clickCancelButton();
    await institutionAdminPage.waitForLoad();
  });
});
