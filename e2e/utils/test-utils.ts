import Checkbox from 'app/element/checkbox';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import GoogleLoginPage from 'app/page/google-login';
import HomePage from 'app/page/home-page';
import { ElementType, XPathOptions } from 'app/xpath-options';
import * as fs from 'fs';
import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import WorkspaceCard from 'app/component/workspace-card';
import { PageUrl, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import Navigation, { NavLink } from 'app/component/navigation';
import { config } from '../resources/workbench-config';
import { makeWorkspaceName } from './str-utils';

export async function signIn(page: Page, userId?: string, passwd?: string): Promise<void> {
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
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36';
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
    case 'radio': {
      const radio = await RadioButton.findByName(page, identifier.textOption);
      await radio.select();
      break;
    }
    case 'checkbox': {
      const checkbox = await Checkbox.findByName(page, identifier.textOption);
      await checkbox.toggle(selected);
      if (value) {
        // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
        identifier.textOption.type = identifier.affiliated;
        await performAction(page, { textOption: identifier.textOption }, value);
      }
      break;
    }
    case 'text': {
      const textbox = await Textbox.findByName(page, identifier.textOption);
      await textbox.type(value, { delay: 20 });
      await textbox.pressTab();
      break;
    }
    case 'textarea': {
      const textarea = await Textarea.findByName(page, identifier.textOption);
      await textarea.paste(value);
      await textarea.pressTab();
      break;
    }
    default:
      throw new Error(`${identifier} is not recognized.`);
  }
}

export async function createWorkspace(
  page: Page,
  cdrVersionName: string = config.defaultCdrVersionName
): Promise<WorkspaceCard> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  const name = makeWorkspaceName();

  await workspacesPage.createWorkspace(name, cdrVersionName);
  console.log(`Created workspace "${name}" with CDR Version "${cdrVersionName}"`);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  return workspaceCard.findCard(name);
}

/**
 * Find a suitable existing workspace older than 30 minutes, or create one if workspace does not exist.
 *
 * TODO: this function does a lot of different things.  refactor and split up according to use cases.
 *
 * If the caller specifies a workspace name and it can be found, return it.
 *
 * If the workspace is not found (or no name is given), search for a workspace where the user
 * has Owner access.
 *
 * If no such workspace exists or the caller specifies alwaysCreate, create a new workspace and return it.
 *
 * Else choose one of the suitable workspaces randomly.
 *
 * @param page
 * @param opts (all are optional)
 *  alwaysCreate - create a new workspace, regardless of whether a suitable workspace exists
 *  workspaceName - return the workspace with this name if it can be found; default behavior otherwise
 */
export async function findOrCreateWorkspace(
  page: Page,
  opts: { alwaysCreate?: boolean; workspaceName?: string } = {}
): Promise<WorkspaceCard> {
  const { alwaysCreate = false, workspaceName } = opts;

  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  // Returns specified workspaceName Workspace card if exists.
  if (workspaceName !== undefined) {
    const cardFound = await workspaceCard.findCard(workspaceName);
    if (cardFound != null) {
      // Workspace card found
      return cardFound;
    }
  }

  // Find all workspaces with OWNER role
  const existingCards = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Owner);

  // Filter to include Workspaces older than 30 minutes
  const halfHourMillisec = 1000 * 60 * 30;
  const now = Date.now();
  const olderWorkspaceCards = [];
  for (const card of existingCards) {
    const workspaceTime = Date.parse(await card.getLastChangedTime());
    const timeDiff = now - workspaceTime;
    if (timeDiff > halfHourMillisec) {
      olderWorkspaceCards.push(card);
    }
  }

  // Create new workspace if existing workspace is zero or alwayCreate is true
  if (alwaysCreate || olderWorkspaceCards.length === 0) {
    const name = workspaceName || makeWorkspaceName();
    await workspacesPage.createWorkspace(name);
    console.log(`Created workspace "${name}"`);
    await workspacesPage.load();
    return workspaceCard.findCard(name);
  }

  // Return one random Workspace card
  const randomCard: WorkspaceCard = fp.shuffle(olderWorkspaceCards).pop() as WorkspaceCard;
  const cardName = await randomCard.getWorkspaceName();
  const lastChangedTime = await randomCard.getLastChangedTime();
  console.log(`Found workspace "${cardName}". Last changed on ${lastChangedTime}`);
  return randomCard;
}

export async function centerPoint(element: ElementHandle): Promise<[number, number]> {
  const box = await element.boundingBox();
  const { x, y, height, width } = box;
  const cx = (x + x + width) / 2;
  const cy = (y + y + height) / 2;
  return [cx, cy];
}

export async function dragDrop(
  page: Page,
  element: ElementHandle,
  destinationPoint: { x: number; y: number }
): Promise<void> {
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
export function isValidDate(date: string): boolean {
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
