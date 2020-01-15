import * as chromePaths from 'chrome-paths';
import * as Puppeteer from 'puppeteer-core';
import {Browser} from 'puppeteer-core';

const defaultLaunchOpts = {
  headless: true,
  slowMo: 10,
  executablePath: chromePaths.chrome,
  defaultViewport: null,
  devtools: false,
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
