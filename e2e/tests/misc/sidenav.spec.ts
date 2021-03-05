import ProfilePage from 'app/page/profile-page';
import {signInWithAccessToken} from 'utils/test-utils';
import Navigation, {NavLink} from 'app/component/navigation';

describe('Sidebar Navigation', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('SideNav menu', async () => {
    // Select Profile link
    await Navigation.navMenu(page, NavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();
    expect(await profilePage.isLoaded()).toBe(true);
  });

});
