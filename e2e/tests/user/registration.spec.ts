import CreateAccountPage from '../../app/create-account-page';
import GoogleLoginPage, {selectors} from '../../app/google-login';
import {getCursorValue} from '../../driver/elementHandle-util';
import PuppeteerLaunch from '../../services/puppeteer-launch';
import { inputFieldsValues, institutionAffiliationValues } from './user-registration-fields.js';
require('../../driver/waitFuncs');
require('../../driver/puppeteerExtension');
const faker = require('faker/locale/en_US');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe('User registration tests:', () => {

  let browser;
  let incognitoContext;
  let page;

  beforeAll(async () =>  {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    incognitoContext = await browser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await incognitoContext.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  // TODO add verification
  test('Can register new user', async () => {
    // Load the landing page for login.
    const url = configs.uiBaseUrl + configs.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);

    await loginPage.goto();

    // Click the create account button to start new-user-registration flow.


    const createAccountButton = await loginPage.createAccountButton();
    expect(createAccountButton).toBeTruthy();
    await createAccountButton.click();

    // Step 1: invitation key.
    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.getInvitationKeyInput()
       .then(invitationKeyInput => invitationKeyInput.type(process.env.INVITATION_KEY))
       .then(() => createAccountPage.getSubmitButton())
       .then(submitButton => submitButton.click());

    // add

    // Step 2: terms of service.
    // await page.waitFor(5000);
    const privacyStatementCheckbox = await createAccountPage.getPrivacyStatementCheckbox();
    expect(privacyStatementCheckbox.asElement()).toBeTruthy();
    expect(await privacyStatementCheckbox.getAttribute('disabled')).toBeDefined();

    const termOfUseCheckbox = await createAccountPage.getTermOfUseCheckbox();
    expect(termOfUseCheckbox).toBeTruthy();
    expect(await termOfUseCheckbox.getAttribute('disabled')).toBeDefined();

    expect(await (privacyStatementCheckbox.asAouElement()).getProp('checked')).toBe(false);
    expect(await (termOfUseCheckbox.asAouElement()).getProp('checked')).toBe(false);

    let nextButton = await createAccountPage.getNextButton();
    // Next button should be disabled
    let cursor = await getCursorValue(page, nextButton);
    expect(cursor).toEqual('not-allowed');

    // scroll to last PDF page
    await createAccountPage.scrollToLastPdfPage();
    expect(await privacyStatementCheckbox.getAttribute('disabled')).toBeNull();
    expect(await termOfUseCheckbox.getAttribute('disabled')).toBeNull();

    // check by click on label works
    await page.evaluate(e => e.click(), await createAccountPage.getPrivacyStatementLabel());
    await page.evaluate(e => e.click(), await createAccountPage.getTermOfUseLabel());

    expect(await privacyStatementCheckbox.asAouElement().getProp('checked')).toBe(true);
    expect(await termOfUseCheckbox.asAouElement().getProp('checked')).toBe(true);

    // TODO uncomment after bug fixed https://precisionmedicineinitiative.atlassian.net/browse/RW-4487
    // uncheck
    // await page.evaluate(e => e.click(), await createAccountPage.getTermOfUseLabel());
    // cursor = await getCursorValue(page, nextButton);
    // expect(cursor).toEqual('not-allowed');
    // check on
   // await page.evaluate(e => e.click(), await createAccountPage.getTermOfUseLabel());

    // NEXT button should be enabled
    cursor = await getCursorValue(page, nextButton);
    expect(cursor).toEqual('pointer');
    // await nextButton.click();
    await page.evaluate(e => e.click(), nextButton);

    await page.waitFor(5000);

    const newUserName = await createAccountPage.fillInFormFields(inputFieldsValues);

    nextButton = await createAccountPage.getNextButton();
    // NEXT button should be disabled
    cursor = await getCursorValue(page, nextButton);
    expect(cursor).toEqual('not-allowed');

    await (await createAccountPage.getResearchBackgroundTextarea()).type(faker.lorem.word());
    await (await createAccountPage.getInstitutionNameInput()).type(faker.company.companyName());
    await createAccountPage.selectInstitution(institutionAffiliationValues.EARLY_CAREER_TENURE_TRACK_RESEARCHER);

    await page.waitFor(1000);

    console.log(createAccountPage.getInstitutionDisplayedValue());


    // NEXT button should be enabled
    cursor = await getCursorValue(page, nextButton);
    expect(cursor).toEqual('pointer');

    // await nextButton.click();

    await page.waitFor(15000);
    // Step 4: Demographic survey.

    // Step 5: landing page.

  });

});
