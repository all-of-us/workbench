import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { HeaderName } from 'app/text-labels';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';
import InstitutionAdminPage from 'app/page/admin/admin-institution-list-page';
import InstitutionEditPage, {
  AcceptedAddressSelectValue,
  InstitutionTypeSelectValue
} from 'app/page/admin/admin-institution-edit-page';
import waitForExpect from 'wait-for-expect';

describe.skip('Institution Admin', () => {
  const testInstitutionName = 'Admin testing';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.INSTITUTION_ADMIN);
  });

  test('check the Institution Admin page UI and test-Institution edit page', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);
    await adminTable.waitUntilVisible();
    const columns = await adminTable.getColumnNames();
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
    await waitForExpect(async () => {
      expect(await institutionEditPage.getInstitutionNameValue()).toBe(testInstitutionName);
    }, 10000);

    expect(await institutionEditPage.getInstitutionTypeValue()).toBe(InstitutionTypeSelectValue.Other);
    expect(await institutionEditPage.getRtEratoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getRtEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Individual);

    //verify that the Accepted Email Addresses text area is present in registered-card-details
    const rtEmailAddressTextArea = institutionEditPage.getRtEmailAddressInput();
    expect(await rtEmailAddressTextArea.asElementHandle()).toBeTruthy();
    expect(await institutionEditPage.getCtEnabledtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getCtEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Domains);
    // verify that the Accepted Email Domains text area is present in controlled-card-details
    const ctEmailDomainsTextArea = institutionEditPage.getCtEmailDomainsInput();
    expect(await ctEmailDomainsTextArea.asElementHandle()).toBeTruthy();
    //verify that the save button is disabled
    expect(await institutionEditPage.getSaveButton().isCursorNotAllowed()).toBe(true);
    await institutionEditPage.clickCancelButton();
    await institutionAdminPage.waitForLoad();
  });

  test('add new Institution page UI check', async () => {
    const institutionAdminPage = new InstitutionAdminPage(page);
    await institutionAdminPage.waitForLoad();
    const adminTable = new AdminTable(page);
    await adminTable.waitUntilVisible();
    await institutionAdminPage.getCreateNewInstitutionBtn().click();
    const institutionEditPage = new InstitutionEditPage(page);
    await institutionEditPage.waitForLoad();
    await institutionEditPage.getAddNewInstitutionFields();
    expect(await institutionEditPage.getRtEratoggle().isChecked()).toBe(false);
    await institutionEditPage.selectRtEmailOption(AcceptedAddressSelectValue.Domains);
    //verify if the Accepted Email Domains textarea now displays in registered-card-details div
    const rtEmailDomainsTextArea = institutionEditPage.getRtEmailDomainsInput();
    expect(await rtEmailDomainsTextArea.asElementHandle()).toBeTruthy();
    expect(await institutionEditPage.getCtEratoggle().isDisabled()).toBe(true);
    expect(await institutionEditPage.getCtEnabledtoggle().isChecked()).toBe(false);
    await institutionEditPage.clickCtEnabledtoggle();
    expect(await institutionEditPage.getCtEnabledtoggle().isChecked()).toBe(true);
    expect(await institutionEditPage.getCtEmailAcceptedValue()).toBe(AcceptedAddressSelectValue.Domains);

    //verify if the Accepted Email Domains textarea now displays in controlled-card-details div
    const ctEmailDomainsTextArea = institutionEditPage.getCtEmailDomainsInput();
    expect(await ctEmailDomainsTextArea.asElementHandle()).toBeTruthy();
    // select from dropdown option- Individual email address is listed below:
    await institutionEditPage.selectCtEmailOption(AcceptedAddressSelectValue.Individual);
    //verify if the Accepted Email Addresses textarea now displays in controlled-card-details div
    const ctEmailAddressTextArea = institutionEditPage.getCtEmailAddressInput();
    expect(await ctEmailAddressTextArea.asElementHandle()).toBeTruthy();
    // verify that the ADD button is disabled
    expect(await institutionEditPage.getAddButton().isCursorNotAllowed()).toBe(true);
    // click the back icon on the top right
    const institutionNotSavedModal = await institutionEditPage.clickBackButton();
    // click Keep Editing button to stay on the add new institution page
    await institutionNotSavedModal.clickKeepEditingButton();
    await institutionEditPage.waitForLoad();
    await institutionEditPage.clickBackButton();
    // click yes, leave button to navigate to the institution admin page
    await institutionNotSavedModal.clickYesLeaveButton();
    await institutionAdminPage.waitForLoad();
  });
});
