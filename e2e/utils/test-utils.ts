import Checkbox from 'app/element/checkbox';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import HomePage from 'app/page/home-page';
import { ElementType, XPathOptions } from 'app/xpath-options';
import * as fs from 'fs';
import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import WorkspaceCard from 'app/component/workspace-card';
import { PageUrl, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import Navigation, { NavLink } from 'app/component/navigation';
import { makeWorkspaceName } from './str-utils';
import { config } from 'resources/workbench-config';
import { logger } from 'libs/logger';

export async function signOut(page: Page): Promise<void> {
  await page.evaluate(async () => {
    return 'window.setTestAccessTokenOverride(null)';
  });

  await Navigation.navMenu(page, NavLink.SIGN_OUT);
  await page.waitForTimeout(1000);
}

export async function signInWithAccessToken(page: Page, tokenFilename = config.USER_ACCESS_TOKEN_FILE): Promise<void> {
  const token = fs.readFileSync(tokenFilename, 'ascii');
  logger.info('Sign in with access token to Workbench application');
  console.log("Printing access token ", token);
  const homePage = new HomePage(page);
  await homePage.gotoUrl(PageUrl.Home);

  // Once ready, initialize the token on the page (this is stored in local storage).
  // See sign-in.service.ts for details.
  const navigationPromise = page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
  await page.waitForFunction('!!window["setTestAccessTokenOverride"]');
  await page.evaluate(`window.setTestAccessTokenOverride('${token}')`);
  await navigationPromise;

  // Force a page reload; auth will be re-initialized with the token now that
  // localstorage has been updated.
  // Disclaimer: any hard page load may result in truncation of browser console
  // logs; there is some delay between a console.log() execution and capture by
  // Puppeteer. Any console.log() within the above global function, for example,
  // is unlikely to be captured.
  await homePage.gotoUrl(PageUrl.Home);
  await homePage.waitForLoad().catch(async () => {
    await homePage.reloadPage();
  });
}

/**
 * Is there a element located by CSS selector?
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string): Promise<boolean> {
  return !!(await page.$(`${selector}`));
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
  const { cdrVersion = config.DEFAULT_CDR_VERSION_NAME, workspaceName = makeWorkspaceName() } = options;
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
export async function findOrCreateWorkspace(
  page: Page,
  opts: { cdrVersion?: string; workspaceName?: string } = {}
): Promise<string> {
  const { cdrVersion = config.DEFAULT_CDR_VERSION_NAME, workspaceName } = opts;
  // Returns specified workspaceName Workspace card if exists.
  if (workspaceName !== undefined) {
    const cardFound = await findWorkspaceCard(page, workspaceName);
    if (cardFound != null) {
      logger.info(`Found workspace card name: ${workspaceName}`);
      // TODO workspace CDR version is not verified
      await cardFound.clickWorkspaceName();
      return workspaceName; // Found Workspace card matching workspace name
    }
    return createWorkspace(page, { workspaceName, cdrVersion });
  }

  // Find a suitable workspace among existing workspaces with OWNER role and older than 30 minutes.
  const olderWorkspaceCards = await findAllCards(page);
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
  const { cdrVersion = config.DEFAULT_CDR_VERSION_NAME, workspaceName = makeWorkspaceName() } = options;

  let cardFound = await findWorkspaceCard(page, workspaceName);
  if (cardFound !== null) {
    // TODO workspaces CDR version is not verified
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

/**
 * Find a suitable workspace among existing workspaces with OWNER role and older than specified time difference.
 */
export async function findAllCards(page: Page, millisAgo = 1000 * 60 * 30): Promise<WorkspaceCard[]> {
  const existingCards: WorkspaceCard[] = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Owner);
  // Filter to exclude Workspaces younger than 30 minutes.
  const halfHourAgoMillis = Date.now() - millisAgo;
  return Promise.all(
    await asyncFilter(existingCards, async (card) => halfHourAgoMillis > Date.parse(await card.getLastChangedTime()))
  );
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
  if (!regex.exec(date)) {
    return false;
  }
  const d = new Date(date);
  const dNum = d.getTime();
  if (!dNum && dNum !== 0) {
    return false;
  }
  return d.toISOString().slice(0, 10) === date;
}

const asyncFilter = async (arr, predicate) =>
  arr.reduce(async (items, item) => ((await predicate(item)) ? [...(await items), item] : items), []);
