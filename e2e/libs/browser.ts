import fp from 'lodash/fp';
import { logger } from 'libs/logger';
import { Browser, defaultArgs, launch } from 'puppeteer';

const isHeadless: boolean = Boolean(process.env.PUPPETEER_HEADLESS) || true;
const slowMotion = Number(process.env.PUPPETEER_SLOWMO) || 20;
const isCi = Boolean(process.env.CI) || false;

const CustomChromeOptions = [
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
const DefaultChromeOptions = fp.concat(
  defaultArgs({ devtools: false, headless: isHeadless, userDataDir: null }),
  CustomChromeOptions
);

// Need for running Puppeteer headless Chromium in docker container.
const CircleCiChromeOptions = fp.concat(DefaultChromeOptions, [
  '--disable-gpu', // https://bugs.chromium.org/p/chromium/issues/detail?id=737678#c10
  '--disable-setuid-sandbox'
]);

const launchOptions = {
  headless: isHeadless,
  slowMo: slowMotion,
  defaultViewport: null,
  ignoreDefaultArgs: true,
  args: isCi ? CircleCiChromeOptions : DefaultChromeOptions
};

export const withBrowser = async (test): Promise<void> => {
  const browser = await launch(launchOptions);
  try {
    await test(browser);
  } finally {
    await browser.close();
  }
};

export const withPage = (browser: Browser) => async (test): Promise<void> => {
  const [page] = await browser.pages();
  await test(page).catch((err: Error) => {
    logger.error(err);
    throw err;
  });
};
