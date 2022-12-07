const config = require('../src/config')
const tu = require('../src/test-utils')

const browserTest = tu.browserTest(__filename)

browserTest('take the new user satisfaction survey via the relevant notification', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await page.goto(config.urlRoot(), {waitUntil: 'networkidle0'})

  const surveyNotification = await page.waitForSelector('[data-test-id="new-user-satisfaction-survey-notification"]');
  const launchSurveyButton = await surveyNotification.waitForSelector('[role="button"]');
  await launchSurveyButton.click();

  let surveyModal = await page.waitForSelector('[data-test-id="new-user-satisfaction-survey-modal"]');

  const verySatisfiedButton = await surveyModal.waitForSelector('[value="VERY_SATISFIED"]');
  await verySatisfiedButton.click();

  const additionalInfoInput = await surveyModal.waitForSelector('#new-user-satisfaction-survey-additional-info');
  await additionalInfoInput.type('I love the workbench!');

  const submitButton = await surveyModal.waitForSelector('[role="button"]:nth-of-type(2)');
  await submitButton.click();

  // wait for the submit request to succeed
  await new Promise((r) => setTimeout(r, 100));

  surveyModal = await page.$('[data-test-id="new-user-satisfaction-survey-modal"]');
  expect(surveyModal).toBeNull();

  // todo: Verify the notification goes away. Not possible without a different /profile handler that sets newUserSatisfactionSurveyEligibility to false.
}, 10e3);

