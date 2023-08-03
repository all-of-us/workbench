import { ConceptSets, LinkText, MenuOption, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import {
  createDataset,
  findOrCreateWorkspace,
  findWorkspaceCard,
  openTab,
  signInWithAccessToken
} from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { config } from 'resources/workbench-config';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import DataResourceCard from 'app/component/card/data-resource-card';
import DatasetBuildPage from 'app/page/dataset-build-page';
import { waitWhileLoading } from 'utils/waits-utils';
import { Page } from 'puppeteer';
import { logger } from 'libs/logger';

describe.skip('Workspace Reader and Writer Permission Test', () => {
  const assignAccess = [
    {
      accessRole: WorkspaceAccessLevel.Writer,
      userEmail: config.WRITER_USER
    },
    {
      accessRole: WorkspaceAccessLevel.Reader,
      userEmail: config.READER_USER
    }
  ];

  const workspace = makeRandomName('e2eShareWorkspace');
  let datasetName;

  test('Share workspace to READER and WRITER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    datasetName = await createDataset(page, {
      conceptSets: [ConceptSets.Demographics, ConceptSets.AllSurveys]
    });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    for (const assign of assignAccess) {
      await aboutPage.shareWorkspaceWithUser(assign.userEmail, assign.accessRole);
    }

    await reloadAboutPage();
    const collaborators = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER (login user) information.
    expect(collaborators.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER or READER information.
    for (const assign of assignAccess) {
      expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);
    }
  });

  // Test depends on previous test: Will fail when workspace is not found or share has failed.
  test.each(assignAccess)('WRITER and READER cannot share, edit or delete workspace', async (assign) => {
    await signInWithAccessToken(page, assign.userEmail);
    logger.info(`${assign.accessRole} log in`);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    const accessLevel = await workspaceCard.getAccessLevel();
    // Verify Snowman menu: Share, Edit and Delete actions are not available for click.
    expect(accessLevel).toBe(assign.accessRole);
    await workspaceCard.verifyWorkspaceCardMenuOptions(assign.accessRole);

    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    const collaborators = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER information in Collaborator list.
    expect(collaborators.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER or READER information in Collaborator list.
    expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);

    // Verify Share modal: the Search input and Save button are disabled.
    const modal = await aboutPage.openShareModal();
    const searchInput = modal.waitForSearchBox();
    expect(await searchInput.isDisabled()).toBe(true);
    await modal.getSaveButton().expectEnabled(false);
    await modal.clickButton(LinkText.Cancel, { waitForClose: true });
    await aboutPage.waitForLoad();

    // Verify Workspace Actions menu: READER or WRITER cannot Share, Edit or Delete workspace.
    await verifyWorkspaceActionMenuOptions(page);
  });

  test.each(assignAccess)('WRITER can rename, edit or delete dataset while READER cannot', async (assign) => {
    await signInWithAccessToken(page, assign.userEmail);
    logger.info(`${assign.accessRole} log in`);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    await workspaceCard.clickName();

    const dataPage = new WorkspaceDataPage(page);

    // Check Create Cohorts and Datasets buttons
    switch (assign.accessRole) {
      case WorkspaceAccessLevel.Writer:
        await dataPage.getAddDatasetButton().expectEnabled(true);
        await dataPage.getAddCohortsButton().expectEnabled(true);
        break;
      case WorkspaceAccessLevel.Reader:
        await dataPage.getAddDatasetButton().expectEnabled(false);
        await dataPage.getAddCohortsButton().expectEnabled(false);
        break;
      default:
        break;
    }

    await openTab(page, Tabs.Datasets, dataPage);

    // Verify Snowman menu: Rename, Edit Export to Notebook and Delete actions are not available for click for Dataset entries.
    const resourceCard = new DataResourceCard(page);
    const dataSetNameCell = await resourceCard.findNameCellLinkFromTable({ name: datasetName });
    expect(dataSetNameCell).not.toBeNull();

    switch (assign.accessRole) {
      case WorkspaceAccessLevel.Reader:
        {
          const snowmanMenu = await resourceCard.getSnowmanMenuFromTable(datasetName);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.RenameDataset)).toBe(true);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.ExportToNotebook)).toBe(true);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);

          // Although Edit option is not available to click. User can click on dataset name and see the dataset details.
          await dataSetNameCell.click();
          const dataSetEditPage = new DatasetBuildPage(page);
          await dataSetEditPage.waitForLoad();

          // Analyze and Save buttons are disabled.
          const analyzeButton = dataSetEditPage.getAnalyzeButton();
          await analyzeButton.expectEnabled(false);
          const saveButton = dataSetEditPage.getSaveButton();
          await saveButton.expectEnabled(false);

          // No matter of what has changed, the Analyze button remains disabled.
          await dataSetEditPage.selectConceptSets([ConceptSets.FitbitIntraDaySteps]);
          await dataSetEditPage.getPreviewTableButton().click();
          await waitWhileLoading(page);
          await analyzeButton.expectEnabled(false);
        }
        break;
      case WorkspaceAccessLevel.Writer:
        {
          const snowmanMenu = await resourceCard.getSnowmanMenuFromTable(datasetName);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.RenameDataset)).toBe(false);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.ExportToNotebook)).toBe(false);
          expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);

          // User can click on dataset name and see the dataset details.
          await dataSetNameCell.click();
          const dataSetEditPage = new DatasetBuildPage(page);
          await dataSetEditPage.waitForLoad();

          // Analyze button is enabled.
          const analyzeButton = dataSetEditPage.getAnalyzeButton();
          await analyzeButton.expectEnabled(true);
          // Save button is disabled because nothing has changed.
          const saveButton = dataSetEditPage.getSaveButton();
          await saveButton.expectEnabled(false);

          // Make a change, Save button is enabled.
          await dataSetEditPage.selectConceptSets([ConceptSets.FitbitIntraDaySteps]);
          await saveButton.expectEnabled(true);
        }
        break;
      default:
        break;
    }
  });

  // Test depends on previous test: Will fail when workspace is not found and share didn't work.
  test('Stop share workspace', async () => {
    await signInWithAccessToken(page);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    // Stop share.
    for (const assign of assignAccess) {
      await aboutPage.removeCollaborator(assign.userEmail);
      await aboutPage.waitForLoad();
    }
    await reloadAboutPage();

    // Verify WRITER and READER are gone in Collaborator list.
    const collaborators = await aboutPage.findUsersInCollaboratorList();
    for (const assign of assignAccess) {
      expect(collaborators.has(assign.accessRole)).toBe(false);
    }
  });

  // Open Data page then back to About page in order to refresh Collaborators list in page.
  async function reloadAboutPage() {
    const dataPage = new WorkspaceDataPage(page);
    await openTab(page, Tabs.Data, dataPage);
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);
  }

  async function verifyWorkspaceActionMenuOptions(page: Page): Promise<void> {
    const dataPage = new WorkspaceDataPage(page);
    const snowmanMenu = await dataPage.getWorkspaceActionMenu();
    const options = await snowmanMenu.getAllOptionTexts();
    expect(options).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));

    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
  }
});
