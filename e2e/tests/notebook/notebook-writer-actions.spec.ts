import { Page } from 'puppeteer';
import CopyToWorkspaceModal from 'app/modal/copy-to-workspace-modal';
import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceCard from 'app/component/card/workspace-card';
import Link from 'app/element/link';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import { LinkText, MenuOption, ResourceCard, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import Modal from 'app/modal/modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName } from 'utils/str-utils';
import expect from 'expect';
import WorkspaceDataPage from 'app/page/workspace-data-page';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe.skip('Workspace WRITER notebook tests', () => {
  const notebookName = makeRandomName('notebookWriterActionsTest-Py3');
  const workspaceName = 'e2eNotebookWriterActionsTest';
  const writerWorkspaceName = 'e2eNotebookTestWriterWorkspace1';

  test('WRITER create workspace', async () => {
    await signInWithAccessToken(page, config.WRITER_USER);
    await findOrCreateWorkspace(page, { workspaceName: writerWorkspaceName });
  });

  test.skip('WRITER can copy notebook to another workspace with same access tier', async () => {
    // WRITER log in.
    await signInWithAccessToken(page, config.WRITER_USER);

    // Verify WRITER is the access level in shared Workspace.
    await new WorkspacesPage(page).load();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName });
    const accessLevel = await workspaceCard.getAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    // Verify notebook actions list.
    await workspaceCard.clickName({ pageExpected: new WorkspaceDataPage(page) });

    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    // Create Notebook link is disabled.
    const isCreateLinkDisabled = await analysisPage.createNewNotebookLink().isCursorNotAllowed();
    expect(isCreateLinkDisabled).toBe(false);

    let notebookCard = await verifyWriterNotebookPermission(page, notebookName);

    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookCard.clickName({ pageExpected: notebookPreviewPage });

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
    const notebookCopyName = makeRandomName(`${notebookName}-copy`);
    await newAnalysisPage.copyNotebookToWorkspace(notebookName, writerWorkspaceName, notebookCopyName);

    // Verify Copy Success modal.
    const modal = new Modal(page);
    await modal.waitForLoad();
    let modalText = await modal.getTextContent();
    const expectedMsg = `Successfully copied ${notebookName}`;
    expect(modalText.some((text) => text.includes(expectedMsg))).toBe(true);

    // Dismiss modal. Open Copied notebook.
    await modal.clickButton(LinkText.GoToCopiedNotebook, { waitForClose: true });

    // Verify current workspace is collaborator Workspace.
    await newAnalysisPage.waitForLoad();
    const workspaceLink = Link.findByName(page, { name: writerWorkspaceName });
    const linkDisplayed = await workspaceLink.isVisible();
    expect(linkDisplayed).toBe(true);

    await verifyWriterNotebookPermission(page, notebookCopyName);

    // Cannot copy notebook to another workspace which already has a notebook with same name.

    // Open Copy modal
    const resourceCard = new DataResourceCard(page);
    notebookCard = await resourceCard.findCard({ name: notebookCopyName, cardType: ResourceCard.Notebook });
    await notebookCard.selectSnowmanMenu(MenuOption.CopyToAnotherWorkspace, { waitForNav: false });
    // Fill out modal fields.
    const copyModal = new CopyToWorkspaceModal(page);
    await copyModal.waitForLoad();
    await copyModal.copyToAnotherWorkspace(workspaceName, notebookName);
    await waitWhileLoading(page);

    expect(await copyModal.isLoaded()).toBe(true);
    modalText = await copyModal.getTextContent();
    const regex = new RegExp(/Notebook with the same name already exists in the targeted workspace/i);
    expect(modalText.some((text) => regex.exec(text) !== null)).toBe(true);
    // Close Copy modal
    await copyModal.clickButton(LinkText.Close, { waitForClose: true });

    // Delete notebook. Don't delete workspace because it's reused in test runs.
    await newAnalysisPage.deleteResource(notebookCopyName, ResourceCard.Notebook);
  });

  async function verifyWriterNotebookPermission(page: Page, notebookName: string): Promise<DataResourceCard> {
    // Notebook snowman actions Rename, Duplicate and Delete are enabled.
    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findCard({
      name: notebookName,
      cardType: ResourceCard.Notebook,
      timeout: 60 * 1000
    });

    // open Snowman menu.
    const snowmanMenu = await notebookCard.getCardSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Rename)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.CopyToAnotherWorkspace)).toBe(false);
    // close Snowman menu.
    await notebookCard.clickSnowmanIcon();
    return notebookCard;
  }
});
