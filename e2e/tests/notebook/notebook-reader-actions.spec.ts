import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Language, LinkText, MenuOption, ResourceCard, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { createWorkspace, findOrCreateWorkspace, signIn, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import Modal from 'app/modal/modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Workspace READER Jupyter notebook action tests', () => {
  // All tests use same workspace and notebook.
  const workspace = makeWorkspaceName();
  const notebookName = makeRandomName('Py3');

  const pyCode = 'print(1+1)';
  const pyAnswer = 2;

  test('Share notebook to workspace READER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);

    const notebook = await dataPage.createNotebook(notebookName, Language.Python);

    // Run code: 1 + 1.
    const cellOutput = await notebook.runCodeCell(1, { code: pyCode });
    expect(Number.parseInt(cellOutput, 10)).toEqual(pyAnswer);
    await notebook.save();

    await notebook.goAnalysisPage();
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    // Share Workspace to a READER.
    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(config.readerUserName, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);
  });

  test('Workspace READER copy notebook to another workspace', async () => {
    // READER log in.
    await signIn(page, config.readerUserName, config.userPassword);

    // Create a new Workspace. This is the copy-to workspace.
    const readerWorkspaceName = await createWorkspace(page);

    // Verify shared Workspace Access Level is READER.
    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspace);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

    // Verify notebook actions list.
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(page).openAnalysisPage();

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();

    // Create Notebook link is disabled.
    expect(await analysisPage.createNewNotebookLink().isCursorNotAllowed()).toBe(true);

    // Notebook snowman actions Rename, Duplicate and Delete are disabled.
    const dataResourceCard = new DataResourceCard(page);
    let notebookCard = await dataResourceCard.findCard(notebookName, ResourceCard.Notebook);
    // open Snowman menu.
    const snowmanMenu = await notebookCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
    // But the Copy to another Workspace action is available for click.
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();

    await notebookCard.clickResourceName();
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Edit link is disabled.
    expect(await notebookPreviewPage.getEditLink().isCursorNotAllowed()).toBe(true);
    // Run (Playground mode) link is disabled.
    expect(await notebookPreviewPage.getRunPlaygroundModeLink().isCursorNotAllowed()).toBe(true);
    // Verify notebook code and answer are displayed.
    const previewCode = await notebookPreviewPage.getFormattedCode();
    expect(
      previewCode.some((item) => {
        return item.includes(pyCode);
      })
    ).toBe(true);
    expect(
      previewCode.some((item) => {
        return item.includes(String(pyAnswer));
      })
    ).toBe(true);

    // Copy notebook to another Workspace and give notebook a new name.
    const newAnalysisPage = await notebookPreviewPage.goAnalysisPage();
    const copyNotebookName = `copy-of-${notebookName}`;
    await newAnalysisPage.copyNotebookToWorkspace(notebookName, readerWorkspaceName, copyNotebookName);

    // Verify Copy Success modal.
    const modal = new Modal(page);
    await modal.waitForLoad();
    const textContent = await modal.getTextContent();
    const expectedMsg = `Successfully copied ${notebookName}`;
    expect(textContent.some((text) => text.includes('Copy to Workspace'))).toBe(true);
    expect(textContent.some((text) => text.includes(expectedMsg))).toBe(true);

    // Dismiss modal. Open Copied notebook.
    await modal.clickButton(LinkText.GoToCopiedNotebook, { waitForClose: true });

    // Verify current workspace is collaborator Workspace.
    await newAnalysisPage.waitForLoad();
    const workspaceLink = Link.findByName(page, { name: readerWorkspaceName });
    const linkDisplayed = await workspaceLink.isDisplayed();
    expect(linkDisplayed).toBe(true);

    // Verify copied notebook exists in collaborator Workspace.
    notebookCard = await dataResourceCard.findCard(copyNotebookName, ResourceCard.Notebook);
    expect(notebookCard).toBeTruthy();

    // Notebook actions Rename, Duplicate, Delete and Copy to another Workspace actions are avaliable to click.
    const copyNotebookCardMenu = await notebookCard.getSnowmanMenu();
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    await notebookCard.clickSnowmanIcon(); // close menu

    await newAnalysisPage.deleteResource(copyNotebookName, ResourceCard.Notebook);
  });

  test('Workspace READER edit copy of notebook in workspace clone', async () => {
    // READER log in.
    await signIn(page, config.readerUserName, config.userPassword);

    // Verify shared Workspace Access Level is READER.
    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspace);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    await workspaceEditPage.fillOutRequiredDuplicationFields();
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await duplicateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(duplicateButton);

    await dataPage.waitForLoad();
    await dataPage.openAnalysisPage();
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();

    // Create Notebook button is enabled.
    expect(await analysisPage.createNewNotebookLink().isCursorNotAllowed()).toBe(false);

    // Notebook snowman actions Rename, Duplicate and Delete are enabled.
    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findCard(notebookName, ResourceCard.Notebook);
    // open Snowman menu.
    const snowmanMenu = await notebookCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();

    // Click notebook name.
    await notebookCard.clickResourceName();
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Edit link is enabled.
    expect(await notebookPreviewPage.getEditLink().isCursorNotAllowed()).toBe(false);
    // Run (Playground mode) link is enabled.
    expect(await notebookPreviewPage.getRunPlaygroundModeLink().isCursorNotAllowed()).toBe(false);
    // Verify notebook code and answer are displayed.
    const previewCode = await notebookPreviewPage.getFormattedCode();
    expect(
      previewCode.some((item) => {
        return item.includes(pyCode);
      })
    ).toBe(true);
    expect(
      previewCode.some((item) => {
        return item.includes(String(pyAnswer));
      })
    ).toBe(true);

    await notebookPreviewPage.goAnalysisPage();
    await dataPage.deleteResource(notebookName, ResourceCard.Notebook);
    await dataPage.deleteWorkspace();
  });
});
