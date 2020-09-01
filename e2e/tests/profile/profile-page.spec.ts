import ProfilePage from 'app/page/profile-page';
import {signIn} from 'utils/test-utils';
import navigation, {NavLink} from 'app/component/navigation';
import {makeString, makeUrl} from 'utils/str-utils';
import Button from '../../app/element/button';

describe('Profile', () => {
  // initialized in beforeEach()
  let profilePage: ProfilePage;

  async function waitForSaveButton(isActive: boolean): Promise<Button> {
    const button = await profilePage.getSaveProfileButton();
    const isCursorEnabled = ! (await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe(isActive);
    return button;
  }

  beforeEach(async () => {
    await signIn(page);
    await navigation.navMenu(page, NavLink.PROFILE);
    profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();

    // Verify "Save Profile" button is disabled first time page is opened.
    await waitForSaveButton(false);
  });

  test('Edit single field of existing user profile', async () => {
    const testText = makeString(50);

    // Type in Research Background textarea
    let researchBackground = await profilePage.getResearchBackgroundTextarea();
    await researchBackground.paste(testText);

    // profile update should enable Save button
    const saveButton = await waitForSaveButton(true);
    await saveButton.click();
    await profilePage.waitForLoad();

    researchBackground = await profilePage.getResearchBackgroundTextarea();
    expect(await researchBackground.getValue()).toBe(testText);
  });

  test('Edit all fields of existing user profile', async () => {
    const testTextFirstName = makeString(10);
    const testTextLastName = makeString(10);
    const testTextURL = makeUrl(10);
    const testTextResearchBackground = makeString(10);
    const testTextAddress1 = makeString(10);
    const testTextAddress2 = makeString(10);
    const testTextCity = makeString(10);
    const testTextState = makeString(10);
    const testTextZip = makeString(10);
    const testTextCountry = makeString(10);

    let firstName = await profilePage.getFirstNameInput();
    let lastName = await profilePage.getLastNameInput();
    let url = await profilePage.getProfessionalUrlInput();
    let researchBackground = await profilePage.getResearchBackgroundTextarea();
    let address1 = await profilePage.getAddress1Input();
    let address2 = await profilePage.getAddress2Input();
    let city = await profilePage.getCityInput();
    let state = await profilePage.getStateInput();
    let zip = await profilePage.getZipCodeInput();
    let country = await profilePage.getCountryInput();

    await firstName.type(testTextFirstName);
    await lastName.type(testTextLastName);
    await url.type(testTextURL);
    await researchBackground.type(testTextResearchBackground);
    await address1.type(testTextAddress1);
    await address2.type(testTextAddress2);
    await city.type(testTextCity);
    await state.type(testTextState);
    await zip.type(testTextZip);
    await country.type(testTextCountry);

    // profile update should enable Save button
    const saveButton = await waitForSaveButton(true);
    await saveButton.click();
    await profilePage.waitForLoad();

    firstName = await profilePage.getFirstNameInput();
    lastName = await profilePage.getLastNameInput();
    url = await profilePage.getProfessionalUrlInput();
    researchBackground = await profilePage.getResearchBackgroundTextarea();
    address1 = await profilePage.getAddress1Input();
    address2 = await profilePage.getAddress2Input();
    city = await profilePage.getCityInput();
    state = await profilePage.getStateInput();
    zip = await profilePage.getZipCodeInput();
    country = await profilePage.getCountryInput();

    expect(await firstName.getValue()).toBe(testTextFirstName);
    expect(await lastName.getValue()).toBe(testTextLastName);
    expect(await url.getValue()).toBe(testTextURL);
    expect(await researchBackground.getValue()).toBe(testTextResearchBackground);
    expect(await address1.getValue()).toBe(testTextAddress1);
    expect(await address2.getValue()).toBe(testTextAddress2);
    expect(await city.getValue()).toBe(testTextCity);
    expect(await state.getValue()).toBe(testTextState);
    expect(await zip.getValue()).toBe(testTextZip);
    expect(await country.getValue()).toBe(testTextCountry);
  });

  test('A missing required field disables the save button', async () => {
    const researchBackground = await profilePage.getResearchBackgroundTextarea();
    await researchBackground.type(makeString(10));

    // save button is enabled and no error message is displayed
    await waitForSaveButton(true);
    await profilePage.expectResearchPurposeErrorMissing();

    // remove text from Research Background textarea
    await researchBackground.clear();

    // save button is disabled and error message is displayed
    await waitForSaveButton(false);
    await profilePage.expectResearchPurposeErrorPresent();
  });


});
