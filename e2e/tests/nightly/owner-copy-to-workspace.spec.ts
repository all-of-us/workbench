import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, findWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import Modal from 'app/modal/modal';

// Reuse same source workspace for all tests in this file, in order to reduce test playback time.
// Workspace to be created in first test. If create failed in first test, next test will try create it.
let defaultCdrWorkspace: string;

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
describe('Workspace owner copy notebook tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test(
    'Copy notebook to another Workspace when CDR versions match',
    async () => {
      defaultCdrWorkspace = await createCustomCdrVersionWorkspace(config.DEFAULT_CDR_VERSION_NAME);
      await copyNotebookTest(defaultCdrWorkspace, config.DEFAULT_CDR_VERSION_NAME);
    },
    30 * 60 * 1000
  );

  test(
    'Copy notebook to another Workspace when CDR versions differ',
    async () => {
      // reuse same source workspace for all tests, but always create new destination workspace.
      if (defaultCdrWorkspace === undefined) {
        defaultCdrWorkspace = await createCustomCdrVersionWorkspace(config.DEFAULT_CDR_VERSION_NAME);
      }
      await copyNotebookTest(defaultCdrWorkspace, config.ALTERNATIVE_CDR_VERSION_NAME);
    },
    30 * 60 * 1000
  );
});

async function copyNotebookTest(sourceWorkspaceName: string, destCdrVersionName: string) {
  const destWorkspace = await createWorkspace(page, { cdrVersion: destCdrVersionName });

  // Find and open source workspace Data page.
  const workspaceCard = await findWorkspaceCard(page, sourceWorkspaceName);
  await workspaceCard.clickWorkspaceName();

  // Create notebook in source workspace.
  const sourceNotebookName = makeRandomName('pytest');
  const dataPage = new WorkspaceDataPage(page);

  const sourceWorkspacePage = await dataPage.createNotebook(sourceNotebookName);

  // Exit notebook and returns to the Workspace Analysis tab.
  const analysisPage = await sourceWorkspacePage.goAnalysisPage();

  // Copy to destination Workspace and give notebook a new name.
  const copiedNotebookName = makeRandomName('copy-of');
  await analysisPage.copyNotebookToWorkspace(sourceNotebookName, destWorkspace, copiedNotebookName);

  // Verify Copy Success modal.
  const modal = new Modal(page);
  await modal.waitForLoad();
  const modalText = await modal.getTextContent();
  console.log(`modal text: ${modalText}`);
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
  const deleteModalTextContent = await analysisPage.deleteResource(sourceNotebookName, ResourceCard.Notebook);
  expect(deleteModalTextContent).toContain(`Are you sure you want to delete Notebook: ${sourceNotebookName}?`);

  // Perform actions in copied notebook.
  // Open destination Workspace
  await findWorkspaceCard(page, destWorkspace).then((card) => card.clickWorkspaceName());

  // Verify copy-to notebook exists in destination Workspace
  await dataPage.openAnalysisPage();
  const dataResourceCard = new DataResourceCard(page);
  const notebookCard = await dataResourceCard.findCard(copiedNotebookName, ResourceCard.Notebook);
  expect(notebookCard).toBeTruthy();

  // Delete notebook
  const modalTextContent = await analysisPage.deleteResource(copiedNotebookName, ResourceCard.Notebook);
  expect(modalTextContent).toContain('This will permanently delete the Notebook.');

  // Delete destination workspace
  await analysisPage.deleteWorkspace();
}

async function createCustomCdrVersionWorkspace(cdrVersion: string): Promise<string> {
  const workspaceName = await createWorkspace(page, { cdrVersion });
  return workspaceName;
}
