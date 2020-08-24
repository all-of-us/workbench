import Navigation, {NavLink} from 'app/component/navigation';
import Link from 'app/element/link';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {EllipsisMenuAction, Language, LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {config} from 'resources/workbench-config';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn, signInAs, waitWhileLoading} from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

jest.setTimeout(20 * 60 * 1000);

describe('Workspace reader Jupyter notebook action tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test: Workspace reader can copy notebook to another Workspace then gain Edit right to the clone notebook.
   * - Create a Workspace and a new notebook. Run simple code in notebook and save notebook.
   * - Share Workspace with collaborator as READER.
   * - Sign in as collaborator.
   * - Create Workspace where collaborator is the OWNER.
   * - Verify Workspace and notebook permissions.
   * - Copy notebook to collaborator Workspace.
   * - Full Edit rights to clone notebook.
   * - Verify original notebook contents are unchaged in clone notebook.
   * - Delete clone notebook.
   */
  test('Workspace reader becomes owner of cloned notebook', async () => {
    const workspaceName = await findWorkspace(page, {create: true}).then(card => card.clickWorkspaceName());

    let dataPage = new DataPage(page);
    const notebookName = makeRandomName('pyAdd');
    let notebook = await dataPage.createNotebook(notebookName, Language.Python);

     // Run code: 1 + 1.
    const code = 'print(1+1)';
    const cellOutput = await notebook.runCodeCell(1, {code});
    await notebook.save();
    await notebook.goAnalysisPage();

    const aboutLink = await Link.findByName(page, {name: 'About'});
    await aboutLink.clickAndWait();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    // Share Workspace to collaborator as READER.
    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);
    await Navigation.navMenu(page, NavLink.SIGN_OUT);

     // browser and page reset.
    await page.deleteCookie(...await page.cookies());
    await jestPuppeteer.resetPage();
    await jestPuppeteer.resetBrowser();

    // Sign in as collaborator in new Incognito page.
    const newPage = await signInAs(config.collaboratorUsername, config.userPassword);

    // Create a new Workspace. This is the copy to workspace.
    const collaboratorWorkspaceName = await findWorkspace(newPage, {create: true}).then(card => card.getWorkspaceName());

    // Verify shared Workspace Access Level is READER.
    const workspaceCard = await WorkspaceCard.findCard(newPage, workspaceName);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

    // Verify notebook actions list.
    await workspaceCard.clickWorkspaceName();
    dataPage = new DataPage(newPage);
    await dataPage.openTab(TabLabelAlias.Analysis);

    // Notebook actions Rename, Duplicate and Delete actions are disabled.
    const dataResourceCard = new DataResourceCard(newPage);
    let notebookCard = await dataResourceCard.findCard(notebookName, CardType.Notebook);
    let menu = notebookCard.getEllipsis();
    await menu.clickEllipsis(); // open ellipsis menu.
    expect(await menu.isDisabled(EllipsisMenuAction.Rename)).toBe(true);
    expect(await menu.isDisabled(EllipsisMenuAction.Duplicate)).toBe(true);
    expect(await menu.isDisabled(EllipsisMenuAction.Delete)).toBe(true);
    // But the Copy to another Workspace action is available for click.
    expect(await menu.isDisabled(EllipsisMenuAction.CopyToAnotherWorkspace)).toBe(false);
    await menu.clickEllipsis(); // close ellipsis menu.

    // Copy notebook to another Workspace and give notebook a new name.
    const analysisPage = new WorkspaceAnalysisPage(newPage);
    const copiedNotebookName = `copy-of-${notebookName}`;
    await analysisPage.copyNotebookToWorkspace(notebookName, collaboratorWorkspaceName, copiedNotebookName);

    // Verify Copy Success modal.
    const modal = new Modal(newPage);
    await modal.waitForButton(LinkText.GoToCopiedNotebook);
    const textContent = await modal.getTextContent();
    expect(textContent).toContain('Copy to Workspace');
    const expectedFullMsg = `Successfully copied ${notebookName}  to ${collaboratorWorkspaceName} . Do you want to view the copied Notebook?`
    expect(textContent).toContain(expectedFullMsg);

    // Dismiss modal. Open Copied notebook.
    await modal.clickButton(LinkText.GoToCopiedNotebook, {waitForClose: true});

    // Verify current workspace is collaborator Workspace.
    await analysisPage.waitForLoad();
    const workspaceLink = await Link.findByName(newPage, {name: collaboratorWorkspaceName});
    const linkDisplayed = await workspaceLink.isDisplayed();
    expect(linkDisplayed).toBe(true);

    // Verify copied notebook exists in collaborator Workspace.
    notebookCard = await dataResourceCard.findCard(copiedNotebookName, CardType.Notebook);
    expect(notebookCard).toBeTruthy();

    // Notebook actions Rename, Duplicate, Delete and Copy to another Workspace actions are avaliable to click.
    menu = notebookCard.getEllipsis();
    await menu.clickEllipsis();
    expect(await menu.isDisabled(EllipsisMenuAction.Rename)).toBe(false);
    expect(await menu.isDisabled(EllipsisMenuAction.Duplicate)).toBe(false);
    expect(await menu.isDisabled(EllipsisMenuAction.Delete)).toBe(false);
    expect(await menu.isDisabled(EllipsisMenuAction.CopyToAnotherWorkspace)).toBe(false);
    await menu.clickEllipsis();

    // Open copied notebook, verify original notebook contents unchanged.
    await notebookCard.clickResourceName();
    const notebookPreviewPage = new NotebookPreviewPage(newPage);
    await notebookPreviewPage.waitForLoad();
    notebook = await notebookPreviewPage.openEditMode(copiedNotebookName);

    // Get Code cell [1] input and output.
    const [newCellInput, newCellOutput] = await notebook.getCellInputOutput(1);

    // Verify Code cell [1] input and output.
    expect(newCellInput).toEqual(code);
    expect(newCellOutput).toEqual(cellOutput);

    await notebook.deleteNotebook(copiedNotebookName);
  })

});
