import GoogleLoginPage, {SELECTOR} from 'app/page/google-login';
import {config} from 'resources/workbench-config';


describe('Login tests:', () => {

  test('Open AoU Workspaces page before login redirects to login page', async () => {
    const url = config.uiBaseUrl + config.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);
    await page.goto(url, {waitUntil: 'networkidle0'});
    expect(await loginPage.loginButton()).toBeTruthy();
  });

  test('Entered wrong password', async () => {
    const INCORRECT_PASSWORD = 'wrongpassword123';
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    const naviPromise = page.waitForNavigation({waitUntil: 'networkidle0'});
    const googleButton = await loginPage.loginButton();
    await googleButton.click();
    await naviPromise;

    await loginPage.enterEmail(config.userEmail);
    await loginPage.enterPassword(INCORRECT_PASSWORD);

    const button = await page.waitForXPath(SELECTOR.NextButton, {visible: true});
    await button.click();

    const err = await loginPage.waitForTextExists('Wrong password. Try again');
    expect(err).toBeTruthy();
  });

});
