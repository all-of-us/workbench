const impersonate = require('../src/impersonate')
const utils = require('../src/utils')

let browser = null

beforeEach(async () => {
  browser = await utils.launch()
  browser.initialPage = (await browser.pages())[0]
})

afterEach(async () => {
  await utils.closeBrowser(browser)
  browser = null
})

test('sign in', async () => {
  const projectName = 'all-of-us-workbench-test'
  const username = 'puppeteer-tester-6@fake-research-aou.org'
  const page = browser.initialPage
  await page.goto(utils.urlRoot())
  const bearerToken = await impersonate.getBearerToken(projectName, username).toPromise()
  await page.evaluate(x => setTestAccessTokenOverride(x), bearerToken)
  expect(await page.waitForSelector('[data-test-id="signed-in"]')).toBeDefined()
})
