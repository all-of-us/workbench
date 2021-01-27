import RuntimePanel, {
  ComputeType,
  RuntimePreset,
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
  });


  test('Switch from dataproc to GCE', async() => {

    await createWorkspace(page, config.altCdrVersionName).then(card => card.clickWorkspaceName());

    // Open the runtime panel
    // Click “customize“ , from the default “create panel”
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();

    await runtimePanel.clickButton(LinkText.Customize);

    // Use the preset selector to pick “Hail genomics analysis“
    await runtimePanel.pickRuntimePreset(RuntimePreset.HailGenomicsAnalysis);

    // Create runtime
    await runtimePanel.createRuntime();

    // Open a notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run notebook to validate runtime settings
    const workersOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/count-workers.py'});
    // Spark config always seems to start at this and then scale if you need additional threads.
    expect(workersOutputText).toBe('\'2\'');

    // Open runtime panel
    await runtimePanel.open();

    // Switch to a GCE VM with custom settings
    await runtimePanel.pickComputeType(ComputeType.Standard);
    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(60);
    await runtimePanel.clickButton(LinkText.Next);
    await runtimePanel.clickButton(LinkText.Update);
    await runtimePanel.waitUntilClose();

    // Automatically opens the Preview page
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Wait new runtime running
    await page.waitForTimeout(2000);
    await runtimePanel.open();
    // runtime status transition from Stopping to None to Running
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.None);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Go back to the notebook
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings (cpu, disk, memory)
    const cpusOutputText = await notebook.runCodeCell(2, {codeFile: 'resources/python-code/count-cpus.py'});
    expect(parseInt(cpusOutputText, 10)).toBe(8);
    // This gets the amount of memory available to Python in bytes
    const memoryOutputText = await notebook.runCodeCell(3, {codeFile: 'resources/python-code/count-memory.py'});
    expect(parseInt(memoryOutputText, 10)).toBeGreaterThanOrEqual(28 * 1000 * 1000 * 1000);
    expect(parseInt(memoryOutputText, 10)).toBeLessThanOrEqual(32 * 1000 * 1000 * 1000);
    // This gets the disk space in bytes
    const diskOutputText = await notebook.runCodeCell(4, {codeFile: 'resources/python-code/count-disk-space.py'});
    // for whatever reason this always comes out at around 52 billion bytes despite definitely asking for 60
    expect(parseInt(diskOutputText, 10)).toBeGreaterThanOrEqual(50 * 1000 * 1000 * 1000);
    expect(parseInt(diskOutputText, 10)).toBeLessThanOrEqual(70 * 1000 * 1000 * 1000);
    await notebook.save();

    // Delete runtime
    await runtimePanel.open();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Click ''delete environment”
    await runtimePanel.clickButton(LinkText.DeleteEnvironment);
    await runtimePanel.clickButton(LinkText.Delete);

    await notebookPreviewPage.waitForLoad();

    // Verify GCE custom settings are still shown
    await runtimePanel.open();
    expect(await runtimePanel.getCpus()).toBe('8');
    expect(await runtimePanel.getRamGbs()).toBe('30');
    expect(await runtimePanel.getDiskGbs()).toBe(60);
  });

});
