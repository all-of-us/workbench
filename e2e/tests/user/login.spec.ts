import CookiePolicyPage from 'app/page/cookie-policy';
import GoogleLoginPage from 'app/page/google-login';
import { config } from 'resources/workbench-config';

describe('Login tests:', () => {
  test('Cookie banner visible on login page', async () => {
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

  test('Open AoU Workspaces page before login redirects to login page', async () => {
    const url = config.uiBaseUrl + config.workspacesUrlPath;
    await page.goto(url, { waitUntil: 'networkidle0' });
    const loginPage = new GoogleLoginPage(page);
    expect(await loginPage.loginButton()).toBeTruthy();
  });
});
