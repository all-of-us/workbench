const config = require('../src/config')
const impersonate = require('../src/impersonate')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

browserTest('sign in', async browser => {
  const page = browser.initialPage
  await page.goto(config.urlRoot(), {waitUntil: 'networkidle0'})
  await tu.impersonateUser(page, config.usernames[0])
  expect(await page.waitForSelector('[data-test-id="signed-in"]', {timeout: 4e3})).toBeDefined()
})
