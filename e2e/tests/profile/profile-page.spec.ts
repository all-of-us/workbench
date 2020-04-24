import HomePage from 'app/page/home-page';
import ProfilePage from 'app/page/profile-page';
import {signIn} from 'utils/app-utils';
import navigation, {NavLink} from 'app/component/navigation';

describe('Profile', () => {

  beforeEach(async () => {
    await signIn(page);
  });


  test('Click First and Last name fields on Profile page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForLoad();
    await navigation.navMenu(page, NavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
      // check last and first name textbox is not empty
    expect(fname).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(lname).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(lname).not.toEqual(fname);
  });

});
