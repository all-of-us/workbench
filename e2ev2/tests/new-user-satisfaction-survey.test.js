const config = require('../src/config')
const tu = require('../src/test-utils')
const utils = require('../src/utils')

const browserTest = tu.browserTest(__filename)

const completeNewUserSatisfactionSurvey = async (page) => {
  const surveyModal = await page.waitForSelector('[aria-modal="true"]');
  await surveyModal.waitForSelector('[value="VERY_SATISFIED"]').then(b => b.click());
  await surveyModal.waitForSelector('#new-user-satisfaction-survey-additional-info').then(i => i.type('I love the workbench!'));
  const submitButton = await surveyModal.waitForSelector('[aria-label="submit"]');

  const [submitEvent] = await tu.promiseWindowEvent(page, 'new-user-satisfaction-survey-submitted');
  await Promise.all([submitEvent, submitButton.click()]);
}

browserTest('take the new user satisfaction survey via the relevant notification', async browser => {
  const page = browser.initialPage
  await tu.useApiProxy(page)
  await tu.fakeSignIn(page)
  await page.goto(config.urlRoot())
  await utils.dismissLeoAuthErrorModal(page);

  const surveyNotification = await page.waitForSelector('[data-test-id="new-user-satisfaction-survey-notification"]');
  await surveyNotification.waitForSelector('[aria-label="take satisfaction survey"]').then(b => b.click());

  await completeNewUserSatisfactionSurvey(page);

  // Ideally, we would check that `surveyModal` is now hidden, but as far as I know puppeteer does not offer a way to check that
  await page.waitForSelector('[aria-modal="true"]', { hidden: true });
}, 10e3);

browserTest('take the new user satisfaction survey via an email link', async browser => {
  const page = browser.initialPage
  const surveyCode = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
  await tu.useApiProxy(page)
  await page.goto(`${config.urlRoot()}?surveyCode=${surveyCode}`)

  await completeNewUserSatisfactionSurvey(page);
}, 10e3);

