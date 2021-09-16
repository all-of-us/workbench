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
import { getPropValue } from 'utils/element-utils';
import { Page } from 'puppeteer';
import { takeScreenshot } from 'utils/save-file-utils';

describe('Genomics Extraction Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = makeWorkspaceName();

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

    // Group 2: Include Demographics Age. range: 18 - 20.
    const minAge = 18;
    const maxAge = 20;
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includeAge(minAge, maxAge, MenuOption.CurrentAge);

    await group2.getGroupCount();
    totalCount = await cohortBuildPage.getTotalCount();

    // Total Count should be 1.
    expect(totalCount).toEqual(1);

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

    // >>> CREATE RUNTIME.

    // Change runtime Compute Type to Dataproc Cluster.
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();

    // Disk (GB) is not visible unless Compute Type is Dataproc Cluster.
    expect(await runtimePanel.getDiskInput().exists()).toEqual(false);

    await runtimePanel.clickButton(LinkText.Customize);

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

    // spinner indicates runtime creation has started.
    const runtimeStatusSpinner = '//*[@data-test-id="runtime-status-icon-container"]/*[@data-icon="sync-alt"]';
    await page.waitForXPath(runtimeStatusSpinner, { visible: true });

    // >>> EXTRACT GENOMIC TO CREATE VCF FILES.

    // Export to new notebook.
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

    const notebookName = makeRandomName();
    await exportToNotebookModal.enterNotebookName(notebookName);
    await exportToNotebookModal.pickLanguage(Language.Python);
    await exportToNotebookModal.pickAnalysisTool(AnalysisTool.Hail);
    await exportToNotebookModal.clickExportButton();

    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Check Genomic Extraction History.
    const genomicExtractionsHistorySidebar = new GenomicExtractionsSidebar(page);
    await genomicExtractionsHistorySidebar.open();

    let historyTable = genomicExtractionsHistorySidebar.getHistoryTable();
    await historyTable.waitUntilVisible();

    // Verify table column names.
    const columnNames = await historyTable.getColumnNames();
    expect(columnNames).toEqual(
      expect.arrayContaining(['DATASET NAME', 'STATUS', 'DATE STARTED', 'COST', 'SIZE', 'DURATION'])
    );

    // Verify row count is 1.
    const rowCount = await historyTable.getRowCount();
    expect(rowCount).toEqual(1);

    // Verify dataset name in row 1 : column 1.
    const td1 = await historyTable.getCell(1, 1);
    const cellValue = await getPropValue(td1, 'textContent');
    expect(cellValue).toEqual(datasetName);
    await genomicExtractionsHistorySidebar.close();

    // spinner indicates extraction has started.
    const extractionStatusSpinner = '//*[@data-test-id="extraction-status-icon-container"]/*[@data-icon="sync-alt"]';
    await page.waitForXPath(extractionStatusSpinner, { visible: true });

    // LONG WAIT: Wait for VCF files and runtime to finish. 50 minutes is the max time.
    await waitWhileLoading(page, 50 * 60 * 1000, { waitForRuntime: true });

    // Verify both runtime and vcf files are created successfully.
    await checkRuntimeRunning(page);

    historyTable = genomicExtractionsHistorySidebar.getHistoryTable();
    await historyTable.waitUntilVisible();
    const td2 = await historyTable.getCell(1, 2);
    const checkImg = await td2.$x('.//*[@data-icon="check-circle" and @role="img"]');
    expect(checkImg).toBeTruthy();

    // Take a screenshot for manual checking in case test has failed.
    await takeScreenshot(page, 'genomic-extraction-history-sidebar.png');

    // Run all notebook code.
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
    await runtimeSidebar.waitForStartStopIconState(StartStopIconState.None);
    await runtimeSidebar.clickButton(LinkText.Create);
    await runtimeSidebar.waitUntilClose();
  }

  async function checkRuntimeRunning(page: Page): Promise<void> {
    const runtimeSidebar = new RuntimePanel(page);
    await runtimeSidebar.open();
    await runtimeSidebar.waitForStartStopIconState(StartStopIconState.Running);
    await runtimeSidebar.close();
    logger.info('Runtime is running');
  }
});
