import CookiePolicyPage from 'app/page/cookie-policy';
import GoogleLoginPage from 'app/page/google-login';
import { config } from 'resources/workbench-config';
import { withPageTest } from 'libs/page-manager';

describe('Login tests:', () => {
  test('Cookie banner visible on login page', async () => {
    await withPageTest()(async (page, browser) => {
      const loginPage = new GoogleLoginPage(page);
      await loginPage.load();
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

  const urls = [
    { value: `${config.LOGIN_URL_DOMAIN_NAME}${config.WORKSPACES_URL_PATH}` },
    { value: config.LOGIN_URL_DOMAIN_NAME },
    { value: `${config.LOGIN_URL_DOMAIN_NAME}${config.PROFILE_URL_PATH}` }
  ];

  test.each(urls)('Open AoU Workspaces page before login redirects to login page', async ({ value }) => {
    await withPageTest()(async (page) => {
      await page.goto(value, { waitUntil: ['load', 'networkidle0'] });
      const loginPage = new GoogleLoginPage(page);
      expect(await loginPage.loginButton()).toBeTruthy();
    });
  });
});
