import {Page} from 'puppeteer';
import GoogleLoginPage from 'app/google-login';


export const signIn = async (page: Page) => {
  await GoogleLoginPage.logIn(page);
   // this element exists in DOM after user has logged in
  await page.waitFor(() => document.querySelector('app-signed-in') !== null);
};
