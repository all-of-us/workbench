import {Page} from 'puppeteer';
import GoogleLoginPage from '../app/google-login';


export const signIn = async (page: Page) => {
  await GoogleLoginPage.logIn(page);
  // this element exists in DOM only after user signed in
  await page.waitFor(() => document.querySelector('body#body'));
};
