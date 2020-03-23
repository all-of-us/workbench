import {defaultArgs} from 'puppeteer';

const EXTEND_CHROME_FLAGS: ReadonlyArray<string> = [
  '--no-sandbox',
  '--disable-setuid-sandbox',
  '--disable-dev-shm-usage',
  '--disable-web-security',
  '--disable-features=TranslateUI,BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
  '--disable-gpu',
  '--disable-accelerated-2d-canvas', // disable gpu-accelerated 2d canvas because disabled gpu
  '--no-zygote', // https://codereview.chromium.org/2384163002
  '--window-size=1920,1080',
];

export function getChromeFlags(headlessMode: boolean = true):  string[] {
  let flags = defaultArgs();
  flags.push(...EXTEND_CHROME_FLAGS); // append my default flags to Puppeteer default flags
  flags = flags.filter(flag => flag !== '--disable-background-networking');
  if (!headlessMode) {
     // configure to use the GPU
    flags = flags.filter(flag => flag !== '--disable-gpu'); // remove
    flags = flags.filter(flag => flag !== '--headless'); // remove
    flags.push('--use-gl=desktop'); // add Use desktop graphics
  }
  return flags;
}
