import Checkbox from 'app/element/checkbox';
import Link from 'app/element/link';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import GoogleLoginPage from 'app/page/google-login';
import HomePage, {LABEL_ALIAS} from 'app/page/home-page';
import {XPathOptions, ElementType} from 'app/xpath-options';
import * as fp from 'lodash/fp';
import {JSHandle, Page} from 'puppeteer';
import {waitForText} from 'utils/waits-utils';
import WorkspaceCard from 'app/component/workspace-card';
import {WorkspaceAccessLevel} from 'app/page-identifiers';
import WorkspacesPage from 'app/page/workspaces-page';
import {makeWorkspaceName} from './str-utils';

export async function signIn(page: Page): Promise<void> {
  const loginPage = new GoogleLoginPage(page);
  await loginPage.login();
  // this element exists in DOM after user has logged in
  await page.waitFor(() => document.querySelector('app-signed-in') !== null);
  const homePage = new HomePage(page);
  await homePage.waitForLoad();
}

/**
 * <pre>
 * Wait while the page is loading (spinner is spinning and visible). Waiting stops when spinner stops spinning or when timed out.
 * It usually indicates the page is ready for user interaction.
 * </pre>
 */
export async function waitWhileLoading(page: Page, timeOut: number = 90000): Promise<void> {
  // wait maximum 1 second for either spinner to show up
  const spinSelector = '.spinner, svg';
  let spinner: JSHandle;
  try {
    spinner = await page.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, spinSelector);
  } catch (err) {
    console.info('waitUntilNoSpinner does not find any spin elements.');
  }
  const jValue = await spinner.jsonValue();

  // wait maximum 90 seconds for spinner disappear if spinner existed
  const spinAnimationSelector = 'svg[style*="spin"], .spinner:empty';
  // const startTime = performance.now();
  try {
    if (jValue) {
      await page.waitFor((selector) => {
        return document.querySelectorAll(selector).length === 0;
      }, {timeout: timeOut}, spinAnimationSelector);
      // 1 second to give page time finish rendering
      await page.waitFor(1000);
    }
  } catch (err) {
    throw new Error(err);
  }

}

export async function clickEvalXpath(page: Page, xpathSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.evaluate(
       selector,
       document,
       null,
       XPathResult.FIRST_ORDERED_NODE_TYPE,
       null
    ).singleNodeValue;
    document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
    node.click();
  }, xpathSelector);
}

export async function clickEvalCss(page: Page, cssSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.querySelector(selector);
    node.click();
  }, cssSelector);
}

/**
 * Is there a element located by CSS selector?
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string) {
  return !!(await page.$(`${selector}`));
}

export async function clickRecaptcha(page: Page) {
  const css = '[id="recaptcha-anchor"][role="checkbox"]';
  await page.frames().find(async (frame) => {
    for(const childFrame of frame.childFrames()) {
      const recaptcha = await childFrame.$$(css);
      if (recaptcha.length > 0) {
        await recaptcha[0].click();
        return;
      }
    }
  });
}

export async function newUserRegistrationSelfBypass(page: Page) {
  const selfBypassXpath = '//*[@data-test-id="self-bypass"]';
  await Promise.race([
    page.waitForXPath(selfBypassXpath, {visible: true, timeout: 60000}),
    Link.findByName(page, {name: LABEL_ALIAS.SEE_ALL_WORKSPACES}),
  ]);

  // check to see if it is the Self-Bypass link
  const bypassLink = await page.$x(selfBypassXpath);
  if (bypassLink.length === 0) {
    return;
  }

  // Click Self-Bypass button to continue
  const selfBypass = await page.waitForXPath(`${selfBypassXpath}//div[@role="button"]`, {visible: true});
  await selfBypass.click();
  try {
    await waitWhileLoading(page);
  } catch (timeouterr) {
    // wait more if 60 seconds wait time wasn't enough.
    await waitWhileLoading(page, 120000);
  }
  await waitForText(page, 'Bypass action is complete. Reload the page to continue.', {css: '[data-test-id="self-bypass"]'}, 60000);
  await page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
  await waitWhileLoading(page);
}

/**
 * Perform array of UI actions defined.
 * @param fields
 */
export async function performActions(
   page: Page,
   fields: ({ id: {textOption: XPathOptions, affiliated?: ElementType}; value?: string; selected?: boolean })[]) {
  for (const field of fields) {
    await performAction(page, field.id, field.value, field.selected);
  }
}

/**
 * Perform one UI action.
 * @param {Page} page
 * @param { textOption?: XpathOptions, affiliated?: InputType } identifier
 * @param { string } value Set textbox or textarea value if associated UI element is a Checkbox.
 * @param { boolean } selected to True for select Checkbox or Radiobutton. False to unselect.
 */
export async function performAction(
   page: Page,
   identifier: {textOption: XPathOptions, affiliated?: ElementType}, value?: string, selected?: boolean) {

  switch (identifier.textOption.type.toLowerCase()) {
  case 'radio':
    const radioELement = await RadioButton.findByName(page, identifier.textOption);
    await radioELement.select();
    break;
  case 'checkbox':
    const checkboxElement = await Checkbox.findByName(page, identifier.textOption);
    await checkboxElement.toggle(selected);
    if (value) {
        // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
      identifier.textOption.type = identifier.affiliated;
      await performAction(page, { textOption: identifier.textOption }, value);
    }
    break;
  case 'text':
    const textboxElement = await Textbox.findByName(page, identifier.textOption);
    await textboxElement.type(value, {delay: 0});
    await textboxElement.tabKey();
    break;
  case 'textarea':
    const textareaElement = await Textarea.findByName(page, identifier.textOption);
    await textareaElement.paste(value);
    await textareaElement.tabKey();
    break;
  default:
    throw new Error(`${identifier} is not recognized.`);
  }

}

/**
 * Find an exsting workspace. Create a new workspace if none exists.
 * @param {boolean} createNew Create a new workspace, without regard to any existing workspaces or not.
 */
export async function findWorkspace(page: Page, createNew: boolean = false): Promise<WorkspaceCard> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  let existingWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.OWNER);

  if (existingWorkspaces.length === 0 || createNew) {
    // Create new workspace
    const workspaceName = makeWorkspaceName();
    await workspacesPage.createWorkspace(workspaceName);

    await workspacesPage.load();
    existingWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.OWNER);
  }

  const oneWorkspaceCard = fp.shuffle(existingWorkspaces)[0];
  const workspaceCardName = await oneWorkspaceCard.getWorkspaceName();
  console.log(`Found a workspace: ${workspaceCardName}`);
  return oneWorkspaceCard;
}
