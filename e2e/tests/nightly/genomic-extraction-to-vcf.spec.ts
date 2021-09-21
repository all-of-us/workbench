import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {
  AnalysisTool,
  Language,
  LinkText,
  MenuOption,
  SelectConceptSetCheckBox,
  SelectValueCheckBox
} from 'app/text-labels';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import GenomicsVariantExtractConfirmationModal from 'app/modal/genomic-variant-extract-confirmation-modal';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import RuntimePanel, { ComputeType, StartStopIconState } from 'app/component/runtime-panel';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import NotebookPage from 'app/page/notebook-page';
import { logger } from 'libs/logger';
import { waitWhileLoading } from 'utils/waits-utils';
import GenomicExtractionsSidebar from 'app/component/genomic-extractions-sidebar';
import { Page } from 'puppeteer';
import { takeScreenshot } from 'utils/save-file-utils';

// 60 minutes. Test could take a long time.
jest.setTimeout(60 * 60 * 1000);

describe('Genomics Extraction Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const maxWaitTime = 50 * 60 * 1000;
  const workspaceName = makeWorkspaceName();
  const notebookName = makeRandomName('testPyNotebook');

  test('Create genomics dataset and export to notebook', async () => {
    await findOrCreateWorkspace(page, { cdrVersion: config.ALTERNATIVE_CDR_VERSION_NAME, workspaceName });

    // Create new cohort from Whole Genome Variant.
    const dataPage = new WorkspaceDataPage(page);

    // Group 1: Include Whole Genome Variant.
    const cohortBuildPage = await dataPage.clickAddCohortsButton();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeWholeGenomeVariant();

    const group1Count = await group1.getGroupCount();
    let totalCount = await cohortBuildPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);

    // Group 2: Include Demographics Age. range: 20 - 21 to have smallest number of participants.
    const minAge = 20;
    const maxAge = 21;
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includeAge(minAge, maxAge, MenuOption.CurrentAge);

    await group2.getGroupCount();
    totalCount = await cohortBuildPage.getTotalCount();

    // Total Count should be 1.
    expect(totalCount).toBeGreaterThanOrEqual(1);

    // Save cohort.
    const cohortName = await cohortBuildPage.createCohort();

    // Create new Dataset.
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    const datasetPage = await cohortActionsPage.clickCreateDatasetButton();

    // Step 1: select user created cohort.
    await datasetPage.selectCohorts([cohortName]);

    // Step 2: select "All whole genome sequence variant data".
    await datasetPage.selectConceptSets([SelectConceptSetCheckBox.WholeGenomeSequenceVariantData]);

    // Step 3: make sure "VCF Files(s)" is selected.
    await datasetPage.selectValues([SelectValueCheckBox.VCFFile]);

    // Save dataset.
    const createModal = await datasetPage.clickCreateButton();
    const datasetName = await createModal.createDataset();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // CREATING NOTEBOOK RUNTIME.

    // Change runtime Compute Type to Dataproc Cluster.
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();

    // If Customize button has been clicked and environment already exists, button won't be visible again.
    const customizeButton = runtimePanel.getCustomizeButton();
    if (await customizeButton.exists()) {
      await runtimePanel.clickButton(LinkText.Customize);
    }

    expect(await runtimePanel.getCpus()).toBe('4');
    expect(await runtimePanel.getRamGbs()).toBe('15');

    // Change Compute Type to Dataproc Cluster.
    await runtimePanel.pickComputeType(ComputeType.Dataproc);

    // Disk (GB) is visible after select Dataproc Cluster.
    expect(await runtimePanel.getDiskGbs()).toBe(100);
    // Make sure CPU and RAM remains unchanged after change Compute Type.
    expect(await runtimePanel.getCpus()).toBe('4');
    expect(await runtimePanel.getRamGbs()).toBe('15');

    // Start creating runtime but NOT wait until finish.
    await createRuntime(page);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // EXTRACTING GENOMIC TO CREATE VCF FILES.

    // Export genomic data to new notebook.
    const analyzeButton = datasetPage.getAnalyzeButton();
    await analyzeButton.waitUntilEnabled();
    await analyzeButton.click();

    // Confirm Extract Genomics Variant Data to VCF files.
    const confirmationModal = new GenomicsVariantExtractConfirmationModal(page);
    await confirmationModal.waitForLoad();
    const continueButton = await confirmationModal.getContinueButton();
    await continueButton.click();

    // Fill out Export to Notebook modal.
    const exportToNotebookModal = new ExportToNotebookModal(page);
    await exportToNotebookModal.waitForLoad();

    await exportToNotebookModal.enterNotebookName(notebookName);
    await exportToNotebookModal.pickLanguage(Language.Python);
    await exportToNotebookModal.pickAnalysisTool(AnalysisTool.Hail);
    await exportToNotebookModal.clickExportButton();

    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Check Genomic Extraction History.
    const genomicExtractionsHistorySidebar = new GenomicExtractionsSidebar(page);
    await genomicExtractionsHistorySidebar.open();

    const historyTable = genomicExtractionsHistorySidebar.getHistoryTable();
    await historyTable.waitUntilVisible();

    // Verify table column names.
    const columnNames = await historyTable.getColumnNames();
    expect(columnNames).toEqual(
      expect.arrayContaining(['DATASET NAME', 'STATUS', 'DATE STARTED', 'COST', 'SIZE', 'DURATION'])
    );

    // Verify row count is 1.
    const rowCount = await historyTable.getRowCount();
    expect(rowCount).toEqual(1);

    // Verify dataset name.
    const tableCellValue = await historyTable.getColumnValue('Dataset Name');
    expect(tableCellValue).toEqual(datasetName);

    await genomicExtractionsHistorySidebar.close();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // LONG WAIT: Wait for VCF files extraction and runtime creation to finish.
    await waitForDone(page, maxWaitTime);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // RUN NOTEBOOK CODE.
    const notebookPage = await notebookPreviewPage.openEditMode(notebookName);

    // We run code cell one by one in order to verify code output.
    await runCell(notebookPage, 1);

    await runCell(notebookPage, 2);

    await runCell(notebookPage, 3);

    await runCell(notebookPage, 4);

    await runCell(notebookPage, 5);
  });

  async function runCell(notebookPage: NotebookPage, cellIndex = 1, timeout?: number): Promise<string> {
    const cell = await notebookPage.findCell(cellIndex);
    await cell.focus();
    await notebookPage.run(timeout);
    return cell.waitForOutput(timeout);
  }

  async function createRuntime(page: Page): Promise<void> {
    logger.info('Creating runtime');
    const runtimeSidebar = new RuntimePanel(page);
    await runtimeSidebar.open();
    const isRunning = await runtimeSidebar.isRunning();
    if (isRunning) {
      return;
    }
    await runtimeSidebar.waitForStartStopIconState(StartStopIconState.None);
    await runtimeSidebar.clickButton(LinkText.Create);
    await runtimeSidebar.waitUntilClose();
  }

  // Check creation status.
  async function waitForDone(page: Page, maxTime: number): Promise<boolean> {
    const runtimeSidebar = new RuntimePanel(page);
    const genomicSidebar = new GenomicExtractionsSidebar(page);
    const startTime = Date.now();
    while (Date.now() - startTime <= maxTime) {
      await waitWhileLoading(page, 60 * 1000, { waitForRuntime: true }).catch(() => {
        // Leave blank.
      });
      const isRuntimeReady = await runtimeSidebar.waitForRunning(30 * 1000);
      const isExtractionReady = await genomicSidebar.waitForGenomicDataExtractionDone(30 * 1000);
      if (isRuntimeReady && isExtractionReady) {
        return true;
      }
    }
    // Take screenshot for manual checking.
    await runtimeSidebar.open();
    await takeScreenshot(page, 'genomic-extraction-test-runtime-sidebar.png');
    await runtimeSidebar.close();

    await genomicSidebar.open();
    await takeScreenshot(page, 'genomic-extraction-test-history-sidebar.png');
    await genomicSidebar.close();

    throw new Error('runtime is not running or/and genomic extraction is not done.');
  }
});
