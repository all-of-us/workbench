const config = require('../src/config')
const tu = require('../src/test-utils')
const u = require('../src/utils')

const browserTest = tu.browserTest(__filename)

browserTest('wait for on-page event', async browser => {
  const page = browser.initialPage
  const [eventPromise] = await tu.promiseWindowEvent(page, 'fired')
  const [eventName] = await Promise.all([eventPromise, page.evaluate(() => {
    window.dispatchEvent(new Event('fired'))
  })])
  expect(eventName).toBe('fired')
})

