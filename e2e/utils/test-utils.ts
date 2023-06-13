import Checkbox from 'app/element/checkbox';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import HomePage from 'app/page/home-page';
import { ElementType, XPathOptions } from 'app/xpath-options';
import * as fs from 'fs';
import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import WorkspaceCard from 'app/component/card/workspace-card';
import {
  AccessTierDisplayNames,
  Cohorts,
  ConceptSets,
  Language,
  PageUrl,
  ResourceCard,
  Tabs,
  WorkspaceAccessLevel
} from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import { isBlank, makeWorkspaceName } from './str-utils';
import { config } from 'resources/workbench-config';
import { logger } from 'libs/logger';
import { authenticator } from 'otplib';
import AuthenticatedPage from 'app/page/authenticated-page';
import Tab from 'app/element/tab';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import DataResourceCard from 'app/component/card/data-resource-card';
import DatasetBuildPage from 'app/page/dataset-build-page';
import CohortBuildPage from 'app/page/cohort-build-page';
import { Ethnicity, GenderIdentity } from 'app/page/cohort-participants-group';
import CohortActionsPage from 'app/page/cohort-actions-page';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

// Resolve typescript error: TS2339: Property 'setTestAccessTokenOverride' does not exist on type 'Window & typeof globalThis'.
declare const window: Window &
  typeof globalThis & {
    setTestAccessTokenOverride: any;
  };

export async function signInWithAccessToken(
  page: Page,
  userEmail = config.USER_NAME,
  opts: { postSignInPage?: AuthenticatedPage; waitForLoad?: boolean } = {}
): Promise<void> {
  const { postSignInPage = new HomePage(page), waitForLoad = true } = opts;
  const tokenLocation = `signin-tokens/${userEmail}.json`;
  // Keep file naming convention synchronized with generate-impersonated-user-tokens
  const tokenJson = fs.readFileSync(tokenLocation, 'ascii');
  if (isBlank(tokenJson)) {
    throw Error(`Token found at ${tokenLocation} is blank`);
  }
  const { token } = JSON.parse(tokenJson);

  logger.info('Sign in with access token to Workbench application');
  const homePage = new HomePage(page);
  await homePage.loadPage({ url: PageUrl.Home });

  // Once ready, initialize the token on the page (this is stored in local storage).
  // See sign-in.service.ts for details.

  await page.waitForFunction('!!window["setTestAccessTokenOverride"]');
  await page.evaluate((token) => {
    window.setTestAccessTokenOverride(token);
  }, token);

  // Force a page reload; auth will be re-initialized with the token now that
  // localstorage has been updated.
  // Disclaimer: any hard page load may result in truncation of browser console
  // logs; there is some delay between a console.log() execution and capture by
  // Puppeteer. Any console.log() within the above global function, for example,
  // is unlikely to be captured.
  await homePage.loadPage({ reload: true });
  await homePage.loadPage({ url: PageUrl.Home });
  // normally the user is routed to the homepage after sign-in, so that's the default here.
  // tests can override this.
  if (waitForLoad) {
    await postSignInPage.waitForLoad();
  }
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
 * @param page
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
  {
    workspaceName = makeWorkspaceName(),
    cdrVersionName = config.DEFAULT_CDR_VERSION_NAME,
    dataAccessTier = AccessTierDisplayNames.Registered
  } = {}
): Promise<string> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();
  await workspacesPage.createWorkspace(workspaceName, { cdrVersionName, dataAccessTier });
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
  opts: {
    cdrVersion?: string;
    workspaceName?: string;
    dataAccessTier?: AccessTierDisplayNames;
    openDataPage?: boolean;
  } = {}
): Promise<string> {
  const { workspaceName, cdrVersion, dataAccessTier, openDataPage = true } = opts;
  // Returns specified workspaceName Workspace card if exists.
  if (workspaceName !== undefined) {
    const card = await findWorkspaceCard(page, workspaceName, 2000);
    if (card != null) {
      // TODO workspace CDR version and Data Access Tier are not verified
      return openDataPage ? card.clickName({ pageExpected: new WorkspaceDataPage(page) }) : workspaceName;
    }
    return createWorkspace(page, { workspaceName, cdrVersionName: cdrVersion, dataAccessTier });
  }

  // Find a suitable workspace among existing workspaces with OWNER role and older than 30 minutes.
  const olderWorkspaceCards = await findAllCards(page);
  // Create new workspace if did not find a suitable workspace.
  if (olderWorkspaceCards.length === 0) {
    return await createWorkspace(page, { workspaceName });
  }

  // Return one random Workspace card
  const randomCard: WorkspaceCard = fp.shuffle(olderWorkspaceCards).pop();
  const cardName = await randomCard.getName();
  const lastChangedTime = await randomCard.getLastChangedTime();
  logger.info(`Found workspace card: "${cardName}". Last changed on ${lastChangedTime}`);
  await randomCard.clickName();
  return cardName;
}

