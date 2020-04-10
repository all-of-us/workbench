import WorkspacesPage from 'app/workspaces-page';
import {signIn} from 'tests/app';
import {workspaceAccessLevel, workspaceAction} from 'util/enums';
import DataPage from 'app/data-page';
import WorkspaceCard from 'app/workspace-card';

const fp = require('lodash/fp');


describe('Clone workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  describe('From "Your Workspaces" page using Workspace card ellipsis menu', () => {

    test('User can clone a workspace in which user is OWNER', async () => {
      const workspacesPage = new WorkspacesPage(page);
      await workspacesPage.load();

      // choose one workspace on "Your Workspaces" page for clone from
      const workspaceCard = new WorkspaceCard(page);
      const retrievedWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(workspaceAccessLevel.OWNER);
      const aWorkspaceCard = fp.shuffle(retrievedWorkspaces)[0];
      await aWorkspaceCard.asElementHandle().hover();
      // click on Ellipsis "Duplicate"
      await aWorkspaceCard.duplicate();

      // fill out Workspace Name should be just enough for clone successfully
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      expect(await finishButton.waitUntilEnabled()).toBe(true);

      await finishButton.focus();
      await workspacesPage.clickAndWait(finishButton);
      await workspacesPage.waitUntilNoSpinner();

      // wait for Data page
      const dataPage = new DataPage(page);
      await dataPage.waitForLoad();
      // save Data page URL for comparison
      const workspaceDataUrl1 = await page.url();

      // verify new workspace was created and now can be opened successfully
      await workspacesPage.load();
      const workspaceLink = await workspaceCard.getWorkspaceNameLink(cloneWorkspaceName);
      await workspaceLink.click();
      await dataPage.waitForLoad();
      const workspaceDataUrl2 = await page.url();

      expect(workspaceDataUrl1).toEqual(workspaceDataUrl2);
    });
  });

  describe('From "Data" page using side ellipsis menu', () => {

    test('User can clone a workspace in which user is OWNER', async () => {
    // choose one workspace on "Home" page for clone from
      const workspaceCard = new WorkspaceCard(page);
      const retrievedWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(workspaceAccessLevel.OWNER);
      const aWorkspaceCard = fp.shuffle(retrievedWorkspaces)[0];
      await aWorkspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.waitForLoad();

      await dataPage.selectFromEllipsisMenu(workspaceAction.DUPLICATE);

      const workspacesPage = new WorkspacesPage(page);

    // fill out Workspace Name
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();
    // select "Share workspace with same set of collaborators radiobutton
      await workspacesPage.clickShareWithCollaboratorsCheckbox();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      expect(await finishButton.waitUntilEnabled()).toBe(true);

      await finishButton.focus();
      await workspacesPage.clickAndWait(finishButton);
      await workspacesPage.waitUntilNoSpinner();

    // wait for Data page
      await dataPage.waitForLoad();
    // save Data page URL for comparison after sign out then sign in back
      const workspaceDataUrl = await page.url();
      expect(workspaceDataUrl).toContain(cloneWorkspaceName.replace(/-/g, ''));

      await jestPuppeteer.resetBrowser();
      const newPage = await browser.newPage();
      await signIn(newPage);

      const response = await newPage.goto(workspaceDataUrl, {waitUntil: ['domcontentloaded','networkidle0']});
      expect(await response.status()).toEqual(200);
    });
  });

});
