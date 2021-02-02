import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {LinkText, ResourceCard} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {createWorkspace, findOrCreateWorkspace, signInWithAccessToken} from 'utils/test-utils';
import {config} from 'resources/workbench-config';
import WorkspaceCard from 'app/component/workspace-card';
import Modal from 'app/modal/modal';

// re-run one more time if test has failed
jest.retryTimes(1);

// Reuse same source workspace for all tests in this file, in order to reduce test playback time.
// Workspace to be created in first test. If create failed in first test, next test will try create it.
let defaultCdrWorkspace: string;

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

   test('Copy notebook to another Workspace when CDR versions match', async () => {
      defaultCdrWorkspace = await createCustomCdrVersionWorkspace(config.defaultCdrVersionName);
      await copyNotebookTest(defaultCdrWorkspace, config.defaultCdrVersionName);
   }, 30 * 60 * 1000)

   test('Copy notebook to another Workspace when CDR versions differ', async () => {
      // reuse same source workspace for all tests, but always create new destination workspace.
      if (defaultCdrWorkspace === undefined) {
         defaultCdrWorkspace = await createCustomCdrVersionWorkspace(config.defaultCdrVersionName);
      }
      await copyNotebookTest(defaultCdrWorkspace, config.altCdrVersionName);
   }, 30 * 60 * 1000)
});


async function copyNotebookTest(sourceWorkspaceName: string, destCdrVersionName: string) {

   const destWorkspace = await createWorkspace(page, destCdrVersionName).then(card => card.getWorkspaceName());

   // Find and open source workspace Data page.
   const workspaceCard = await WorkspaceCard.findCard(page, sourceWorkspaceName);
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
   await modal.waitForButton(LinkText.GoToCopiedNotebook);
   const textContent = await modal.getTextContent();
   const successMsg = `Successfully copied ${sourceNotebookName}  to ${destWorkspace} . Do you want to view the copied Notebook?`;
   expect(textContent).toContain(successMsg);
   // Dismiss modal.
   await modal.clickButton(LinkText.StayHere, {waitForClose: true});

   // Delete notebook
   const deleteModalTextContent = await analysisPage.deleteResource(sourceNotebookName, ResourceCard.Notebook);
   expect(deleteModalTextContent).toContain(`Are you sure you want to delete Notebook: ${sourceNotebookName}?`);

   // Perform actions in copied notebook.
   // Open destination Workspace
   await findOrCreateWorkspace(page, {workspaceName: destWorkspace}).then(card => card.clickWorkspaceName());

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
   const workspace = await createWorkspace(page, cdrVersion);
   return workspace.getWorkspaceName();
}
