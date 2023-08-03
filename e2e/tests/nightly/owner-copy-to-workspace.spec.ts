import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard, Tabs } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, findWorkspaceCard, openTab, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import Modal from 'app/modal/modal';

/**
 * This test suite takes a long time to run. Two tests create 3 new workspaces and 2 notebook runtime.
 * Test suite runs nightly shortens CI job runtime and reduces number of new workspaces and notebooks.
 */

/**
 * Test:
 * - Create new Workspace as the copy-to destination Workspace.
 * - Create new Workspace as copy-from Workspace and create new notebook in this Workspace.
 * - Run code to print WORKSPACE_NAMESPACE. It should match Workspace namespace from Workspace URL.
 * - Copy notebook to destination Workspace and give copied notebook a new name.
 * - Verify copied notebook is in destination Workspace.
 * - Open copied notebook and run code to print WORKSPACE_NAMESPACE. It should match destination Workspace namespace.
 * - Delete notebooks.
 *
 * @param {string} sourceWorkspaceName: Source workspace name
 * @param {string} to create new destination workspace with CDR Version
 */
describe.skip('Workspace owner can copy notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const defaultCdrWorkspace = 'e2eNightlyOwnerCopyNotebookToAnotherWorkspace';
  const destinationDefaultCdrWorkspace = 'e2eNightlyOwnerCopyNotebookDestinationWorkspace';
  const destinationOldCdrWorkspace = 'e2eNightlyOwnerCopyNotebookDestinationOldCdrWorkspace';

  // Copy to another workspace works, however the other modal giving user the option to go to copied notebook doesnt show up
  // https://precisionmedicineinitiative.atlassian.net/browse/RW-9034 bugs is going to fix that
  test.skip(
    'Copy notebook to another Workspace when CDR versions match',
    async () => {
      await createCustomCdrVersionWorkspace(defaultCdrWorkspace, config.DEFAULT_CDR_VERSION_NAME);
      await copyNotebookTest(defaultCdrWorkspace, {
        destinationWorkspaceName: destinationDefaultCdrWorkspace,
        destCdrVersionName: config.DEFAULT_CDR_VERSION_NAME
      });
    },
    30 * 60 * 1000
  );

  test.skip(
    'Copy notebook to another Workspace when CDR versions differ',
    async () => {
      // reuse same source workspace for all tests, but always create new destination workspace.
      await createCustomCdrVersionWorkspace(defaultCdrWorkspace, config.DEFAULT_CDR_VERSION_NAME);
      await copyNotebookTest(defaultCdrWorkspace, {
        destinationWorkspaceName: destinationOldCdrWorkspace,
        destCdrVersionName: config.OLD_CDR_VERSION_NAME
      });
    },
    30 * 60 * 1000
  );

  async function copyNotebookTest(
    sourceWorkspaceName: string,
    opts: { destinationWorkspaceName: string; destCdrVersionName: string }
  ) {
    const destWorkspace = await findOrCreateWorkspace(page, {
      workspaceName: opts.destinationWorkspaceName,
      cdrVersion: opts.destCdrVersionName
    });

    // Find and open source workspace Data page.
    const workspaceCard = await findWorkspaceCard(page, sourceWorkspaceName);
    await workspaceCard.clickName();

    // Create notebook in source workspace.
    const sourceNotebookName = makeRandomName('pytest');
    const dataPage = new WorkspaceDataPage(page);

    const sourceWorkspacePage = await dataPage.createNotebook(sourceNotebookName);

    // Exit notebook and returns to the Workspace Analysis tab.
    const analysisPage = await sourceWorkspacePage.goAnalysisPage();

    // Copy to destination Workspace and give notebook a new name.
    const copiedNotebookName = makeRandomName('copy-of');
    await analysisPage.copyNotebookToWorkspace(sourceNotebookName, destWorkspace, copiedNotebookName).catch(() => {
      // Retry. Sometimes POST /notebooks/[notebook-name]/copy request fails.
      analysisPage.copyNotebookToWorkspace(sourceNotebookName, destWorkspace, copiedNotebookName);
    });

    // Verify Copy Success modal.
    const modal = new Modal(page);
    await modal.waitForLoad();
    await modal.waitForButton(LinkText.GoToCopiedNotebook).waitUntilEnabled();
    const modalText = await modal.getTextContent();

    expect(
      modalText.some((text) => {
        return text.includes('Notebooks can only be copied to workspaces in the same access tier.');
      })
    ).toBeTruthy();
    expect(
      modalText.some((text) => {
        return text.includes(`Successfully copied ${sourceNotebookName} to ${destWorkspace}`);
      })
    ).toBeTruthy();
    expect(
      modalText.some((text) => {
        return text.includes('Do you want to view the copied Notebook?');
      })
    ).toBeTruthy();

    // Dismiss modal.
    await modal.clickButton(LinkText.StayHere, { waitForClose: true });

    // Delete notebook
    const deleteModalTextContent = await analysisPage.deleteResourceFromTable(
      sourceNotebookName,
      ResourceCard.Notebook
    );
    expect(deleteModalTextContent).toContain(`Are you sure you want to delete Notebook: ${sourceNotebookName}?`);

    // Perform actions in copied notebook.
    // Open destination Workspace
    await findWorkspaceCard(page, destWorkspace).then((card) => card.clickName());

    // Verify copy-to notebook exists in destination Workspace
    await openTab(page, Tabs.Analysis, analysisPage);
    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findNameCellLinkFromTable({ name: copiedNotebookName });
    expect(notebookCard).toBeTruthy();

    // Delete notebook
    const modalTextContent = await analysisPage.deleteResourceFromTable(copiedNotebookName, ResourceCard.Notebook);
    expect(modalTextContent).toContain('This will permanently delete the Notebook.');

    // Delete destination workspace
    await analysisPage.deleteWorkspace();
  }

  async function createCustomCdrVersionWorkspace(workspaceName: string, cdrVersion: string): Promise<void> {
    await findOrCreateWorkspace(page, { cdrVersion, workspaceName });
  }
});
