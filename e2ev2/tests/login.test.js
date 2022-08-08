const config = require('../src/config')
const impersonate = require('../src/impersonate')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

browserTest('sign in', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
})
