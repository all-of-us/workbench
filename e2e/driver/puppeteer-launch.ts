import * as puppeteer from 'puppeteer';

const defaultLaunchOpts = {
  headless: !process.env.PUPPETEER_HEADLESS,
  slowMo: 10,
  defaultViewport: null,
  devtools: false,
  ignoreDefaultArgs: ['--disable-extensions'],
  args: [
    '--no-sandbox',
    '--disable-setuid-sandbox',
    '--disable-dev-shm-usage',
    '--disable-gpu',
    '--disable-web-security',
    '--disable-features=TranslateUI,BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  ]
};

export default async (opts?): Promise<puppeteer.Browser> => {
  const launchOptions = opts || defaultLaunchOpts;
  return await puppeteer.launch(launchOptions);
};
