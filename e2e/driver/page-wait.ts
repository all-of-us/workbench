import {Page} from 'puppeteer';

// Experienmental method
export async function waitUntilNetworkIdle(page: Page) {
  const networkidle0 = async () => {
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    return page.waitFor(1000);
  };
   // recurring requests that could prevent the networkidle0 condition from ever being satisfied.
  const networkidle2 = async () => {
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
    return page.waitFor(1000);
  };
  const domloaded = async () => {
    await page.waitForNavigation({ waitUntil: 'domcontentloaded' });
    return page.waitFor(2000);
  };
  return Promise.race([
    networkidle0(),
    networkidle2(),
    domloaded(),
  ]);
}
