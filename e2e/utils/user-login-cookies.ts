import * as fs from 'fs'
import {Cookie} from 'puppeteer';

function getCookieFileName(userId: string): string {
  const id = userId.split('@')[0];
  return `./${id}_cookies.json`;
}

export const saveCookiesToFile = (cookies: Cookie[], userId: string): void => {
  const cookiesFile = getCookieFileName(userId);
  fs.writeFileSync(cookiesFile, JSON.stringify(cookies, null, 2));
}

export const readCookiesFromFile = (userId: string): Cookie[] | null => {
  const cookiesFile = getCookieFileName(userId);
  const fileExists = fs.existsSync(cookiesFile);
  if (fileExists) {
    const cookies = fs.readFileSync(cookiesFile).toString();
    return JSON.parse(cookies);
  }
  return null;
}
