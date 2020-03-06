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

export async function handleRecaptcha(page: Page) {
  const css = '[id="recaptcha-anchor"][role="checkbox"]';
  await page.frames().find(async (frame) => {
    for(const childFrame of frame.childFrames()) {
      const recaptcha = await childFrame.$$(css);
      if (recaptcha.length > 0) {
        await recaptcha[0].click();
        return;
      }
    }

  });
}
