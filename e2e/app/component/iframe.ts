import {Frame, Page} from 'puppeteer';
import {waitForFn} from 'utils/wait-utils';
import {iframeXpath} from 'app/element/xpath-defaults';

export default class IFrame {

  static async find(page: Page, label: string): Promise<Frame> {
    const iframeNode = await page.waitForXPath(iframeXpath(label));
    const srcHandle = await iframeNode.getProperty('src');
    const src = await srcHandle.jsonValue();
    const hasFrame = (): Frame => page.frames().find(frame => frame.url() === src);
    await waitForFn(hasFrame);
    return hasFrame();
  }

}
