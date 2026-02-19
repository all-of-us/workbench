const config = require('../src/config')
const tu = require('../src/test-utils')

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
  await page.type('[aria-label="Institution"]',"Broad Institute");
  await page.keyboard.press('Tab')

  // Email needs to be real or it will bounce and account creation will fail.
  await page.type('#contact-email', 'dmohs@broadinstitute.org')
  // The second dropdown, which is a child of a sibling of the first one.
  await page.waitForSelector('[aria-label="Select Role"]')
    .then(eh => eh.evaluate(e => e.parentNode.click()))
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
  }).then(sel => tu.jsClick(page,sel))

  await page.waitForSelector('#demographics-survey') // Demographics Survey Page
  // Fill in all required demographics survey fields via direct clicks.
  // Checkbox indices: Q1 ethnicity [0-8], Q2 gender [9-18], Q4 sexual orientations [19-30], Q12 year of birth PNA [31]
  // Radio indices: Q3 sex at birth [0-4], Q5-Q10 disability [5-22], Q13 education [23-29], Q14 disadvantaged [30-32]
  await page.evaluate(() => {
    const checkboxes = document.querySelectorAll('input[type="checkbox"]')
    const radios = document.querySelectorAll('input[type="radio"]')
    checkboxes[7].click()  // Q1 Ethnicity: Prefer not to answer
    checkboxes[17].click() // Q2 Gender: Prefer not to answer
    radios[0].click()      // Q3 Sex at birth: Female
    checkboxes[29].click() // Q4 Sexual orientations: Prefer not to answer
    ;[7, 10, 13, 16, 19, 22].forEach(i => radios[i].click()) // Q5-Q10 Disability: Prefer not to answer
    checkboxes[31].click() // Q12 Year of birth: Prefer not to answer
    radios[23].click()     // Q13 Education: Never attended school
    radios[32].click()     // Q14 Disadvantaged: Prefer not to answer
  })
  const submitSel = '[role="button"][aria-label="Submit"]'
  await page.waitForFunction(
    sel => document.querySelector(sel).style.cursor !== 'not-allowed', {}, submitSel)
  await page.click(submitSel)

  await page.waitForSelector('#account-creation-success')
  await expect(page.waitForSelector('h2').then(eh => eh.evaluate(e => e.innerText)))
    .resolves.toBe('Congratulations!')
}, 20e3)

