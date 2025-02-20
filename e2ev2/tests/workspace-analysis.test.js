const config = require('../src/config')
const tu = require('../src/test-utils')
const utils = require("../src/utils");

const browserTest = tu.browserTest(__filename)

const navigateToAnalysisTab = async (browser) => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await utils.dismissLeoAuthErrorModal(page);
  await page.goto(config.urlRoot()+'/workspaces/aou-rw-test-53ff4756/mohstest/data')
  await utils.dismissLeoAuthErrorModal(page);
  await utils.dismissPrivacyWarning(page);
  await tu.jsClick(page,'div[role="button"][aria-label="Analysis"]')
  await tu.jsClick(page,'div[role="button"][aria-label="Analysis"]')
  await page.waitForSelector('h3[aria-label="Your Analyses"]')
  return page
}

browserTest('create an application', async browser => {
  const page = await navigateToAnalysisTab(browser)

  await tu.jsClick(page,'div[role="button"][aria-label="start"]')

  await page.waitForSelector('div[aria-label="Select Applications Modal"]')

  const nextButton = await page.waitForSelector('div[role="button"][aria-label="next"]')
  await expect(nextButton.evaluate(n => n.style.cursor)).resolves.toBe('not-allowed')

  await page.waitForSelector('#application-list-dropdown').then(eh => eh.evaluate(e => e.click()))

  await tu.jsClick(page,'li[role="option"][aria-label="Jupyter"]')
  await tu.jsClick(page,'div[role="button"][aria-label="next"]')

  await page.waitForFunction(() => !document.querySelector('div[aria-label="Select Applications Modal"]'));
  await page.waitForSelector('div[aria-label="New Notebook Modal"]')

}, 10e3)

browserTest('Cancel the creation of an application', async browser => {
  const page = await navigateToAnalysisTab(browser)

  await tu.jsClick(page,'div[role="button"][aria-label="start"]')
  await tu.jsClick(page,'div[role="button"][aria-label="close"]')

  await page.waitForFunction(() => !document.querySelector('div[aria-label="Select Applications Modal"]'));

}, 10e3)

