import Checkbox from 'app/element/checkbox';
import Link from 'app/element/link';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import GoogleLoginPage from 'app/page/google-login';
import HomePage, { LabelAlias } from 'app/page/home-page';
import { ElementType, XPathOptions } from 'app/xpath-options';
import * as fs from 'fs';
import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { PageUrl, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import Navigation, { NavLink } from 'app/component/navigation';
import { makeWorkspaceName } from './str-utils';
import { config } from 'resources/workbench-config';
import { logger } from 'libs/logger';

export async function signIn(page: Page, userId?: string, passwd?: string): Promise<void> {
  logger.info('Sign in with Google to Workbench application');
  const loginPage = new GoogleLoginPage(page);
  await loginPage.login(userId, passwd);
  // This element exists in DOM after user has logged in. But it could takes a while.
  await page.waitForFunction(() => !!document.querySelector('app-signed-in'), { timeout: 30000 });
  const homePage = new HomePage(page);
  await homePage.waitForLoad();
}

/**
 * Login in new Incognito page.
 * @param {string} userId
 * @param {string} passwd
 *
 * @deprecated use signInWithAccessToken, rm with RW-5580
 */
export async function signInAs(userId: string, passwd: string, opts: { reset?: boolean } = {}): Promise<Page> {
  const { reset = true } = opts;
  if (reset) {
    await jestPuppeteer.resetBrowser();
  }
  const newPage = await browser.createIncognitoBrowserContext().then((context) => context.newPage());
  const userAgent =
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) ' +
    'Chrome/81.0.4044.138 Safari/537.36';
  await newPage.setUserAgent(userAgent);
  newPage.setDefaultNavigationTimeout(90000);
  await signIn(newPage, userId, passwd);
  return newPage;
}

export async function signOut(page: Page): Promise<void> {
  await page.evaluate('window.setTestAccessTokenOverride(null)');

  await Navigation.navMenu(page, NavLink.SIGN_OUT);
  await page.waitForTimeout(1000);
}

export async function signInWithAccessToken(page: Page, tokenFilename = config.userAccessTokenFilename): Promise<void> {
  const token = fs.readFileSync(tokenFilename, 'ascii');
  logger.info('Sign in with access token to Workbench application');
  const homePage = new HomePage(page);
  await homePage.gotoUrl(PageUrl.Home.toString());

  // See sign-in.service.ts.
  await page.evaluate(`window.setTestAccessTokenOverride('${token}')`);

  await homePage.gotoUrl(PageUrl.Home.toString());
  await homePage.waitForLoad();
}

/**
 * <pre>
 * Wait while the page is loading (spinner is spinning and visible). Waiting stops when spinner stops spinning or when timed out.
 * It usually indicates the page is ready for user interaction.
 * </pre>
 */
export async function waitWhileLoading(page: Page, timeOut?: number): Promise<void> {
  const notBlankPageSelector = '[data-test-id="sign-in-container"], title:not(empty), div.spinner, svg[viewBox]';
  const spinElementsSelector = '[style*="running spin"], .spinner:empty, [style*="running rotation"]';

  await Promise.race([
    // To prevent checking on blank page, wait for elements exist in DOM.
    page.waitForSelector(notBlankPageSelector),
    page.waitForSelector(spinElementsSelector)
  ]);

  // Wait for spinners stop and gone.
  await page.waitForFunction(
    (css) => {
      const elements = document.querySelectorAll(css);
      return elements && elements.length === 0;
    },
    { polling: 'mutation', timeout: timeOut },
    spinElementsSelector
  );
}

/**
 * Is there a element located by CSS selector?
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string): Promise<boolean> {
  return !!(await page.$(`${selector}`));
}

export async function newUserRegistrationSelfBypass(page: Page): Promise<void> {
  const selfBypassXpath = '//*[@data-test-id="self-bypass"]';
  await Promise.race([
    page.waitForXPath(selfBypassXpath, { visible: true, timeout: 60000 }),
    Link.findByName(page, { name: LabelAlias.SeeAllWorkspaces })
  ]);

  // check to see if it is the Self-Bypass link
  const bypassLink = await page.$x(selfBypassXpath);
  if (bypassLink.length === 0) {
    return;
  }

  // Click Self-Bypass button to continue
  const selfBypass = await page.waitForXPath(`${selfBypassXpath}//div[@role="button"]`, { visible: true });
  await selfBypass.click();
  try {
    await waitWhileLoading(page);
  } catch (timeouterr) {
    // wait more if 60 seconds wait time wasn't enough.
    await waitWhileLoading(page);
  }
  await waitForText(page, 'Bypass action is complete. Reload the page to continue.', {
    css: '[data-test-id="self-bypass"]'
  });
  await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
  await waitWhileLoading(page);
}

/**
 * Perform array of UI actions defined.
 * @param fields
 */
export async function performActions(
  page: Page,
  fields: { id: { textOption: XPathOptions; affiliated?: ElementType }; value?: string; selected?: boolean }[]
): Promise<void> {
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
  identifier: { textOption: XPathOptions; affiliated?: ElementType },
  value?: string,
  selected?: boolean
): Promise<void> {
  switch (identifier.textOption.type.toLowerCase()) {
    case 'radio':
      await RadioButton.findByName(page, identifier.textOption).select();
      break;
    case 'checkbox':
      await Checkbox.findByName(page, identifier.textOption).toggle(selected);
      if (value) {
        // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
        identifier.textOption.type = identifier.affiliated;
        await performAction(page, { textOption: identifier.textOption }, value);
      }
      break;
    case 'text':
      await Textbox.findByName(page, identifier.textOption)
        .type(value, { delay: 0 })
        .then((textbox) => textbox.pressTab());
      break;
    case 'textarea':
      {
        const textareaElement = Textarea.findByName(page, identifier.textOption);
        await textareaElement.paste(value);
        await textareaElement.pressTab();
      }
      break;
    default:
      throw new Error('identifier not recognized.');
  }
}

