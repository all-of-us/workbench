module.exports = {
  launch: {
    dumpio: process.env.HEADLESS === 'false',
    slowMo: process.env.SLOWMO ? process.env.SLOWMO : 10,
    defaultViewport: null,
    headless: process.env.HEADLESS !== 'false',
    ignoreDefaultArgs: ['--disable-extensions'],
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--disable-background-timer-throttling',
      '--disable-backgrounding-occluded-windows',
      '--disable-renderer-backgrounding',
      '--disable-web-security',
      '--disable-features=IsolateOrigins,site-per-process',
      '--disable-site-isolation-trials',
    ],
  },
  browser: 'chromium',
  browserContext: (process.env.INCOGNITO || true) ? 'incognito' : 'default',
};
