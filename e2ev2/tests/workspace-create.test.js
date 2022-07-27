const fsp = require('fs').promises
const config = require('../src/config')
const impersonate = require('../src/impersonate')
const tu = require('../src/test-utils')
const utils = require('../src/utils')

const browserTest = tu.browserTest(__filename)

const workspaceCreationTimeoutMs = 30e3
const workspaceDeletionTimeoutMs = 10e3
browserTest('create a workspace', async browser => {
  const page = browser.initialPage
  await page.goto(config.urlRoot(), {waitUtil: ['domcontentloaded']})
  await page.goto(config.urlRoot())
  await tu.impersonateUser(page, config.usernames[0])
  const createWorkspaceLink = await page.waitForSelector('clr-icon[shape="plus-circle"]')
  await createWorkspaceLink.click()
  expect(await page.waitForSelector('title').then(eh => eh.evaluate(n => n.innerText)))
    .toContain('Create Workspace |')
  // Workspace creation isn't really available until billing accounts have been fetched.
  await page.waitForSelector('[data-test-id="billing-dropdown-div"]')
  const createButton = await page.waitForFunction(
    () => [...document.querySelectorAll('[role="button"]')]
      .filter(n => n.innerText.toLowerCase() === 'create workspace')[0])
  expect(createButton.evaluate(n => n.style.cursor)).resolves.toBe('not-allowed')
  const wsName = `test-ws-share-${utils.denseDateTime()}`
  await page.type('[placeholder="Workspace Name"]', wsName)
  await page.type('#education-purpose', ' ') // ???!!!
  await page.type('#intendedStudyText', 'Does this work?')
  await page.type('#scientificApproachText', 'Automated regression testing.')
  await page.type('#anticipatedFindingsText', 'What, if anything, is broken.')
  expect(await page.waitForSelector('[data-test-id="otherDisseminateResearch-text"]')
    .then(eh => eh.evaluate(e => e.disabled)))
    .toBe(true)
  // Why doesn't this work?
  // await page.click('input[data-test-id="OTHER-checkbox"]')
  await page.waitForSelector('input[data-test-id="OTHER-checkbox"]')
    .then(eh => eh.evaluate(e => e.click()))
  expect(await page.waitForSelector('[data-test-id="otherDisseminateResearch-text"]')
    .then(eh => eh.evaluate(e => e.disabled)))
    .toBe(false)
  await page.type('textarea[data-test-id="otherDisseminateResearch-text"]', 'Team test reports.')
  await page.evaluate(() =>
    Array.from(document.querySelectorAll('input[type="checkbox"]'))[17].click()) // no
  await page.click('input[type="radio"][data-test-id="specific-population-no"]')
  await page.click('div > div + div > input[type="radio"][name="reviewRequested"]')
  expect(await createButton.evaluate(n => n.style.cursor)).toBe('pointer')
  await createButton.click()
  await page.click('[role="dialog"] [role="button"] + [role="button"]')
  // Allow 30 seconds for workspace creation.
  const workspacePageTitleLink =
    await page.waitForSelector('a[href="/workspaces"] + span + div > a',
    {timeout: workspaceCreationTimeoutMs})
  expect(await workspacePageTitleLink.evaluate(e => e.innerText)).toBe(wsName)
  await page.click('[data-test-id="workspace-menu-button"]')
  // side menu pops up
  await page.click('div#popup-root clr-icon[shape="trash"]')
  // modal confirmation pops up
  const confirmButton =
    await page.waitForSelector('[role="dialog"] [role="button"] + [role="button"]')
  expect(await confirmButton.evaluate(n => n.style.cursor)).toBe('not-allowed')
  await page.type('[role="dialog"] input[placeholder="type DELETE to confirm"]', 'delete')
  expect(await confirmButton.evaluate(n => n.style.cursor)).toBe('pointer')
  await confirmButton.click()
  // automatic navigation to Workspaces page
  expect(await page.waitForSelector(
    '[data-test-id="signed-in"] > div:nth-child(2) h3', {timeout: workspaceDeletionTimeoutMs}
    ).then(eh => eh.evaluate(e => e.innerText)))
    .toBe('Workspaces')
}, workspaceCreationTimeoutMs + workspaceDeletionTimeoutMs + 10e3)

