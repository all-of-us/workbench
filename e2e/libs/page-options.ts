import { defaultArgs } from 'puppeteer';
import fp from 'lodash/fp';

const { PUPPETEER_DEBUG, PUPPETEER_HEADLESS, CI } = process.env;
const isCi = CI === 'true';
const isDebug = PUPPETEER_DEBUG === 'true';
const isHeadless = isDebug ? false : isCi || (PUPPETEER_HEADLESS || 'true') === 'true';

const customChromeOptions = [
  // Reduce cpu and memory usage. Disables one-site-per-process security policy, dedicated processes for site origins.
  '--disable-features=BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  // Page not loading Login URL when launched Chromium without this flag. It disables use of zygote process for forking child processes.
  // https://codereview.chromium.org/2384163002
  '--no-zygote',
  '--no-sandbox', // required for --no-zygote flag
  '--safebrowsing-disable-auto-update',
  '--window-size=1300,1024',
  '--window-position=0,0'
];

// Append to Puppeteer default chrome flags.
// https://github.com/puppeteer/puppeteer/blob/33f1967072e07824c5bf6a8c1336f844d9efaabf/lib/Launcher.js#L261
const chromeOptions = defaultArgs({
  userDataDir: null,
  headless: isHeadless,
  devtools: true,
  args: customChromeOptions
});

// Need for running Puppeteer headless Chromium in docker container.
const ciChromeOptions = fp.concat(chromeOptions, [
  '--disable-gpu', // https://bugs.chromium.org/p/chromium/issues/detail?id=737678#c10
  '--disable-setuid-sandbox'
]);

export const defaultLaunchOptions = {
  devtools: isDebug,
  slowMo: 20,
  defaultViewport: null,
  ignoreDefaultArgs: true,
  ignoreHTTPSErrors: true,
  args: isCi ? ciChromeOptions : chromeOptions
};
