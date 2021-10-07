import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Language, LinkText, MenuOption, ResourceCard, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, signInWithAccessToken, signOut } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import Modal from 'app/modal/modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

// TODO (RW-7312): Re-enable once issue is fixed.
xdescribe('Workspace WRITER Jupyter notebook action tests', () => {
  // All tests use same workspace and notebook.
  const workspaceName = makeWorkspaceName();
  const notebookName = makeRandomName('py3');
  const writerWorkspaceName = 'e2eNotebookWriterActionsTestWorkspace'; // WRITER workspace for copy-to.

  test('Create Python notebook and share workspace to a WRITER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName });

    // Share workspace to a WRITER before creating new notebook.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    await aboutPage.shareWorkspaceWithUser(config.WRITER_USER, WorkspaceAccessLevel.Writer);
    await waitWhileLoading(page);

    const notebook = await dataPage.createNotebook(notebookName, Language.Python);

    // Run Python code.
    expect(
      await notebook.runCodeCell(1, {
        codeFile: 'resources/python-code/gsutil.py',
        markdownWorkaround: true
      })
    ).toMatch(/success$/);

    await notebook.save();
    await signOut(page);
  });

  test('WRITER can clone workspace and edit notebook in workspace clone', async () => {
    // WRITER log in.
    await signInWithAccessToken(page, config.WRITER_USER);

    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clone();

    // Currently displayed workspace is the workspace clone.
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
        return item.includes('success');
      })
    ).toBe(true);

    await notebookPreviewPage.goAnalysisPage();
    await dataPage.deleteResource(notebookName, ResourceCard.Notebook);
    await dataPage.deleteWorkspace();
  });

  test('WRITER can copy notebook to another workspace', async () => {
    // WRITER log in.
    await signInWithAccessToken(page, config.WRITER_USER);

    await findOrCreateWorkspace(page, { workspaceName: writerWorkspaceName });

    // Verify WRITER is the access level in shared Workspace Access.
    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    // Verify notebook actions list.
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(page).openAnalysisPage();

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();

    // Create Notebook link is disabled.
    const isCreateLinkDisabled = await analysisPage.createNewNotebookLink().isCursorNotAllowed();
    expect(isCreateLinkDisabled).toBe(false);

    // Notebook snowman actions Rename, Duplicate and Delete are disabled.
    const dataResourceCard = new DataResourceCard(page);
    let notebookCard = await dataResourceCard.findCard(notebookName, ResourceCard.Notebook);
    // open Snowman menu.  All Workspace actions are available for click.
    const snowmanMenu = await notebookCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();

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
        return item.includes('success');
      })
    ).toBe(true);

    // Copy notebook to another Workspace and give notebook a new name.
    const newAnalysisPage = await notebookPreviewPage.goAnalysisPage();
    const copyNotebookName = `copy-of-${notebookName}`;
    await newAnalysisPage.copyNotebookToWorkspace(notebookName, writerWorkspaceName, copyNotebookName);

    // Verify Copy Success modal.
    const modal = new Modal(page);
    await modal.waitForLoad();
    const textContent = await modal.getTextContent();
    const expectedMsg = `Successfully copied ${notebookName}`;
    expect(textContent.some((text) => text.includes(expectedMsg))).toBe(true);

    // Dismiss modal. Open Copied notebook.
    await modal.clickButton(LinkText.GoToCopiedNotebook, { waitForClose: true });

    // Verify current workspace is collaborator Workspace.
    await newAnalysisPage.waitForLoad();
    const workspaceLink = Link.findByName(page, { name: writerWorkspaceName });
    const linkDisplayed = await workspaceLink.isVisible();
    expect(linkDisplayed).toBe(true);

    // Verify copied notebook exists in collaborator Workspace.
    notebookCard = await dataResourceCard.findCard(copyNotebookName, ResourceCard.Notebook);
    expect(notebookCard).toBeTruthy();

    // Notebook actions Rename, Duplicate, Delete and Copy to another Workspace actions are available for click.
    const copyNotebookCardMenu = await notebookCard.getSnowmanMenu();
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    await notebookCard.clickSnowmanIcon(); // close menu

    await newAnalysisPage.deleteResource(copyNotebookName, ResourceCard.Notebook);
  });
});
