import * as chromePaths from 'chrome-paths';
import * as Puppeteer from 'puppeteer-core';
import {Browser} from 'puppeteer-core';
import * as configs from '../config/config.js';

const defaultLaunchOpts = {
  headless: configs.puppeteerHeadless,
  slowMo: configs.puppeteerSlowMotion,
  executablePath: chromePaths.chrome,
  defaultViewport: null,
  devtools: configs.puppeteerDevTools,
  args: [
    '--no-sandbox',
    '--disable-setuid-sandbox',
    '--disable-dev-shm-usage',
    '--disable-gpu',
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding'
  ]
};

export default async (opts?): Promise<Browser> => {
  const launchOptions = opts || defaultLaunchOpts;
  return await Puppeteer.launch(launchOptions);
};
