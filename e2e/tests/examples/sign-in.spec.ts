import ProfilePage from 'app/page/profile-page';
import { signInWithAccessToken, signOut } from 'utils/test-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import { withPageTest, withSignIn } from 'libs/page-manager';
import { config } from 'resources/workbench-config';
import HomePage from 'app/page/home-page';
import GoogleLoginPage from 'app/page/google-login';
import CookiePolicyPage from 'app/page/cookie-policy';
import WorkspacesPage from 'app/page/workspaces-page';

describe('login tests', () => {
  test('withPage: Sign in by access token', async () => {
    await withPageTest()(async (page) => {
      await signInWithAccessToken(page);

      // Navigate to Profile page.
      await Navigation.navMenu(page, NavLink.PROFILE);
      const profilePage = new ProfilePage(page);
      await profilePage.waitForLoad();
      expect(await profilePage.isLoaded()).toBe(true);

      await signOut(page);
    });
  });

  test('withPage example: Sign in as READER', async () => {
    await withPageTest()(async (page) => {
      await signInWithAccessToken(page, config.READER_ACCESS_TOKEN_FILE);
      const homePage = new HomePage(page);
      const plusIcon = homePage.getCreateNewWorkspaceLink();
      expect(await plusIcon.asElementHandle()).toBeTruthy();
    });
  });

  test('withPage example: No sign in', async () => {
    await withPageTest()(async (page, browser) => {
      const loginPage = new GoogleLoginPage(page);
      await loginPage.load();
      expect(await loginPage.loginButton()).toBeTruthy();
      const link = await loginPage.cookiePolicyLink();
      expect(link).toBeTruthy();
      await link.click();

      // This link is a target='_blank', so we need to capture the new page.
      const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
      const newPage = await newTarget.page();
      const cookiePage = new CookiePolicyPage(newPage);
      await cookiePage.loaded();
    });
  });

  test('withSignIn example: Default is access token', async () => {
    await withSignIn()(async (page, _browser) => {
      const workspacesPage = new WorkspacesPage(page);
      await workspacesPage.load();
      expect(await workspacesPage.isLoaded()).toBe(true);
    });
  });

  test('withSignIn example: Entering user email and password', async () => {
    await withSignIn(config.READER_ACCESS_TOKEN_FILE)(async (page, _browser) => {
      const workspacesPage = new WorkspacesPage(page);
      await workspacesPage.load();
      expect(await workspacesPage.isLoaded()).toBe(true);
    });
  });
});
