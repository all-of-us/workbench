const puppeteer = require('puppeteer-core')

const urlRoot = () => 'https://all-of-us-workbench-test.appspot.com'
module.exports.urlRoot = urlRoot

const launch = options => {
  const launchOptions = {
  ...{
    // Enable slowMo (e.g., 200ms) to slow down browser interaction to make it
    // easier to follow visually.
    // slowMo: 200,
    // dumpio: true,
    defaultViewport: null, //{width: 1024, height: 768},
    // args: ['--app-shell-host-window-size=1600x900'],
    args: ['--disable-gpu'],
  },
  ...(v => v === 'false' ? {headless: false} : {})(process.env.HEADLESS),
  ...(v => v ? {executablePath: v} : {})(process.env.PUPPETEER_EXECUTABLE_PATH),
  ...options
  }
  // console.log({launchOptions})
  // console.log(puppeteer.defaultArgs())
  return puppeteer.launch(launchOptions)
}

module.exports.launch = launch

const promiseEvent = (eventName, x) =>
  new Promise(resolve => x.on(eventName, (...args) => resolve(...args)))
module.exports.promiseEvent = promiseEvent

const closeBrowser = async browser => {
  // browser.close() hangs for about thirty seconds on my machine. Termination happens much
  // more quickly with a kill.
  const p = browser.process()
  p.kill()
  await promiseEvent('close', p)
}
module.exports.closeBrowser = closeBrowser

const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
module.exports.delay = delay