/**
 * Create new workspace in All Workspaces page. Returns Workspace card and workspace name object.
 * @param page
 * @param options
 */
export async function createWorkspace(
  page: Page,
  options: { cdrVersion?: string; workspaceName?: string } = {}
): Promise<string> {
  const { cdrVersion = config.defaultCdrVersionName, workspaceName = makeWorkspaceName() } = options;
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();
  await workspacesPage.createWorkspace(workspaceName, cdrVersion);
  return workspaceName;
}

/**
 * Find a suitable existing workspace older than 30 minutes, or create one if workspace does not exist.
 *
 * If the caller specifies a workspace name and it can be found, return it.
 *
 * If the workspace is not found (or no name is given), search for a workspace where the user
 * has Owner access.
 *
 * If no such workspace exists or workspace name is not undefined, create a new workspace and return it.
 *  Else, choose one of the suitable workspaces randomly.
 *
 * @param page
 * @param opts (all are optional)
 *  workspaceName - return the workspace with this name if it can be found. Otherwise create workspace with this name.
 */
export async function findOrCreateWorkspace(page: Page, opts: { workspaceName?: string } = {}): Promise<string> {
  const { workspaceName } = opts;

  // Returns specified workspaceName Workspace card if exists.
  if (workspaceName !== undefined) {
    const cardFound = await findWorkspaceCard(page, workspaceName);
    if (cardFound != null) {
      await cardFound.clickWorkspaceName();
      return workspaceName; // Found Workspace card matching workspace name
    } else {
      return await createWorkspace(page, { workspaceName });
    }
  }

  // Find a suitable workspace among existing workspaces with OWNER role and older than 30 minutes.
  const existingCards = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Owner);
  // Filter to include Workspaces older than 30 minutes
  const halfHourMilliSec = 1000 * 60 * 30;
  const now = Date.now();
  const olderWorkspaceCards = [];
  for (const card of existingCards) {
    const workspaceTime = Date.parse(await card.getLastChangedTime());
    const timeDiff = now - workspaceTime;
    if (timeDiff > halfHourMilliSec) {
      olderWorkspaceCards.push(card);
    }
  }

  // Create new workspace if did not find a suitable workspace.
  if (olderWorkspaceCards.length === 0) {
    return await createWorkspace(page, { workspaceName });
  }

  // Return one random Workspace card
  const randomCard: WorkspaceCard = fp.shuffle(olderWorkspaceCards).pop();
  const cardName = await randomCard.getWorkspaceName();
  const lastChangedTime = await randomCard.getLastChangedTime();
  logger.info(`Found workspace card: "${cardName}". Last changed on ${lastChangedTime}`);
  await randomCard.clickWorkspaceName();
  return cardName;
}

/**
 * Find Workspace card matching workspace name.
 * @param page
 * @param workspaceName
 */
export async function findWorkspaceCard(page: Page, workspaceName: string): Promise<WorkspaceCard | null> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();
  const workspaceCard = new WorkspaceCard(page);
  return workspaceCard.findCard(workspaceName);
}

/**
 * Find Workspace card matching workspace name. If not exist, create new workspace.
 * @param page
 * @param options
 * @return WorkspaceCard
 */
export async function findOrCreateWorkspaceCard(
  page: Page,
  options: { cdrVersion?: string; workspaceName?: string } = {}
): Promise<WorkspaceCard> {
  const { cdrVersion = config.defaultCdrVersionName, workspaceName = makeWorkspaceName() } = options;

  let cardFound = await findWorkspaceCard(page, workspaceName);
  if (cardFound !== null) {
    logger.info(`Found Workspace card name: "${workspaceName}"`);
    return cardFound;
  } else {
    logger.info(`Not finding workspace card name: ${workspaceName}`);
  }

  await createWorkspace(page, { workspaceName, cdrVersion });

  cardFound = await findWorkspaceCard(page, workspaceName);
  if (cardFound === null) {
    throw new Error(`Failed finding Workspace card name: ${workspaceName}`);
  }
  logger.info(`Found Workspace card name: "${workspaceName}"`);
  return cardFound;
}

export async function centerPoint(element: ElementHandle): Promise<[number, number]> {
  const box = await element.boundingBox();
  const { x, y, height, width } = box;
  const cx = (x + x + width) / 2;
  const cy = (y + y + height) / 2;
  return [cx, cy];
}

export async function dragDrop(page: Page, element: ElementHandle, destinationPoint: { x; y }): Promise<void> {
  const [x0, y0] = await centerPoint(element);
  const { x, y } = destinationPoint;
  const mouse = page.mouse;
  await mouse.move(x0, y0);
  await page.waitForTimeout(100);
  await mouse.down();
  await page.waitForTimeout(100);
  await mouse.move(x, y, { steps: 10 });
  await page.waitForTimeout(100);
  await mouse.up();
  await page.waitForTimeout(1000);
}

/**
 * Validate a date string.
 * @param {string} date
 */
// See: https://stackoverflow.com/questions/18758772/how-do-i-validate-a-date-in-this-format-yyyy-mm-dd-using-jquery
export function isValidDate(date: string) {
  const regex = /^\d{4}-\d{2}-\d{2}$/;
  if (!date.match(regex)) {
    return false;
  }
  const d = new Date(date);
  const dNum = d.getTime();
  if (!dNum && dNum !== 0) {
    return false;
  }
  return d.toISOString().slice(0, 10) === date;
}