/**
 * Find Workspace card matching workspace name.
 * @param page
 * @param workspaceName
 * @param timeout
 */
export async function findWorkspaceCard(
  page: Page,
  workspaceName: string,
  timeout = 30000
): Promise<WorkspaceCard | null> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  return workspaceCard.findCard({ name: workspaceName, timeout });
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

  let cardFound = await findWorkspaceCard(page, workspaceName, 2000);
  if (cardFound !== null) {
    // TODO workspaces CDR version is not verified
    logger.info(`Found Workspace card name: "${workspaceName}"`);
    return cardFound;
  } else {
    logger.info(`Not finding workspace card name: ${workspaceName}`);
  }

  await createWorkspace(page, { workspaceName, cdrVersionName: cdrVersion });

  cardFound = await findWorkspaceCard(page, workspaceName);
  if (cardFound === null) {
    throw new Error(`FAIL: Failed to find Workspace card with name: ${workspaceName}`);
  }
  logger.info(`Found Workspace card name: "${workspaceName}"`);
  return cardFound;
}

/**
 * Find a suitable workspace among existing workspaces with OWNER role and older than specified time difference.
 */
export async function findAllCards(page: Page, millisAgo = 1000 * 60 * 30): Promise<WorkspaceCard[]> {
  const workspaceCard = new WorkspaceCard(page);
  const existingCards: WorkspaceCard[] = await workspaceCard.findAllCards({
    accessLevel: WorkspaceAccessLevel.Owner
  });

  // Filter to exclude Workspaces younger than 30 minutes.
  const halfHourAgoMillis = Date.now() - millisAgo;
  return Promise.all(
    await asyncFilter(
      existingCards,
      async (card: WorkspaceCard) => halfHourAgoMillis > Date.parse(await card.getLastChangedTime())
    )
  );
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

export const asyncFilter = async (arr, predicate) =>
  arr.reduce(async (items, item) => ((await predicate(item)) ? [...(await items), item] : items), []);

/**
 * Generates a two factor auth code by given secret.
 */
export function generate2FACode(secret: string): string {
  return authenticator.generate(secret);
}

/**
 * Click tab to open a page.
 * @param page {Page}
 * @param tabName: Tab name.
 * @param pageExpected: Page expected to load.
 */
export async function openTab<T extends AuthenticatedPage>(page: Page, tabName: Tabs, pageExpected?: T): Promise<void> {
  const tab = new Tab(page, tabName);
  await tab.click();
  await tab.waitUntilSelected();
  if (pageExpected) {
    await tab.waitFor(pageExpected);
  }
}

// Create a simple dataset. Returns dataset name.
export async function createDataset(
  page: Page,
  opts: { name?: string; cohorts?: string[]; conceptSets?: string[]; returnToDataPage?: boolean } = {}
): Promise<string> {
  const {
    name,
    cohorts = [Cohorts.AllParticipants],
    conceptSets = [ConceptSets.Demographics],
    returnToDataPage = true
  } = opts;

  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();

  const buildPage = await dataPage.clickAddDatasetButton();

  await buildPage.selectCohorts(cohorts);
  await buildPage.selectConceptSets(conceptSets);

  const createModal = await buildPage.clickCreateButton();
  const dataSetName = await createModal.create(name);
  await buildPage.waitForLoad();

  if (!returnToDataPage) {
    return dataSetName;
  }

  await openTab(page, Tabs.Data, dataPage);
  await dataPage.waitForLoad();
  return dataSetName;
}

// Find an existing dataset. Returns dataset name.
export async function findDataset(
  page: Page,
  opts: { name?: string; openEditPage?: boolean } = {}
): Promise<string | null> {
  const { name, openEditPage = false } = opts;

  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();

  await openTab(page, Tabs.Datasets, dataPage);
  const datasetCard = name
    ? await new DataResourceCard(page).findCard({ name, cardType: ResourceCard.Dataset })
    : await new DataResourceCard(page).findAnyCard(ResourceCard.Dataset);

  if (datasetCard !== null) {
    if (openEditPage) {
      const resourceName = await datasetCard.clickName();
      await new DatasetBuildPage(page).waitForLoad();
      return resourceName;
    }
    return name ? name : await datasetCard.getName();
  }
  return null;
}

// Find an existing dataset or create a dataset.
export async function findOrCreateDataset(
  page: Page,
  opts: { cohortNames?: string[]; openEditPage?: boolean } = {}
): Promise<string> {
  const { cohortNames = [Cohorts.AllParticipants], openEditPage } = opts;
  const dataset = await findDataset(page, { openEditPage });
  if (dataset) {
    return dataset;
  }
  return createDataset(page, { cohorts: cohortNames, returnToDataPage: !openEditPage });
}

// Find an existing Cohort. Returns cohort name.
export async function findCohort(page: Page, opts: { name?: string; openEditPage?: boolean } = {}): Promise<string> {
  const { name, openEditPage = false } = opts;

  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();

  const cohortCard = name
    ? await new DataResourceCard(page).findCard({ name, cardType: ResourceCard.Cohort })
    : await new DataResourceCard(page).findAnyCard(ResourceCard.Cohort);

  if (cohortCard !== null) {
    if (openEditPage) {
      const resourceName = await cohortCard.clickName();
      await new CohortBuildPage(page).waitForLoad();
      return resourceName;
    }
    return name ? name : await cohortCard.getName();
  }
  return null;
}

// Create a simple cohort. Returns cohort name.
export async function createCohort(
  page: Page,
  opts: { name?: string; returnToDataPage?: boolean } = {}
): Promise<string> {
  const { name, returnToDataPage = true } = opts;

  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();

  const cohortBuildPage = await dataPage.clickAddCohortsButton();

  // Include Participants Group 1: Add Criteria: Ethnicity.
  const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
  await group1.includeEthnicity([Ethnicity.Skip, Ethnicity.PreferNotToAnswer]);
  await group1.includeGenderIdentity([GenderIdentity.MAN, GenderIdentity.WOMAN]);

  const cohortName = await cohortBuildPage.createCohort(name);

  const cohortActionsPage = new CohortActionsPage(page);
  await cohortActionsPage.waitForLoad();

  if (!returnToDataPage) {
    return cohortName;
  }

  await openTab(page, Tabs.Data, dataPage);
  await dataPage.waitForLoad();
  return cohortName;
}

// Find an existing cohort or create a cohort.
export async function findOrCreateCohort(page: Page, opts: { returnToDataPage?: boolean } = {}): Promise<string> {
  const { returnToDataPage } = opts;
  const cohort = await findCohort(page);
  return cohort ? cohort : createCohort(page, { returnToDataPage });
}

// create notebook - function used for workspace admin test
export async function createDatasetNotebook(page: Page, pyNotebookName: string): Promise<NotebookPreviewPage> {
  await findOrCreateDataset(page, { openEditPage: true });

  const datasetBuildPage = new DatasetBuildPage(page);
  const exportModal = await datasetBuildPage.clickAnalyzeButton();

  await exportModal.enterNotebookName(pyNotebookName);
  await exportModal.pickLanguage(Language.Python);
  await exportModal.clickExportButton();
  const notebookPreviewPage = new NotebookPreviewPage(page);
  return await notebookPreviewPage.waitForLoad();
}

export function parseForNumericalStrings(text: string): RegExpMatchArray | null {
  return text.match(/(\d|\.)+/g);
}

// Find and delete the workspace
export async function cleanupWorkspace(page: Page, workspaceName: string) {
  const workspaceCard = await findWorkspaceCard(page, workspaceName);
  if (workspaceCard != null) {
    try {
      await workspaceCard.delete();
    } catch (e) {
      logger.info(`Workspace "${workspaceName}" could not be deleted`);
    }
  }
}
