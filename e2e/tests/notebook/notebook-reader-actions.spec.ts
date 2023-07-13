import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceCard from 'app/component/card/workspace-card';
import Link from 'app/element/link';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption, ResourceCard, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import Modal from 'app/modal/modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Workspace READER Jupyter notebook action tests', () => {
  // Test reuse same workspace and notebook if exists.
  const workspaceName = makeWorkspaceName();
  const notebookName = makeRandomName('py', { includeHyphen: false });
  const readerWorkspaceName = 'e2eNotebookReaderActionsTestWorkspace'; // READER workspace for copy-to.

  const pyCode = '!jupyter kernelspec list';
  const pyAnswer = 'python3';

  // TODO(RW-7312): update and re-enable
  test.skip('Workspace READER copy notebook to another workspace', async () => {
    // READER log in.
    await signInWithAccessToken(page, config.READER_USER);

    await findOrCreateWorkspace(page, { workspaceName: readerWorkspaceName });

    // Verify shared Workspace Access Level is READER.
    await new WorkspacesPage(page).load();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName });
    const accessLevel = await workspaceCard.getAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

    // Verify notebook actions list.
    await workspaceCard.clickName();
    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    // Create Notebook link is disabled.
    expect(await analysisPage.createNewNotebookLink().isCursorNotAllowed()).toBe(true);

    // Notebook snowman actions Rename, Duplicate and Delete are disabled.
    const dataResourceCard = new DataResourceCard(page);
    let notebookCard = await dataResourceCard.findCard({ name: notebookName, cardType: ResourceCard.Notebook });
    // open Snowman menu.
    const snowmanMenu = await notebookCard.getCardSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
    // But the Copy to another Workspace action is available for click.
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();

    await notebookCard.clickName();
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
    const copyOfNotebookName = `copy-of-${notebookName}`;
    await newAnalysisPage.copyNotebookToWorkspace(notebookName, readerWorkspaceName, copyOfNotebookName);

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
    const linkDisplayed = await workspaceLink.isVisible();
    expect(linkDisplayed).toBe(true);

    // Verify copied notebook exists in collaborator Workspace.
    notebookCard = await dataResourceCard.findCard({ name: copyOfNotebookName, cardType: ResourceCard.Notebook });
    expect(notebookCard).toBeTruthy();

    // Notebook actions Rename, Duplicate, Delete and Copy to another Workspace actions are avaliable to click.
    const copyNotebookCardMenu = await notebookCard.getCardSnowmanMenu();
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    await notebookCard.clickSnowmanIcon(); // close menu

    await newAnalysisPage.deleteResource(copyOfNotebookName, ResourceCard.Notebook);
  });

  // TODO(RW-7312): update and re-enable
  test.skip('Workspace READER edit copy of notebook in workspace clone', async () => {
    // READER log in.
    await signInWithAccessToken(page, config.READER_USER);

    // Verify shared Workspace Access Level is READER.
    await new WorkspacesPage(page).load();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName });
    await workspaceCard.clickName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    await workspaceEditPage.fillOutRequiredDuplicationFields();
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await duplicateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(duplicateButton);

    await dataPage.waitForLoad();
    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    // Create Notebook button is enabled.
    expect(await analysisPage.createNewNotebookLink().isCursorNotAllowed()).toBe(false);

    // Notebook snowman actions Rename, Duplicate and Delete are enabled.
    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findCard({ name: notebookName, cardType: ResourceCard.Notebook });
    // open Snowman menu.
    const snowmanMenu = await notebookCard.getCardSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();

    // Click notebook name.
    await notebookCard.clickName();
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
