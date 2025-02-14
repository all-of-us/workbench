const http = require('http');
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
    // Enable to open with devtools already visible.
    // devtools: true,
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
    // Stolen from a recent Chrome version. Avoids browser warning.
    const uaString = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'
      +' AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36'
    await browser.initialPage.setUserAgent(uaString)
    // Default timeout is 30 seconds, which is too long for most operations
    // (e.g., finding an element).
    browser.initialPage.setDefaultTimeout(2e3)
    browser.initialPage.setDefaultNavigationTimeout(4e3)
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

const useApiProxy = async page => {
  assert(process.env.API_PROXY_URL, 'API_PROXY_URL not defined')
  await new Promise((resolve, reject) => {
    http.get(process.env.API_PROXY_URL + '/v1/config',
      response => {
        assert(response.statusCode === 200);
        resolve()
      }
    ).on('error', (cause) => {
      reject(new Error('Proxy request failed. \n' + cause + '\nIs your API Proxy running?'))
    })
  })
  await page.setJavaScriptEnabled(false)
  await page.goto(config.urlRoot())
  await page.evaluate(
    url => { localStorage.allOfUsApiUrlOverride = url},
    process.env.API_PROXY_URL
  )
  await page.setJavaScriptEnabled(true)


}
export_({useApiProxy})

const fakeSignIn = async page => {
  await page.evaluate(() => localStorage['test-access-token-override'] = 'fake-bearer-token')
  await page.reload()
  await expect(page.waitForSelector('[data-test-id="signed-in"]')).resolves.toBeDefined()
}
export_({fakeSignIn})

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
  expect(await page.waitForSelector('[data-test-id="signed-in"]', {timeout: 4e3})).toBeDefined()
}
export_({impersonateUser})

const promiseWindowEvent = async (page, eventName) => {
  let resolveEvent = undefined
  let timeout = undefined
  const eventPromise = new Promise(resolve => { resolveEvent = resolve })
  const timeoutPromise = new Promise((resolve, reject) => {
    timeout = setTimeout(
      () => reject(new Error(`Timed out waiting for event "${eventName}"`)),
      page.getDefaultTimeout()
    )
  })

  await page.exposeFunction('handleEvent', eventType => {
    clearTimeout(timeout)
    resolveEvent(eventType)
  })
  await page.evaluate(eventName => {
    window.addEventListener(eventName, async e => { handleEvent(e.type) })
  }, eventName)
  // This has to be wrapped so callers can await this function to get the event handler installed
  // without also awaiting the event itself. The auto-chaining nature of promises in JavaScript
  // makes cases like this one particularly ugly.
  return [Promise.race([eventPromise, timeoutPromise])]
}
export_({promiseWindowEvent})

// Puppeteer's Page.click is not reliable. This is worth investigation.
const jsClick = (page, sel) => page.waitForSelector(sel).then(eh => eh.evaluate(e => e.click()))
export_({jsClick})
