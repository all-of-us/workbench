import WorkspacesPage from 'app/page/workspaces-page';
import {findWorkspace, signIn} from 'utils/test-utils';
import {EllipsisMenuAction} from 'app/text-labels';
import DataPage from 'app/page/data-page';

describe('Clone workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the clone.
   */
  describe('From "Your Workspaces" page using Workspace card ellipsis menu', () => {

    test('As OWNER, user can clone workspace', async () => {

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.asElementHandle().hover();
      // click on Ellipsis "Duplicate"
      await (workspaceCard.getEllipsis()).clickAction(EllipsisMenuAction.Duplicate);

      // fill out Workspace Name should be just enough for clone successfully
      const workspacesPage = new WorkspacesPage(page);
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspacesPage.clickCreateFinishButton(finishButton);

      // wait for Data page
      const dataPage = new DataPage(page);
      await dataPage.waitForLoad();
      // save Data page URL for comparison
      const workspaceDataUrl1 = page.url();

      // verify new workspace was created and now can be opened successfully
      await workspacesPage.load();
      const workspaceLink = await workspaceCard.getWorkspaceNameLink(cloneWorkspaceName);
      await workspaceLink.click();
      await dataPage.waitForLoad();
      const workspaceDataUrl2 = page.url();

      expect(workspaceDataUrl1).toEqual(workspaceDataUrl2);
    });
  });

  describe('From "Data" page using side ellipsis menu', () => {

    test('As OWNER, user can clone workspace', async () => {

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.selectWorkspaceAction(EllipsisMenuAction.Duplicate);

      const workspacesPage = new WorkspacesPage(page);

      // fill out Workspace Name
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();
      // select "Share workspace with same set of collaborators radiobutton
      await workspacesPage.clickShareWithCollaboratorsCheckbox();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspacesPage.clickCreateFinishButton(finishButton);

      // wait for Data page
      await dataPage.waitForLoad();
      // save Data page URL for comparison after sign out then sign in back
      const workspaceDataUrl = page.url();
      // strips out dash from workspace name
      expect(workspaceDataUrl).toContain(cloneWorkspaceName.replace(/-/g, ''));

      // starting a new incognito page
      await page.deleteCookie(...await page.cookies());
      await jestPuppeteer.resetBrowser();
      await page.waitFor(2000);
      const newBrowser = await browser.createIncognitoBrowserContext();
      const newPage = await newBrowser.newPage();
      const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36';
      await newPage.setUserAgent(userAgent);
      await signIn(newPage);

      const response = await newPage.goto(workspaceDataUrl, {waitUntil: ['domcontentloaded','networkidle0'], timeout: 60000});
      expect(await response.status()).toEqual(200);

      await newPage.close();
    });
  });

});
