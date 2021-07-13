import { config } from 'resources/workbench-config';
import GoogleLoginPage from 'app/page/google-login';
import { withPage } from 'libs/page-manager';

describe('Login tests:', () => {
  const urls = [
    { value: `${config.uiBaseUrl}${config.workspacesUrlPath}` },
    { value: config.uiBaseUrl },
    { value: `${config.uiBaseUrl}${config.profileUrlPath}` }
  ];

  test.each(urls)('Redirects to login page', async (url) => {
    await withPage()(async (page) => {
      await page.goto(url.value, { waitUntil: 'networkidle0' });
      const loginPage = new GoogleLoginPage(page);
      expect(await loginPage.loginButton()).toBeTruthy();
    });
  });
});
