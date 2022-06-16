const puppeteer = require('puppeteer-core')
const utils = require('../src/utils')

let browser = null

beforeEach(async () => {
  browser = await utils.launch()
})

afterEach(async () => {
  await utils.closeBrowser(browser)
  browser = null
})

test('page loads', async () => {
  const page = (await browser.pages())[0]
  await page.goto('https://example.com')
  const h1 = await page.waitForSelector('h1')
  expect(await h1.evaluate(n => n.innerText)).toBe('Example Domain')
})

test('view cookie policy page', async () => {
  const page = (await browser.pages())[0]
  await page.goto(utils.urlRoot()+'/login')
  const cpLink = await page.waitForSelector('a[href="/cookie-policy"]')
  expect(cpLink).toBeDefined()
  await cpLink.click()
  const target = await browser.waitForTarget(target => target.opener() === page.target())
  const cpPage = await target.page()
  const h3 = await cpPage.waitForSelector('h3')
  expect(await h3.evaluate(n => n.innerText))
    .toBe('All of Us Research Program Cookie Policy')
})

const paths = ['/workspaces', '/profile'].map(x => [x]) // expected format for test.each
test.each(paths)('navigation to %s redirects to sign-in page', async p => {
  const page = (await browser.pages())[0]
  await page.goto(utils.urlRoot()+p)
  const button = await page.waitForSelector('[role="button"]')
  expect(await button.evaluate(n => n.textContent)).toBe('Sign In')
})
