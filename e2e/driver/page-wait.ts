import {Page} from 'puppeteer';

export async function waitForNavigation(page: Page) {
  return Promise.all([
    page.waitForNavigation({waitUntil: 'load'}),
    page.waitForNavigation({waitUntil: 'domcontentloaded'})
  ]);
}

export async function waitUntilNetworkIdle(page: Page) {
  const networkidle0 = async () => {
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    return page.waitFor(500);
  };
   // recurring requests that could prevent the networkidle0 condition from ever being satisfied.
  const networkidle2 = async () => {
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
    return page.waitFor(2000);
  };
  return Promise.race([
    networkidle0(),
    networkidle2(),
  ]);
}
