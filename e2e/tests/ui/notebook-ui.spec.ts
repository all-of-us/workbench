import { openTab, signInWithAccessToken } from 'utils/test-utils';
import { LinkText, Tabs } from 'app/text-labels';
import expect from 'expect';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import { ElementHandle, Page } from 'puppeteer';
import { logger } from 'libs/logger';
import WorkspaceCard from 'app/component/card/workspace-card';
import HomePage from 'app/page/home-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { getPropValue } from '../../utils/element-utils';

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
      logger.info(`Cannot find workspace "${workspaceName}". Test end early.`);
      return;
    }

    const pyNotebookCard = await openAnyNotebook(page);
    if (!pyNotebookCard) {
      logger.info('Cannot find a notebook. Test end early.');
      return;
    }

    pyNotebookName = await getPropValue<string>(pyNotebookCard, 'textContent');

    // Attempt to create another notebook with same name. It should be blocked.
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.createNewNotebookLink().click();

    const modal = new NewNotebookModal(page);
    await modal.waitForLoad();
    await modal.name().type(pyNotebookName.replace('.ipynb', ''));

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
    await new HomePage(page).goToAllWorkspacesPage();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName, timeout: 2000 });
    // Don't create new workspace if none found.
    if (!workspaceCard) {
      return false;
    }

    await workspaceCard.clickName({ pageExpected: new WorkspaceDataPage(page) });
    return true;
  }

  async function openAnyNotebook(page: Page): Promise<ElementHandle | null> {
    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);
    return analysisPage.findNotebookCard();
  }
});
