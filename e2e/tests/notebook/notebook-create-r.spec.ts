import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { Language, ResourceCard } from 'app/text-labels';
import expect from 'expect';
import { Page } from 'puppeteer';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import DataResourceCard from 'app/component/data-resource-card';
import { Tabs } from 'app/page/workspace-base';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Create R kernel notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  let workspaceUrl: string;
  const workspaceName = 'e2eCreateRKernelNotebookTest';
  const rNotebookName = makeRandomName('R');

  test('Run R code', async () => {
    await loadWorkspace(page, workspaceName);

    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(rNotebookName, Language.R);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('R');

    // Run math function in Code cell [1].
    let cellIndex = 1;
    const code1Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/calculate-max.R',
      markdownWorkaround: true
    });
    expect(code1Output).toEqual('[1] 20');

    // Print sys environment details in Code cell [2].
    cellIndex = 2;
    const code2Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/sys-print.R',
      markdownWorkaround: true
    });
    expect(code2Output).toMatch(/success$/);

    // Import R libs in Code cell [3].
    cellIndex = 3;
    const cell3Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/import-libs.R',
      markdownWorkaround: true,
      timeOut: 5 * 60 * 1000
    });
    await notebook.save();
    expect(cell3Output).toMatch(/success$/);
  });

  test('Duplicate rename delete notebook', async () => {
    await loadWorkspace(page, workspaceName);

    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    await analysisPage.waitForLoad();

    // Start clone notebook.
    const cloneNotebookName = `Duplicate of ${rNotebookName}`;
    await analysisPage.duplicateNotebook(rNotebookName);

    // Rename notebook clone.
    const newNotebookName = makeRandomName('r-cloneNotebook');
    const modalTextContents = await analysisPage.renameResource(
      cloneNotebookName,
      newNotebookName,
      ResourceCard.Notebook
    );
    expect(modalTextContents).toContain(`Enter new name for ${cloneNotebookName}`);

    // Notebook card with new name is found.
    const dataResourceCard = new DataResourceCard(page);
    let cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(true);

    // Notebook card with old name is not found.
    cardExists = await dataResourceCard.cardExists(cloneNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(false);

    // Delete newly renamed notebook.
    await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);
    // Verify delete was successful.
    cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(false);

    // Delete R notebook
    await analysisPage.deleteResource(rNotebookName, ResourceCard.Notebook);
    await analysisPage.waitForLoad();
  });

  // Helper functions: Load previously saved URL instead clicks thru links to open workspace data page.
  async function loadWorkspace(page: Page, workspaceName?: string): Promise<string> {
    if (workspaceUrl) {
      await page.goto(workspaceUrl, { waitUntil: ['load', 'networkidle0'] });
      return;
    }

    workspaceName = await findOrCreateWorkspace(page, { workspaceName });

    workspaceUrl = page.url(); // Save URL for load workspace directly without search.
    return workspaceName;
  }
});
