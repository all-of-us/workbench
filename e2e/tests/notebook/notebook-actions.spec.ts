import DataResourceCard from 'app/component/data-resource-card';
import DataPage from 'app/page/data-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {LinkText} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import NewNotebookModal from 'app/component/new-notebook-modal';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  describe('Create new notebooks', () => {

    test('Notebook names must be unique in same workspace', async () => {
      expect.assertions(5);

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebookName = makeRandomName('test-notebook-');
      const dataPage = new DataPage(page);
      const notebookPage = await dataPage.createNotebook(notebookName);

      // Do not run code. Returns to the Workspace Analysis tab.
      await notebookPage.goBackAnalysisPage();

      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.waitForLoad();

      const notebookCard = await DataResourceCard.findCard(page, notebookName);
      expect(notebookCard).toBeTruthy();

      await analysisPage.createNewNotebookLink().then( (link) => link.click());

      const modal = new NewNotebookModal(page);
      await modal.waitForLoad();

      await modal.name().then( (textbox) => textbox.type(notebookName));

      const errorTextXpath = `${modal.getXpath()}//*[text()="Name already exists"]`;
      const errorExists = await page.waitForXPath(errorTextXpath, {visible: true});
      expect(errorExists.asElement()).not.toBeNull();

      const createButton = await modal.createNotebookButton();
      const disabledButton = await createButton.isCursorNotAllowed();
      expect(disabledButton).toBe(true);

      // Click "Cancel" button.
      await modal.waitForButton(LinkText.Cancel).then( (butn) => butn.click());
      await modal.waitUntilClose();
      const modalExists = await modal.exists();
      expect(modalExists).toBe(false);
      // Page remain unchanged, still should be the Analysis page.
      expect(await analysisPage.isLoaded()).toBe(true);
    })

  })

});
