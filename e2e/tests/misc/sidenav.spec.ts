import ProfilePage from 'app/page/profile-page';
import { signInWithAccessToken, signOut } from 'utils/test-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import { waitForDocumentTitle } from 'utils/waits-utils';
import { withPage } from 'libs/page-init';

describe('Sidebar Navigation', () => {
  test('SideNav menu', async () => {
    await withPage()(async (page) => {
      await signInWithAccessToken(page);

      // Navigate to Profile page.
      await Navigation.navMenu(page, NavLink.PROFILE);
      const profilePage = new ProfilePage(page);
      await profilePage.waitForLoad();
      expect(await profilePage.isLoaded()).toBe(true);

      // Select Sign Out link
      await signOut(page);
      await waitForDocumentTitle(page, 'Redirect Notice');
      const href = await page.evaluate(() => location.href);
      expect(href).toEqual(expect.stringMatching(/(\/|%2F)login$/));
    });
  });
});
