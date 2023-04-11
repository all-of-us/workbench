const config = require('../src/config')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

browserTest('page loads', async browser => {
  const page = browser.initialPage
  await page.goto('https://example.com')
  const h1 = await page.waitForSelector('h1')
  expect(await h1.evaluate(n => n.innerText)).toBe('Example Domain')
})

browserTest('view cookie policy page', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
    .then(async () => {
      await page.goto(config.urlRoot()+'/login')
      const cpLink = await page.waitForSelector('a[href="/cookie-policy"]')
      expect(cpLink).toBeDefined()
      // click and wait need to happen simultaneously to avoid a race
      const [, target] = await Promise.all([
        cpLink.click(),
        browser.waitForTarget(target => target.opener() === page.target(), {timeout: 2e3})
      ])
      const cpPage = await target.page()
      cpPage.setDefaultTimeout(2000)
      const h3 = await cpPage.waitForSelector('h3')
      expect(await h3.evaluate(n => n.innerText))
        .toBe('All of Us Research Program Cookie Policy')})
    .catch((err) => {
      throw err
    })

}, 10e3) // page loads take some time

for (const p of ['/workspaces', '/profile']) {
  browserTest(`navigation to ${p} redirects to sign-in page`, async browser => {
    const page = browser.initialPage
    await tu.useApiProxy(page)
    await page.goto(config.urlRoot()+p)
    const button = await page.waitForSelector('[role="button"]')
    expect(await button.evaluate(n => n.textContent)).toBe('Sign In')
  })
}
