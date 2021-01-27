import RuntimePanel, {
  ComputeType,
  StartStopIconState
} from 'app/component/runtime-panel';
import {config} from 'resources/workbench-config';
import {createWorkspace, signInWithAccessToken} from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import {LinkText} from 'app/text-labels';

// This one is going to take a long time.
jest.setTimeout(60 * 30 * 1000);

describe('Updating runtime compute type', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    // Pause a bit to wait for getRuntime to complete
    await page.waitForTimeout(2000);
  });

  test('Switch from GCE to dataproc', async() => {

    // Open the runtime panel
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();

    // Click “create“ , from the default “create panel”
    // using let here instead of const because we navigate during the test and must reinstantiate
    await runtimePanel.clickButton(LinkText.Create);

    // Wait until status shows green in side-nav
    await page.waitForTimeout(2000);
    await runtimePanel.open();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

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

    // Open runtime panel
    await runtimePanel.open();

    // Switch to dataproc cluster with custom settings (e.g. 1 preemptible worker)
    await runtimePanel.pickComputeType(ComputeType.Dataproc);
    await runtimePanel.pickDataprocNumWorkers(3);
    await runtimePanel.pickDataprocNumPreemptibleWorkers(1);
    await runtimePanel.pickWorkerCpus(2);
    await runtimePanel.pickWorkerRamGbs(13);
    await runtimePanel.pickWorkerDisk(60);

    // Wait for indicator to go green in side-nav
    await runtimePanel.clickButton(LinkText.Next);
    await runtimePanel.clickButton(LinkText.Update);
    await page.waitForTimeout(2000);
    await runtimePanel.open();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Go back to the notebook:
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings
    const workersOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/count-workers.py'});
    // Spark config always seems to start at this and then scale if you need additional threads.
    expect(workersOutputText).toBe('\'2\'');

    // Open runtime panel
    await runtimePanel.open();

    // Click 'delete environment'
    await runtimePanel.clickButton(LinkText.DeleteEnvironment);
    await runtimePanel.clickButton(LinkText.Delete);

    // wait until status indicator disappears
    await page.waitForTimeout(2000);
    await runtimePanel.open();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.None);

    // Refresh page, and reopen the panel
    await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
    // Wait for runtime get to complete
    await page.waitForTimeout(2000);
    await runtimePanel.open();

    // Verify that dataproc settings are still shown
    expect(await runtimePanel.getDataprocNumWorkers()).toBe(3);
    expect(await runtimePanel.getDataprocNumPreemptibleWorkers()).toBe(1);
    expect(await runtimePanel.getWorkerCpus()).toBe('2');
    expect(await runtimePanel.getWorkerRamGbs()).toBe('13');
    expect(await runtimePanel.getWorkerDisk()).toBe(60);
  });

});
