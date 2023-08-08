import RuntimePanel, { ComputeType, RuntimePreset, StartStopIconState } from 'app/sidebar/runtime-panel';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { AccessTierDisplayNames, LinkText, ResourceCard } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';

// This test could take a long time to run
jest.setTimeout(40 * 60 * 1000);

describe.skip('Updating runtime compute type', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = makeRandomName('e2eDataprocToGceTest');

  //TODO: Refactor to contract test(s) https://precisionmedicineinitiative.atlassian.net/browse/RW-9658
  test.skip('Switch from dataproc to GCE', async () => {
    await findOrCreateWorkspace(page, {
      workspaceName,
      cdrVersion: config.CONTROLLED_TIER_CDR_VERSION_NAME,
      dataAccessTier: AccessTierDisplayNames.Controlled
    });

    // Open the runtime panel
    // Click “customize“ , from the default “create panel”
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    await runtimePanel.clickButton(LinkText.Customize);

    // Use the preset selector to pick “Hail genomics analysis“
    await runtimePanel.pickRuntimePreset(RuntimePreset.HailGenomicsAnalysis);

    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);

    // Open a notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run notebook to validate runtime settings
    const workersOutputText = await notebook.runCodeCell(1, { codeFile: 'resources/python-code/count-workers.py' });
    // Spark config always seems to start at this and then scale if you need additional threads.
    expect(workersOutputText).toBe("'2'");
    await notebook.save();

    // Open runtime panel
    await runtimePanel.open();

    // Switch to a GCE VM with custom settings
    await runtimePanel.pickComputeType(ComputeType.Standard);
    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);

    // Apply changes and wait for new runtime running
    const notebookPreviewPage = await runtimePanel.applyChanges(true);

    // Go back to the notebook
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings (cpu, disk, memory)
    const cpusOutputText = await notebook.runCodeCell(2, { codeFile: 'resources/python-code/count-cpus.py' });
    expect(parseInt(cpusOutputText, 10)).toBe(8);

    // This gets the amount of memory available to Python in GiB
    const memoryOutputText = await notebook.runCodeCell(3, { codeFile: 'resources/python-code/count-memory.py' });
    expect(parseInt(memoryOutputText, 10)).toBeGreaterThanOrEqual(28);
    expect(parseInt(memoryOutputText, 10)).toBeLessThanOrEqual(32);

    await notebook.save();

    // Delete runtime
    await runtimePanel.open();
    // Confirm runtime status is Running before delete
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Delete environment
    await notebook.deleteRuntime();

    // Verify GCE custom settings are still shown
    await runtimePanel.open();
    expect(await runtimePanel.getCpus()).toBe('8');
    expect(await runtimePanel.getRamGbs()).toBe('30');

    // Delete notebook
    const workspaceAnalysisPage = await notebookPreviewPage.goAnalysisPage();
    await workspaceAnalysisPage.deleteResourceFromTable(notebookName, ResourceCard.Notebook);
  });
});
