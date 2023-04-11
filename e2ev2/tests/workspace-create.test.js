const fsp = require('fs').promises
const config = require('../src/config')
const impersonate = require('../src/impersonate')
const tu = require('../src/test-utils')
const u = require('../src/utils')

const browserTest = tu.browserTest(__filename)

const workspaceCreationTimeoutMs = 30e3
const workspaceDeletionTimeoutMs = 10e3
browserTest('create a workspace', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await page.goto(config.urlRoot())

  // Workspace creation isn't really available until billing accounts have been fetched.
  const [baEventPromise] = await tu.promiseWindowEvent(page, 'billing-accounts-loaded')
  await Promise.all([baEventPromise, tu.jsClick(page, '[aria-label="Create Workspace"]')])

  await expect(page.waitForSelector('title').then(eh => eh.evaluate(n => n.innerText)))
    .resolves.toContain('Create Workspace |')
  await page.waitForSelector('#billing-dropdown-container')
  const createButton = await page.waitForSelector('[role="button"][aria-label="Create Workspace"]')
  await expect(createButton.evaluate(n => n.style.cursor)).resolves.toBe('not-allowed')
  const wsName = `test-ws-share-202207311932`
  await page.type('[placeholder="Workspace Name"]', wsName)
  await page.type('#education-purpose', ' ') // ???!!!
  await page.type('#intendedStudyText', 'Does this work?')
  await page.type('#scientificApproachText', 'Automated regression testing.')
  await page.type('#anticipatedFindingsText', 'What, if anything, is broken.')
  await expect(page.waitForSelector('[data-test-id="otherDisseminateResearch-text"]')
    .then(eh => eh.evaluate(e => e.disabled)))
    .resolves.toBe(true)
  await tu.jsClick(page, 'input[data-test-id="OTHER-checkbox"]')
  await expect(page.waitForSelector('[data-test-id="otherDisseminateResearch-text"]')
    .then(eh => eh.evaluate(e => e.disabled)))
    .resolves.toBe(false)
  await page.type('textarea[data-test-id="otherDisseminateResearch-text"]', 'Team test reports.')
  await tu.jsClick(page,
    'input[aria-label="None of these statements apply to this research project"]')
  await page.click('input[type="radio"][data-test-id="specific-population-no"]')
  await page.click('input[aria-label="Do Not Request Review"]')
  await expect(createButton.evaluate(n => n.style.cursor)).resolves.toBe('pointer')
  await createButton.click()
  await page.click('[role="button"][aria-label="Confirm"]')

  // Allow 30 seconds for workspace creation.
  await page.waitForSelector('#workspace-top-nav-bar', {timeout: workspaceCreationTimeoutMs})
  const workspacePageTitleLink =
    await page.waitForSelector('a[href="/workspaces"] + span + div > a')
  await expect(workspacePageTitleLink.evaluate(e => e.innerText)).resolves.toBe(wsName)
  await page.waitForSelector('[aria-label="Open Actions Menu"]').then(eh => eh.click())
  // side menu pops up
  await page.click('[aria-label="Delete"]')
  // modal confirmation pops up
  const confirmButton =
    await page.waitForSelector('[role="button"][aria-label="Confirm Delete"]')
  await expect(confirmButton.evaluate(n => n.style.cursor)).resolves.toBe('not-allowed')
  await page.waitForSelector('[role="dialog"] input[placeholder="type DELETE to confirm"]')
    .then(eh => eh.type('delete'))
  await expect(confirmButton.evaluate(n => n.style.cursor)).resolves.toBe('pointer')
  await confirmButton.click()

  // automatic navigation to Workspaces page
  await page.waitForSelector('#workspaces-list')
}, workspaceCreationTimeoutMs + workspaceDeletionTimeoutMs + 10e3)

