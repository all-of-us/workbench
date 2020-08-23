import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import NewNotebookModal from 'app/component/new-notebook-modal';
import WorkspacesPage from 'app/page/workspaces-page';
import {LinkText} from 'app/text-labels';
import {makeRandomName, makeWorkspaceName} from 'utils/str-utils';
import {signIn} from 'utils/test-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Workspace owner Jupyter notebook action tests', () => {

  // In order to reduce test playback time, reuse same Workspace for all tests in this file.
  // Workspace to be created in first test. If first test fails, next test will create it.
  let workspaceName: string;

  beforeEach(async () => {
    await signIn(page);
  });


  test('Notebook name must be unique in same workspace', async () => {
    const workspacesPage = new WorkspacesPage(page);

    const notebookName = makeRandomName('pyNotebook1');
    workspaceName = makeWorkspaceName();

    const workspaceAnalysisPage = await workspacesPage.createNotebook({workspaceName, notebookName});

    const notebookCard = await DataResourceCard.findCard(page, notebookName);
    expect(notebookCard).toBeTruthy();

    await workspaceAnalysisPage.createNewNotebookLink().then( (link) => link.click());

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
    await modal.clickButton(LinkText.Cancel, {waitForClose: true});

    const modalExists = await modal.exists();
    expect(modalExists).toBe(false);
    // Page remain unchanged, still should be the Analysis page.
    expect(await workspaceAnalysisPage.isLoaded()).toBe(true);

    await workspaceAnalysisPage.deleteNotebook(notebookName);
  })

  test('Notebook can be duplicated by workspace owner', async () => {
    const workspacesPage = new WorkspacesPage(page);
    const notebookName = makeRandomName('pyNotebook2');
    const workspaceAnalysisPage = await workspacesPage.createNotebook({workspaceName, notebookName});
    const duplNotebookName = await workspaceAnalysisPage.duplicateNotebook(notebookName);
    // Delete clone notebook.
    await workspaceAnalysisPage.deleteNotebook(duplNotebookName);
    await workspaceAnalysisPage.deleteNotebook(notebookName);
  })

  test('Notebook can be renamed by workspace owner', async () => {
    const workspacesPage = new WorkspacesPage(page);
    const notebookName = makeRandomName('pyNotebook3');
    const workspaceAnalysisPage = await workspacesPage.createNotebook({workspaceName, notebookName});

    const newName = makeRandomName('test-notebook');
    const modalTextContents = await workspaceAnalysisPage.renameNotebook(notebookName, newName);
    expect(modalTextContents).toContain(`Enter new name for ${notebookName}.ipynb`);

    const newNotebookCard = new DataResourceCard(page);
    let cardExists = await newNotebookCard.cardExists(newName, CardType.Notebook);
    expect(cardExists).toBe(true);

    cardExists = await newNotebookCard.cardExists(notebookName, CardType.Notebook);
    expect(cardExists).toBe(false);

    await workspaceAnalysisPage.deleteNotebook(newName);
  })


});
