import { Page, Frame } from 'puppeteer';
import * as xpathDefaults from 'app/xpath-builders';

const waitForFn = async (fn: () => any, interval = 2000, timeout = 10000): Promise<boolean> => {
  const start = Date.now();
  while (Date.now() < start + timeout) {
    if (fn()) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
  return false;
};

export async function findIframe(page: Page, label: string): Promise<Frame> {
  const iframeNode = await page.waitForXPath(xpathDefaults.iframeXpath(label));
  const srcHandle = await iframeNode.getProperty('src');
  const src = await srcHandle.jsonValue();
  const hasFrame = (): Frame => page.frames().find((frame) => frame.url() === src);

  await waitForFn(hasFrame);
  return hasFrame();
}
