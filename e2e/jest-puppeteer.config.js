/**
 * This file is required by jest-puppeteer preset.
 * https://github.com/smooth-code/jest-puppeteer#configure-puppeteer
 */

const fp = require('lodash/fp');
const puppeteer = require('puppeteer');
const isHeadless = (process.env.PUPPETEER_HEADLESS || 'true') === 'true';
const slowMotion = parseInt(process.env.PUPPETEER_SLOWMO, 10) || 10;

const NEW_CHROME_SWITCHES = [
  // Reduce cpu and memory usage. Disables one-site-per-process security policy, dedicated processes for site origins.
  '--disable-features=BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  // Page not loading Login URL when launched Chromium without this flag. It disables use of zygote process for forking child processes.
  // https://codereview.chromium.org/2384163002
  '--no-zygote',
  '--no-sandbox', // required for --no-zygote flag
  '--safebrowsing-disable-auto-update',
  '--window-size=1300,1024',
  '--incognito'
];

// Append to Puppeteer default chrome flags.
// https://github.com/puppeteer/puppeteer/blob/33f1967072e07824c5bf6a8c1336f844d9efaabf/lib/Launcher.js#L261
const DEFAULT_SWITCHES = fp.concat(
  puppeteer.defaultArgs({ devtools: false, headless: isHeadless, userDataDir: null }),
  NEW_CHROME_SWITCHES
);

// Need for running Puppeteer headless Chromium in docker container.
const CI_SWITCHES = fp.concat(DEFAULT_SWITCHES, [
  '--disable-gpu', // https://bugs.chromium.org/p/chromium/issues/detail?id=737678#c10
  '--disable-setuid-sandbox'
]);

const SWITCHES = process.env.CI === 'true' ? CI_SWITCHES : DEFAULT_SWITCHES;

module.exports = {
  launch: {
    headless: isHeadless,
    slowMo: slowMotion, // slow down creation of browser to free up heap memory. https://github.com/puppeteer/puppeteer/issues/4684#issuecomment-511255786
    defaultViewport: null,
    ignoreDefaultArgs: true,
    args: SWITCHES // Chrome switches to pass to the browser instance
  },
  browser: 'chromium',
  browserContext: process.env.INCOGNITO || true ? 'incognito' : 'default',
  exitOnPageError: false
};
