import { signInWithAccessToken } from 'utils/test-utils';
import { LinkText } from 'app/text-labels';
import expect from 'expect';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import { Page } from 'puppeteer';
import { logger } from 'libs/logger';
import WorkspaceCard from 'app/component/workspace-card';

describe('Notebook and Runtime UI Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // This notebook is created by notebook-create-python.spec
  const workspaceName = 'e2eCreatePythonKernelNotebookTest';
  let pyNotebookName: string;

  test('Notebook name is unique', async () => {
    const existWorkspace = await openWorkspace(page, workspaceName);
    if (!existWorkspace) {
      return;
    }

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.openAnalysisPage();
    await analysisPage.waitForLoad();

    const pyNotebookCard = await analysisPage.findNotebookCard();
    pyNotebookName = await pyNotebookCard.getResourceName();

    // Attempt to create another notebook with same name. It should be blocked.
    await analysisPage.createNewNotebookLink().click();

    const modal = new NewNotebookModal(page);
    await modal.waitForLoad();
    await modal.name().type(pyNotebookName);

    const errorTextXpath = `${modal.getXpath()}//*[text()="Name already exists"]`;
    const errorExists = await page.waitForXPath(errorTextXpath, { visible: true });
    expect(errorExists.asElement()).not.toBeNull();

    const disabledButton = await modal.createNotebookButton().isCursorNotAllowed();
    expect(disabledButton).toBe(true);

    // Click "Cancel" button to close modal.
    await modal.clickButton(LinkText.Cancel, { waitForClose: true });
    const modalExists = await modal.exists();
    expect(modalExists).toBe(false);

    // Page remain unchanged, still should be in Analysis page.
    expect(await analysisPage.isLoaded()).toBe(true);
  });

  async function openWorkspace(page: Page, workspaceName: string): Promise<boolean> {
    // Find all workspaces that are older than 30 min.
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    // Don't create new workspace if none found.
    if (!workspaceCard) {
      logger.info(`Cannot find workspace "${workspaceName}". Test end early.`);
      return false;
    }

    // Open workspace.
    await workspaceCard.clickWorkspaceName();
    return true;
  }
});
