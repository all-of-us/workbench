const config = require('../src/config')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

browserTest('take the new user satisfaction survey via the relevant notification', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await page.goto(config.urlRoot(), {waitUntil: 'networkidle0'})

  const surveyNotification = await page.waitForSelector('[data-test-id="new-user-satisfaction-survey-notification"]');
  await surveyNotification.waitForSelector('[aria-label="take satisfaction survey"]').then(b => b.click());

  let surveyModal = await page.waitForSelector('[aria-modal="true"]');
  await surveyModal.waitForSelector('[value="VERY_SATISFIED"]').then(b => b.click());
  await surveyModal.waitForSelector('#new-user-satisfaction-survey-additional-info').then(i => i.type('I love the workbench!'));
  const submitButton = await surveyModal.waitForSelector('[aria-label="submit"]');

  const [submitEvent] = await tu.promiseWindowEvent(page, 'new-user-satisfaction-survey-submitted');
  await Promise.all([submitEvent, submitButton.click()]);

  surveyModal = await page.$('[aria-modal="true"]');
  expect(surveyModal).toBeNull();

  // todo: Verify the notification goes away. Not possible without a different /profile handler that sets newUserSatisfactionSurveyEligibility to false.
}, 10e3);

