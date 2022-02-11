import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {
  AgeSelectionRadioButton,
  AnalysisTool,
  ConceptSetSelectValue,
  DatasetValueSelect,
  Language,
  LinkText
} from 'app/text-labels';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import GenomicsVariantExtractConfirmationModal from 'app/modal/genomic-extract-confirmation-modal';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import RuntimePanel, { AutoPauseIdleTime, ComputeType } from 'app/sidebar/runtime-panel';
import { logger } from 'libs/logger';
import GenomicExtractionsSidebar from 'app/sidebar/genomic-extractions-sidebar';
import { Page } from 'puppeteer';
import { takeScreenshot } from 'utils/save-file-utils';
import expect from 'expect';
import { AccessTierDisplayNames } from 'app/page/workspace-edit-page';

// 60 minutes. Test could take a long time.
// Since refresh token expires in 60 min. test can fail if running takes longer than 60 min.
jest.setTimeout(60 * 60 * 1000);

describe('Genomics Extraction Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const maxWaitTime = 50 * 60 * 1000;
  const workspaceName = makeWorkspaceName();
  const notebookName = makeRandomName('genomicDataToVcf');

  test('Export genomics dataset to new notebook', async () => {
    await createWorkspace(page, {
      workspaceName,
      cdrVersionName: config.CONTROLLED_TIER_CDR_VERSION_NAME,
      dataAccessTier: AccessTierDisplayNames.Controlled
    });

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
    await group2.includeAge(minAge, maxAge, AgeSelectionRadioButton.CurrentAge);

    await group2.getGroupCount();
    totalCount = await cohortBuildPage.getTotalCount();

    // Total Count should be greater than 1.
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
    await datasetPage.selectConceptSets([ConceptSetSelectValue.WholeGenomeSequenceVariantData]);
    // Step 3: make sure "VCF Files(s)" is selected.
    await datasetPage.selectValues([DatasetValueSelect.VCFFile]);

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

    // Increase runtime auto-pause time because runtime will auto. pause after 30 min (default value) of idle
    // while export to vcf file still in progress.
    await runtimePanel.pickAutoPauseTime(AutoPauseIdleTime.EightHours);

    // Disk (GB) is visible after select Dataproc Cluster.
    expect(await runtimePanel.getStandardDiskGbs()).toBe(100);
    // Make sure CPU and RAM remains unchanged after change Compute Type.
    expect(await runtimePanel.getCpus()).toBe('4');
    expect(await runtimePanel.getRamGbs()).toBe('15');

    // Start creating runtime but NOT wait until finish.
    await runtimePanel.createRuntime({ waitForComplete: false });

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // EXTRACTING GENOMIC TO CREATE VCF FILES.

    // Export genomic data to new notebook.
    const analyzeButton = datasetPage.getAnalyzeButton();
    await analyzeButton.waitUntilEnabled();
    await analyzeButton.click();

    // Confirm Extract Genomics Variant Data to VCF files.
    const confirmationModal = new GenomicsVariantExtractConfirmationModal(page);
    await confirmationModal.waitForLoad();
    const continueButton = confirmationModal.getContinueButton();
    await continueButton.click();

    // Fill out Export to Notebook modal.
    const exportToNotebookModal = new ExportToNotebookModal(page);
    await exportToNotebookModal.waitForLoad();

    await exportToNotebookModal.enterNotebookName(notebookName);
    await exportToNotebookModal.pickLanguage(Language.Python);
    await exportToNotebookModal.pickAnalysisTool(AnalysisTool.Hail);
    const notebookPreviewPage = await exportToNotebookModal.clickExportButton();

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

    // Verify dataset name (Column #1 is 'Dataset Name').
    const tableCellValue = await historyTable.getCellValue(1, 1);
    expect(tableCellValue).toEqual(datasetName);

    await genomicExtractionsHistorySidebar.close();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // LONG WAIT: Wait for VCF files extraction and runtime creation to finish.
    await waitForComplete(page, maxWaitTime);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // RUN NOTEBOOK CODE.
    const notebookPage = await notebookPreviewPage.openEditMode(notebookName);
    // Run all cells. Run one cell at a time is slower.
    await notebookPage.runAllCells();
    await notebookPage.waitForKernelIdle(5 * 60 * 1000, 5000);
    await notebookPage.save();

    // Find any output_error.
    const frame = await notebookPage.getIFrame();
    const hasOutputError = (await frame.$$('.cell.code_cell .output_subarea.output_error')).length > 0 ? true : false;
    expect(hasOutputError).toBe(false);

    // Check all seven code cells have ran.
    const prompts = await frame.$$('.input .prompt_container .input_prompt');
    expect(prompts.length).toBe(7);

    // Spot-check on some cell outputs.
    const [, cell2Output] = await notebookPage.getCellInputOutput(2);
    expect(cell2Output).toMatch(/^VCF extraction has completed, continuing$/);

    // cell #7 output is html table.
    const cell7 = await notebookPage.findLastCell();
    const cell7OutputElement = await cell7.findOutputElementHandle();
    const [imgElement] = await cell7OutputElement.$x('./table');
    expect(imgElement).toBeTruthy();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // Clean up.
    await notebookPage.deleteNotebook(notebookName);

    logger.info('Deleting runtime');
    await runtimePanel.open();
    await runtimePanel.clickButton(LinkText.DeleteEnvironment);
    await runtimePanel.clickButton(LinkText.Delete);
    await runtimePanel.waitUntilClose();
    // Not waiting for delete to finish.
  });

  // Check creation status.
  async function waitForComplete(page: Page, maxTime: number): Promise<boolean> {
    const pollInterval = 10 * 1000;
    let isRuntimeReady = false;
    let isExtractionReady = false;
    const runtimeSidebar = new RuntimePanel(page);
    const genomicSidebar = new GenomicExtractionsSidebar(page);
    const startTime = Date.now();
    while (Date.now() - startTime <= maxTime) {
      isRuntimeReady = !isRuntimeReady ? await runtimeSidebar.waitForRunningAndClose(pollInterval) : true;
      // At the time of writing this test, it takes 30 - 40 minutes to create VCF files.
      isExtractionReady = !isExtractionReady ? await genomicSidebar.waitForCompletionAndClose(pollInterval) : true;
      if (isRuntimeReady && isExtractionReady) {
        logger.info('Runtime is running and Genomic data extraction is done.');
        return true;
      }
      await page.waitForTimeout(pollInterval);
      const timeInSecs = Math.round((Date.now() - startTime) / 1000);
      logger.info(`Waiting [ ${timeInSecs} ] seconds for runtime and genomic extraction to finish.`);
    }
    // Take screenshot for manual checking.
    await runtimeSidebar.open();
    await takeScreenshot(page, 'genomic-extraction-test-runtime-sidebar');
    await runtimeSidebar.close();

    await genomicSidebar.open();
    await takeScreenshot(page, 'genomic-extraction-test-history-sidebar');
    await genomicSidebar.close();

    throw new Error('Runtime is not running or/and genomic extraction is not done.');
  }
});
