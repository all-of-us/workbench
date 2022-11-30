const config = require('../src/config')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

const navigateToNewAnalysisPage = async (browser) => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await page.goto(config.urlRoot()+'/workspaces/aou-rw-test-53ff4756/mohstest/data', {waitUntil: 'networkidle0'})
  const newAnalysisTab = await page.waitForSelector('div[role="button"][aria-label="Analysis (New)"]')
  await newAnalysisTab.click()
  return page
}

browserTest('create an application', async browser => {
  const page = await navigateToNewAnalysisPage(browser)

  const startButton = await page.waitForSelector('div[role="button"][aria-label="start"]')
  await startButton.click()

  await page.waitForSelector('div[aria-label="Select Applications Modal"]')

  await page.waitForSelector('div[role="button"][aria-label="close"]')

  const nextButton = await page.waitForSelector('div[role="button"][aria-label="next"]')
  await expect(nextButton.evaluate(n => n.style.cursor)).resolves.toBe('not-allowed')

  const applicationListDropdown = await page.waitForSelector('input[aria-label="Application List Dropdown"]')
  /*
  * Since Prime React uses attaches the aria-label of its Dropdown element to an
  * invisible element, Puppeteer's click method does not work as intended.
  * More details can be found here:
  * https://stackoverflow.com/a/66537619
  * */
  await applicationListDropdown.evaluate(b => b.click())

  const jupyterOption = await page.waitForSelector('li[role="option"][aria-label="JUPYTER"]')
  await jupyterOption.click()
  await nextButton.click()

  await page.waitForFunction(() => !document.querySelector('div[aria-label="Select Applications Modal"]'));
  await page.waitForSelector('div[aria-label="New Notebook Modal"]')

}, 10e3)

browserTest('Cancel the creation of an application', async browser => {
  const page = await navigateToNewAnalysisPage(browser)

  const startButton = await page.waitForSelector('div[role="button"][aria-label="start"]')
  await startButton.click()

  const closeButton = await page.waitForSelector('div[role="button"][aria-label="close"]')
  await closeButton.click()

  await page.waitForFunction(() => !document.querySelector('div[aria-label="Select Applications Modal"]'));

}, 10e3)

