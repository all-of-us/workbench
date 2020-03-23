import * as puppeteer from 'puppeteer';
import {getChromeFlags} from './chrome-startup-flags';

const isHeadless = !process.env.PUPPETEER_HEADLESS;

const defaultLaunchOpts = {
  headless: isHeadless,
  slowMo: 10,
  defaultViewport: null,
  devtools: false,
  ignoreDefaultArgs: true,
  args: getChromeFlags(isHeadless)
};

export default async (opts?): Promise<puppeteer.Browser> => {
  const launchOptions = opts || defaultLaunchOpts;
  return await puppeteer.launch(launchOptions);
};
