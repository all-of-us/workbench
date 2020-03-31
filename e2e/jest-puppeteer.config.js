const lodash = require('lodash');
const puppeteer = require('puppeteer');
const isHeadless = process.env.HEADLESS !== 'false';

const NEW_CHROME_SWITCHES = [
  '--disable-web-security',
  '--disable-features=TranslateUI,BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  '--disable-gpu',
  '--disable-accelerated-2d-canvas', // disable gpu-accelerated 2d canvas
  '--no-zygote', // https://codereview.chromium.org/2384163002
  '--window-size=1920,1080',
];

// append to Puppeteer default chrome flags.
// https://github.com/puppeteer/puppeteer/blob/33f1967072e07824c5bf6a8c1336f844d9efaabf/lib/Launcher.js#L261
const DEFAULT_SWITCHES = puppeteer.defaultArgs({devtools: false, headless: isHeadless, userDataDir: null})
  .concat(...NEW_CHROME_SWITCHES)
  .filter(flag => flag !== '--disable-background-networking'); // filter out from default arguments

// merge without changing DEFAULT_SWITCHES
const CI_SWITCHES = lodash.assign([], DEFAULT_SWITCHES,
    [
      '--disable-dev-shm-usage',
      '--no-sandbox',
      '--disable-setuid-sandbox',
    ]
);

// switches for Chrome launch
const SWITCHES = (process.env.CI === 'true') ? CI_SWITCHES : DEFAULT_SWITCHES;

module.exports = {
  launch: {
    //dumpio: true,
    headless: isHeadless,
    slowMo: 10,
    defaultViewport: null,
    ignoreDefaultArgs: true, // ['--disable-extensions'], // filter out from default arguments
    args: SWITCHES, // Chrome switches to pass to the browser instance
  },
  browser: 'chromium',
  browserContext: (process.env.INCOGNITO || true) ? 'incognito' : 'default',
  exitOnPageError: true,
};
