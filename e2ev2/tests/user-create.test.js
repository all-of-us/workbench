const config = require('../src/config')
const tu = require('../src/test-utils')
const u = require('../src/utils')

const browserTest = tu.browserTest(__filename)

// Puppeteer's Page.click is not reliable. This is worth investigation.
const jsClick = (page, sel) => page.waitForSelector(sel).then(eh => eh.evaluate(e => e.click()))

const pressKey = async (page, keyName, count = 1) => {
  for (let i = 0; i < count; ++i) {
    await page.keyboard.press(keyName)
  }
}

browserTest('create user', async browser => {
  const page = browser.initialPage
  const iframeSel = '[aria-label="terms of use and privacy statement"]>iframe'
  const ackCheckboxSel = '[data-test-id="agreement-check"]'
  const isAckCheckboxDisabled =
    () => page.waitForSelector(ackCheckboxSel).then(eh => eh.evaluate(e => e.disabled))

  await tu.useApiProxy(page)
  await page.goto(config.urlRoot(), {waitUntil: 'networkidle0'})
  await jsClick(page, '[data-test-id="login"]>div+div>[role="button"]') // Create Account

  await page.waitForSelector(iframeSel) // Agreement Page
  await page.evaluate(() => window.forceHasReadEntireAgreement())
  await expect(isAckCheckboxDisabled()).resolves.toBe(false)
  await jsClick(page, ackCheckboxSel)
  await page.click('[role="button"]') // Next button

  await page.waitForSelector('.p-dropdown') // Institution Page
  await page.click('.p-dropdown')
  await page.waitForSelector('[role="option"][aria-label="Broad Institute"]')
  await page.click('[role="option"][aria-label="Broad Institute"]')
  // Email needs to be real or it will bounce and account creation will fail.
  await page.type('#contact-email', 'dmohs@broadinstitute.org')
  // The second dropdown, which is a child of a sibling of the first one.
  await page.click('.p-dropdown~div>div>.p-dropdown')
  await page.waitForSelector('[role="option"][aria-label^="Project Personnel"]')
  await page.click('[role="option"][aria-label^="Project Personnel"]')
  await jsClick(page, '[role="button"]+[role="button"]')

  await page.waitForSelector('#username') // Personal Information Page
  await page.type('#username', 'test')
  await page.keyboard.press('Tab')
  await page.keyboard.type('Test')
  await page.keyboard.press('Tab')
  await page.keyboard.type('User')
  await pressKey(page, 'Tab', 2)
  await page.keyboard.type('123 Anytown Rd')
  await pressKey(page, 'Tab', 2)
  await page.keyboard.type('Anytown')
  await page.keyboard.press('Tab')
  await page.keyboard.type('TN')
  await page.keyboard.press('Tab')
  await page.keyboard.type('12345')
  await page.keyboard.press('Tab')
  await page.keyboard.type('United')
  await page.keyboard.press('Enter')
  await page.keyboard.press('Tab')
  await page.keyboard.type('Testing the system.')
  await pressKey(page, 'Tab', 3)
  await page.keyboard.press('Enter')

  await page.waitForSelector('iframe[title="reCAPTCHA"]') // Demographics Survey Page
  await pressKey(page, 'Tab', 10)
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab', 9)
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab', 2)
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab', 10)
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab', 2)
  await pressKey(page, 'ArrowRight', 17) // Yikes
  await pressKey(page, 'Tab', 3)
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab')
  await page.keyboard.press('Space')
  await pressKey(page, 'Tab', 2)
  await pressKey(page, 'ArrowRight', 2)
  await page.waitForSelector('#g-recaptcha-response')
  await pressKey(page, 'Tab', 2)
  const [captchaSolved] = await tu.promiseWindowEvent(page, 'captchaSolved')
  await Promise.all([captchaSolved, page.keyboard.press('Space')])
  await page.click('[data-test-id="sign-in-page"]+div [role="button"]+[role="button"]')

  await page.waitForSelector('h2')
  await expect(page.waitForSelector('h2').then(eh => eh.evaluate(e => e.innerText)))
    .resolves.toBe('Congratulations!')
}, 20e3)

