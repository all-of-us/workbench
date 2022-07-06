const assert = require('assert').strict
const fs = require('fs')
const fsp = fs.promises
const path = require('path')
const puppeteer = require('puppeteer-core')
const config = require('./config')
const impersonate = require('./impersonate')
const {getSaBearerToken} = require('./sa-bearer')
const {promiseEvent} = require('./utils')

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const launch = options => {
  const launchOptions = {
  ...{
    // Enable slowMo (e.g., 200ms) to slow down browser interaction to make it
    // easier to follow visually.
    // slowMo: 200,
    // Enable dumpio to see the browser's console output.
    // dumpio: true,
    defaultViewport: null, // allow the viewport to use available window space
    // args: ['--app-shell-host-window-size=1600x900'],
    args: ['--disable-gpu'],
  },
  ...(v => v === 'false' ? {headless: false} : {})(process.env.HEADLESS),
  ...(v => v ? {executablePath: v} : {})(process.env.PUPPETEER_EXECUTABLE_PATH),
  ...options
  }
  // console.log(puppeteer.defaultArgs())
  return puppeteer.launch(launchOptions)
}
export_({launch})

const closeBrowser = async browser => {
  // browser.close() hangs for about thirty seconds on my machine. Termination happens much
  // more quickly with a kill.
  const p = browser.process()
  p.kill()
  await promiseEvent('close', p)
}
export_({closeBrowser})

const browserTest = testFilePath => (description, testFn, timeoutMs) =>
  test(description, async () => {
    const ssDir = 'screenshots'
    await fsp.access(ssDir, fs.constants.R_OK | fs.constants.W_OK).catch(e => fsp.mkdir(ssDir))
    const browser = await launch()
    browser.initialPage = await browser.pages().then(pages => pages[0])
    // Stolen from a recent Chrome version.
    const uaString = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'
      +' AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36'
    await browser.initialPage.setUserAgent(uaString)
    // Default timeout is 30 seconds, which is too long for most operations
    // (e.g., finding an element).
    browser.initialPage.setDefaultTimeout(2000)
    try {
      return await testFn(browser)
    } catch (e) {
      await browser.pages().then(pages => Promise.all(pages.map((page, i) => {
        const ssBase = `${path.basename(testFilePath)}.${description.replace(/[/]/g, '|')}`
        const ssPath = path.join(ssDir, `${ssBase}${i > 0 ? ('.'+(i+1)) : ''}.png`)
        return page.screenshot({path: ssPath, fullPage: true})
      })))
      throw e
    } finally {
      await closeBrowser(browser)
    }
  }, timeoutMs)
export_({browserTest})

const impersonateUser = async (page, username) => {
  const saBearerToken = await (() => {
    assert(process.env.SA_KEY_JSON, 'SA_KEY_JSON not defined')
    return getSaBearerToken(process.env.SA_KEY_JSON).map(x => x.access_token).toPromise()
  })()
  const bearerToken =
    await impersonate.getBearerTokenForUser(saBearerToken, config.projectName, username)
      .map(x => x.access_token)
      .toPromise()
  await page.evaluate(t => setTestAccessTokenOverride(t), bearerToken)
  expect(await page.waitForSelector('[data-test-id="signed-in"]', 4e3)).toBeDefined()
}
export_({impersonateUser})
