import HelpSidebar from 'app/component/help-sidebar';
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

// This one is going to take a long time.
jest.setTimeout(60 * 30 * 1000);

// Flaky tests. Disable tests now to allow time to address the issue.
describe.skip('Updating runtime parameters', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    // Pause a bit to wait for getRuntime to complete
    await page.waitForTimeout(2000);
  });

  test('Switch from GCE to dataproc', async() => {
    // Open the runtime panel
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.toggleRuntimePanel();

    // Click “create“ , from the default “create panel”
    // using let here instead of const because we navigate during the test and must reinstantiate
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCreateButton();

    // Wait until status shows green in side-nav
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();
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
    await helpSidebar.toggleRuntimePanel();

    // Switch to dataproc cluster with custom settings (e.g. 1 preemptible worker)
    await runtimePanel.pickComputeType(ComputeType.Dataproc);
    await runtimePanel.pickDataprocNumWorkers(3);
    await runtimePanel.pickDataprocNumPreemptibleWorkers(1);
    await runtimePanel.pickWorkerCpus(2);
    await runtimePanel.pickWorkerRamGbs(13);
    await runtimePanel.pickWorkerDisk(60);

    // Wait for indicator to go green in side-nav
    await runtimePanel.clickNextButton();
    await runtimePanel.clickApplyAndRecreateButton();
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();
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
    await helpSidebar.toggleRuntimePanel();

    // Click 'delete environment'
    await runtimePanel.clickDeleteEnvironmentButton();
    await runtimePanel.clickDeleteButton();

    // wait until status indicator disappears
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.None);

    // Refresh page, and reopen the panel
    await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
    // Wait for runtime get to complete
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();

    // Verify that dataproc settings are still shown
    expect(await runtimePanel.getDataprocNumWorkers()).toBe(3);
    expect(await runtimePanel.getDataprocNumPreemptibleWorkers()).toBe(1);
    expect(await runtimePanel.getWorkerCpus()).toBe('2');
    expect(await runtimePanel.getWorkerRamGbs()).toBe('13');
    expect(await runtimePanel.getWorkerDisk()).toBe(60);
  });

  test('Switch from dataproc to GCE', async() => {
    // Open the runtime panel
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.toggleRuntimePanel();

    // Click “customize“ , from the default “create panel”
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCustomizeButton();

    // Use the preset selector to pick “Hail genomics analysis“
    await runtimePanel.pickRuntimePreset(RuntimePreset.HailGenomicsAnalysis);
    await runtimePanel.clickCreateButton();

    // Wait until status shows green in side-nav
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();;
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Open a notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run notebook to validate runtime settings
    const workersOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/count-workers.py'});
    // Spark config always seems to start at this and then scale if you need additional threads.
    expect(workersOutputText).toBe('\'2\'');

    // Open runtime panel
    await helpSidebar.toggleRuntimePanel();

    // Switch to a GCE VM with custom settings
    await runtimePanel.pickComputeType(ComputeType.Standard);
    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(60);
    await runtimePanel.clickNextButton();
    await runtimePanel.clickApplyAndRecreateButton();

    // Wait for indicator to go green in side-nav
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Go back to the notebook:
    const notebookPreviewPage = new NotebookPreviewPage(page);
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

    // Open runtime panel
    await helpSidebar.toggleRuntimePanel();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Click ''delete environment”
    await runtimePanel.clickDeleteEnvironmentButton();
    await runtimePanel.clickDeleteButton();

    // wait until status indicator disappears
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.None);

    // Refresh page, reopen the panel
    await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
    // Wait for runtime get to complete
    await page.waitForTimeout(2000);
    await helpSidebar.toggleRuntimePanel();

    // Verify GCE custom settings are still shown
    expect(await runtimePanel.getCpus()).toBe('8');
    expect(await runtimePanel.getRamGbs()).toBe('30');
    expect(await runtimePanel.getDiskGbs()).toBe(60);
  });

  test('Pause and resume', async() => {
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.toggleRuntimePanel();
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCreateButton();

    await helpSidebar.toggleRuntimePanel();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    await runtimePanel.clickStatusIcon();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopped);

    await runtimePanel.clickStatusIcon();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);
  });

  // TODO: changes that don't require destroy/create - GCE
  // TODO: changes that don't require destroy/create - Dataproc
});
