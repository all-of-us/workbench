import RuntimePanel, {ComputeType} from 'app/component/runtime-panel';
import {config} from 'resources/workbench-config';
import {createWorkspace, signInWithAccessToken} from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import {ResourceCard} from 'app/text-labels';

// This one is going to take a long time
jest.setTimeout(60 * 30 * 1000);
// Retry one more when fails
jest.retryTimes(1);

describe('Updating runtime compute type', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Switch from GCE to dataproc', async() => {

    await createWorkspace(page, config.altCdrVersionName).then(card => card.clickWorkspaceName());

    // Open the runtime panel
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();

    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);

    // Open a Python notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run some Python commands to validate the VM configuration
    const cpusOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/count-cpus.py'});
    // Default CPU count is 4
    expect(parseInt(cpusOutputText, 10)).toBe(4);
    // This gets the amount of memory available to Python in bytes
    const memoryOutputText = await notebook.runCodeCell(2, {codeFile: 'resources/python-code/count-memory.py'});
    // Default memory is 15 gibibytes, we'll check that it is between 14 billion and 16 billion bytes
    expect(parseInt(memoryOutputText, 10)).toBeGreaterThanOrEqual(14 * 1000 * 1000 * 1000);
    expect(parseInt(memoryOutputText, 10)).toBeLessThanOrEqual(16 * 1000 * 1000 * 1000);
    // This gets the disk space in bytes
    const diskOutputText = await notebook.runCodeCell(3, {codeFile: 'resources/python-code/count-disk-space.py'});
    // Default disk is 50 gibibytes, we'll check that it is between 45 and 55 billion bytes
    expect(parseInt(diskOutputText, 10)).toBeGreaterThanOrEqual(45 * 1000 * 1000 * 1000);
    expect(parseInt(diskOutputText, 10)).toBeLessThanOrEqual(55 * 1000 * 1000 * 1000);
    await notebook.save();

    // Open runtime panel
    await runtimePanel.open();

    // Switch to dataproc cluster with custom settings (e.g. 1 preemptible worker)
    const numWorkers = 3;
    const numPreemptibleWorkers = 1;
    const numCpus = 2;
    const ramGbs = 13;
    const workerDisk = 60;

    await runtimePanel.pickComputeType(ComputeType.Dataproc);
    await runtimePanel.pickDataprocNumWorkers(numWorkers);
    await runtimePanel.pickDataprocNumPreemptibleWorkers(numPreemptibleWorkers);
    await runtimePanel.pickWorkerCpus(numCpus);
    await runtimePanel.pickWorkerRamGbs(ramGbs);
    await runtimePanel.pickWorkerDisk(workerDisk);

    // Apply changes and wait for new runtime running
    const notebookPreviewPage = await runtimePanel.applyChanges();

    // Go back to the notebook
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings
    const workersOutputText = await notebook.runCodeCell(4, {codeFile: 'resources/python-code/count-workers.py'});
    // Spark config always seems to start at this and then scale if you need additional threads.
    expect(workersOutputText).toBe('\'2\'');
    await notebook.save();

    // Delete runtime
    await notebook.deleteRuntime();

    // Verify that dataproc settings are still shown
    await runtimePanel.open();
    expect(await runtimePanel.getDataprocNumWorkers()).toBe(numWorkers);
    expect(await runtimePanel.getDataprocNumPreemptibleWorkers()).toBe(numPreemptibleWorkers);
    expect(parseInt(await runtimePanel.getWorkerCpus(), 10)).toBe(numCpus);
    expect(parseInt(await runtimePanel.getWorkerRamGbs(), 10)).toBe(ramGbs);
    expect(await runtimePanel.getWorkerDisk()).toBe(workerDisk);

    // Delete notebook
    const workspaceAnalysisPage = await notebookPreviewPage.goAnalysisPage();
    await workspaceAnalysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });

});
