const config = require('../src/config')
const tu = require('../src/test-utils')
const u = require('../src/utils')

const browserTest = tu.browserTest(__filename)

const pressKey = async (page, keyName, count = 1) => {
  for (let i = 0; i < count; ++i) {
    await page.keyboard.press(keyName)
  }
}

browserTest('create user', async browser => {
  const page = browser.initialPage
  const ackCheckboxSel = 'input[aria-label="Acknowledge Terms"]'
  const isAckCheckboxDisabled =
    () => page.waitForSelector(ackCheckboxSel).then(eh => eh.evaluate(e => e.disabled))

  await tu.useApiProxy(page)
  await page.goto(config.urlRoot())
  await tu.jsClick(page, '[role="button"][aria-label="Create Account"]')

  // Agreement Page
  await page.waitForSelector('[aria-label="terms of use and privacy statement"]>iframe')
  await expect(isAckCheckboxDisabled()).resolves.toBe(true)
  await page.evaluate(() => window.forceHasReadEntireAgreement())
  await expect(isAckCheckboxDisabled()).resolves.toBe(false)
  await tu.jsClick(page, ackCheckboxSel)
  await Promise.resolve('[role="button"][aria-label="Next"]').then(sel => {
    page.waitForFunction(sel => document.querySelector(sel).style.cursor !== 'not-allowed', {}, sel)
    return sel
  }).then(sel => page.click(sel))

  await page.waitForSelector('#account-creation-institution') // Institution Page
  await page.waitForSelector('[aria-label="Institution"]')
    .then(eh => eh.evaluate(e => e.parentNode.click()))
  await page.waitForSelector('[role="option"][aria-label="Broad Institute"]').then(eh => eh.click())
  // Email needs to be real or it will bounce and account creation will fail.
  await page.type('#contact-email', 'dmohs@broadinstitute.org')
  // The second dropdown, which is a child of a sibling of the first one.
  await page.waitForSelector('[aria-label="Role"]')
    .then(eh => eh.evaluate(e => e.parentNode.parentNode.click()))
  await page.waitForSelector('[role="option"][aria-label^="Project Personnel"]')
    .then(eh => eh.click())
  await Promise.resolve('[role="button"][aria-label="Next"]').then(sel => {
    page.waitForFunction(sel => document.querySelector(sel).style.cursor !== 'not-allowed', {}, sel)
    return sel
  }).then(sel => page.click(sel))

  await page.waitForSelector('#account-creation') // Personal Information Page
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
  await Promise.resolve('[role="button"][aria-label="Next"]').then(sel => {
    page.waitForFunction(sel => document.querySelector(sel).style.cursor !== 'not-allowed', {}, sel)
    return sel
  }).then(sel => page.click(sel))

  await page.waitForSelector('#demographics-survey') // Demographics Survey Page
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
  await pressKey(page, 'Tab', 2)
  await Promise.resolve('[role="button"][aria-label="Submit"]').then(sel => {
    page.waitForFunction(sel => document.querySelector(sel).style.cursor !== 'not-allowed', {}, sel)
    return sel
  }).then(sel => page.click(sel))

  await page.waitForSelector('#account-creation-success')
  await expect(page.waitForSelector('h2').then(eh => eh.evaluate(e => e.innerText)))
    .resolves.toBe('Congratulations!')
}, 20e3)

