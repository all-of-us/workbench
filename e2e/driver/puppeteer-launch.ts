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
    '--disable-background-timer-throttling',//
    '--disable-backgrounding-occluded-windows',//
    '--disable-renderer-backgrounding',
    '--disable-web-security',
    '--disable-breakpad',
    '--disable-component-extensions-with-background-pages',
    '--disable-extensions',
    '--disable-features=TranslateUI,BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
    '--disable-ipc-flooding-protection',
    '--enable-features=NetworkService,NetworkServiceInProcess',
    '--force-color-profile=srgb',
    '--hide-scrollbars',
    '--metrics-recording-only',
    '--mute-audio',
    '--headless',
  ]
};

export default async (opts?): Promise<puppeteer.Browser> => {
  const launchOptions = opts || defaultLaunchOpts;
  return await puppeteer.launch(launchOptions);
};
