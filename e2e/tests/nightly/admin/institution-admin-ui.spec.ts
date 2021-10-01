import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { HeaderName } from 'app/text-labels';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import InstitutionAdminPage from 'app/page/admin-institution-list-page';
import InstitutionEditPage, {
  AcceptedAddressSelectValue,
  InstitutionTypeSelectValue
} from 'app/page/admin-institution-edit-page';

describe('Institution Admin', () => {
  const testInstitutionName = 'Admin testing';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_ACCESS_TOKEN_FILE);
    await navigation.navMenu(page, NavLink.INSTITUTION_ADMIN);
  });

  test('check the Institution Admin page UI and test-Institution edit page', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);
    const columns = await adminTable.getAllColumnNames();
    expect(columns).toEqual(
      expect.arrayContaining([
        HeaderName.InstitutionName,
        HeaderName.InstitutionType,
        HeaderName.DataAccessTiers,
        HeaderName.UserEmailInstruction
      ])
    );

    //click on the Institution Name link
    await institutionAdminPage.clickInstitutionNameLink(testInstitutionName);
    // navigate to Institution edit page
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();

    // verify the input fields
    const institutionName = await institutionEditPage.getInstitutionNameValue();
    expect(institutionName).toBe(testInstitutionName);
    expect(await institutionEditPage.getInstitutionTypeValue()).toBe(InstitutionTypeSelectValue.Other);
    expect(await institutionEditPage.getRTeRAtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getRTEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Individual);

    //verify that the Accepted Email Addresses text area is present in registered-card-details
    institutionEditPage.getRTEmailAddressInput();
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getCTEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Domains);
    // verify that the Accepted Email Domains text area is present in controlled-card-details
    institutionEditPage.getCTEmailDomainsInput();
    //verify that the save button is disabled
    expect(await institutionEditPage.getSaveButton().isCursorNotAllowed()).toBe(true);
    await institutionEditPage.clickCancelButton();
    await institutionAdminPage.waitForLoad();
  });

  test('add new Institution page UI check', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    await institutionAdminPage.getCreateNewInstitutionBtn().click();
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    await institutionEditPage.getAddNewInstitutionFields();
    expect(await institutionEditPage.getRTeRAtoggle().isChecked()).toBe(true);
    await institutionEditPage.selectRTEmailOption(AcceptedAddressSelectValue.Domains);
    institutionEditPage.getRTEmailDomainsInput();
    expect(await institutionEditPage.getCTeRAtoggle().isDisabled()).toBe(true);
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(false);
    await institutionEditPage.clickCTEnabledtoggle();
    expect(await institutionEditPage.getCTEnabledtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getCTEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Domains);

    //verify if the Accepted Email Domains textarea now displays in controlled-card-details div
    institutionEditPage.getCTEmailDomainsInput();
    // select from dropdown option- Individual email address is listed below:
    await institutionEditPage.selectCTEmailOption(AcceptedAddressSelectValue.Individual);
    //verify if the Accepted Email Addresses textarea now displays in controlled-card-details div
    institutionEditPage.getCTEmailAddressInput();

    // verify that the ADD button is disabled
    expect(await institutionEditPage.getAddButton().isCursorNotAllowed()).toBe(true);
    await institutionEditPage.clickCancelButton();
    await institutionAdminPage.waitForLoad();
  });
});
