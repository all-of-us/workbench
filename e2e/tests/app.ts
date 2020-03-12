import {Page} from 'puppeteer';
import GoogleLoginPage from '../app/google-login';

const configs = require('../resources/workbench-config');

export const signIn = async (page: Page) => {
  await page.setUserAgent(configs.puppeteerUserAgent);
  await page.setDefaultNavigationTimeout(60000);
  await GoogleLoginPage.logIn(page);
  // this element exists in DOM only after user signed in
  await page.waitFor(() => document.querySelector('body#body'));
};
