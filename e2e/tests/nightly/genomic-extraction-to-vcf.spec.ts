import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {
  AccessTierDisplayNames,
  AgeSelectionRadioButton,
  AnalysisTool,
  ConceptSets,
  DataSets,
  Language,
  LinkText
} from 'app/text-labels';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { makeRandomName } from 'utils/str-utils';
import GenomicsVariantExtractConfirmationModal from 'app/modal/genomic-extract-confirmation-modal';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import RuntimePanel, { AutoPauseIdleTime, ComputeType } from 'app/sidebar/runtime-panel';
import { logger } from 'libs/logger';
import GenomicExtractionsSidebar from 'app/sidebar/genomic-extractions-sidebar';
import { Page } from 'puppeteer';
import { takeScreenshot } from 'utils/save-file-utils';
import expect from 'expect';
import { range } from 'lodash';

// 60 minutes. Test could take a long time.
// Since refresh token expires in 60 min. test may fail if running takes longer than 60 min.
jest.setTimeout(60 * 60 * 1000);

describe.skip('Genomics Extraction Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const waitForCompletionMaxTime = 50 * 60 * 1000;
  const workspaceName = 'e2eGenomicExtractionToVcfTest';
  const notebookName = makeRandomName('genomicDataToVcf');

  test('Export genomics dataset to new notebook', async () => {
    await findOrCreateWorkspace(page, {
      workspaceName,
      cdrVersion: config.CONTROLLED_TIER_CDR_VERSION_NAME,
      dataAccessTier: AccessTierDisplayNames.Controlled
    });

    // Create new cohort from Whole Genome Variant.
    const dataPage = new WorkspaceDataPage(page);

    // Group 1: Include Whole Genome Variant.
    const cohortBuildPage = await dataPage.clickAddCohortsButton();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeShortReadWGS();

    const group1Count = await group1.getGroupCount();
    let totalCount = await cohortBuildPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);

    // Group 2: Include Demographics Age. range: 21 - 22 to have smallest number of participants.
    const minAge = 21;
    const maxAge = 22;
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includeAge(minAge, maxAge, AgeSelectionRadioButton.CurrentAge);

    await group2.getGroupCount();
    totalCount = await cohortBuildPage.getTotalCount();

    // Total Count should be greater than or equal to 1 and less than or equal to 5.
    // At the time of writing test, Total Count is 2. Total Count can change if synthetic dataset changes.
    // We want to keep Total Count small. Otherwise it will take longer time to for the extraction job to complete.
    expect(totalCount).toBeGreaterThanOrEqual(1);
    expect(totalCount).toBeLessThanOrEqual(5);

    // Save cohort.
    const cohortName = await cohortBuildPage.createCohort();

    // Create new Dataset.
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    const datasetPage = await cohortActionsPage.clickCreateDatasetButton();

    // Step 1: select user created cohort.
    await datasetPage.selectCohorts([cohortName]);
    // Step 2: select "All whole genome sequence variant data".
    await datasetPage.selectConceptSets([ConceptSets.WholeGenomeSequenceVariantData]);
    // Step 3: make sure "VCF Files(s)" is selected.
    await datasetPage.selectValues([DataSets.VCFFile]);

    // Save dataset.
    const createModal = await datasetPage.clickCreateButton();
    const datasetName = await createModal.create();

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

    // Change Compute Type to Dataproc Cluster.
    await runtimePanel.pickComputeType(ComputeType.Dataproc);

    // Increase runtime auto-pause time because runtime will auto. pause after 30 min (default value) of idle
    // while export to vcf file still in progress.
    await runtimePanel.pickAutoPauseTime(AutoPauseIdleTime.EightHours);

    // Pick master node disk size.
    await runtimePanel.pickStandardDiskGbs(150);

    // Start creating runtime but NOT wait until finish.
    await runtimePanel.createRuntime({ waitForComplete: false });

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // EXTRACTING GENOMIC TO CREATE VCF FILES.

    // Try waiting a few minutes before launching genomic extraction. We've observed that
    // immediately launching an extraction can result in VPC-SC errors with BigQuery in a
    // small fraction of test runs. Waiting a bit here may avoid consistency issues with
    // adding the new project to the perimeter. See RW-8607.
    await page.waitForTimeout(3 * 60 * 1000);

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

    // Verify row count is 1.
    const rowCount = await historyTable.getRowCount();
    expect(rowCount).toBeGreaterThanOrEqual(1);

    // Verify dataset name found in table (Column #1 is 'Dataset Name').
    const rowValues = await Promise.all(range(0, rowCount).map((row) => historyTable.getCellValue(row + 1, 1)));
    const rowIndex = rowValues.findIndex((v) => v === datasetName);
    expect(rowIndex).not.toBe(-1);

    await genomicExtractionsHistorySidebar.close();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // LONG WAIT: Wait for VCF files extraction and runtime creation to finish.
    await waitForRuntimeComplete(page, waitForCompletionMaxTime);
    await waitForExtractionComplete(page, datasetName, waitForCompletionMaxTime);

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

  async function waitForRuntimeComplete(page: Page, maxTime: number): Promise<boolean> {
    let isDone = false;
    const pollInterval = 10 * 1000;
    const runtimeSidebar = new RuntimePanel(page);

    const startTime = Date.now();
    while (Date.now() - startTime <= maxTime) {
      let isSuccess = false;
      await runtimeSidebar.open();
      isDone = await runtimeSidebar.waitForFinish(pollInterval);
      if (isDone) {
        isSuccess = await runtimeSidebar.isRunning();
        if (!isSuccess) {
          // Take screenshot for manual verification.
          await takeScreenshot(page, 'genomic-extraction-test-runtime-sidebar');
          break;
        }
      }
      await runtimeSidebar.close();
      if (isSuccess) {
        logger.info('Runtime is running.');
        return true;
      }
      const timeSpentInMin = Math.round((Date.now() - startTime) / 1000) / 60;
      logger.info(`Waited [ ${timeSpentInMin} ] minutes for runtime to start running.`);
      await page.waitForTimeout(pollInterval);
    }

    throw new Error('Runtime is not running or it has failed.');
  }

  // Check creation status.
  async function waitForExtractionComplete(page: Page, datasetName: string, maxTime: number): Promise<boolean> {
    let isDone = false;
    const pollInterval = 10 * 1000;
    const genomicSidebar = new GenomicExtractionsSidebar(page);

    const startTime = Date.now();
    while (Date.now() - startTime <= maxTime) {
      let isSuccess = false;
      await genomicSidebar.open();
      // At the time of writing this test, it takes 30 - 40 minutes to create the VCF file.
      isDone = !(await genomicSidebar.isInProgress(datasetName));
      if (isDone) {
        isSuccess = await genomicSidebar.isJobSuccess(datasetName);
        if (!isSuccess) {
          // Take screenshot for manual verification.
          await takeScreenshot(page, 'genomic-extraction-test-history-sidebar');
          break;
        }
      }
      await genomicSidebar.close();
      if (isSuccess) {
        logger.info('Genomic extraction job completed successfully.');
        return true;
      }
      const timeSpentInMin = Math.round((Date.now() - startTime) / 1000) / 60;
      logger.info(`Waited [ ${timeSpentInMin} ] minutes for genomic extraction job to finish.`);
      await page.waitForTimeout(pollInterval);
    }

    throw new Error('Genomic extraction job is not done or it has failed.');
  }
});
