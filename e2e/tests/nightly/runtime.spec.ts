import WorkspaceDataPage from 'app/page/workspace-data-page';
import RuntimePanel from 'app/sidebar/runtime-panel';
import path from 'path';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';

// 30 minutes. Test could take a long time.
jest.setTimeout(30 * 60 * 1000);

describe('Updating runtime status', () => {

  // Notebooks to run before/after reattaching a PD.
  const diskBeforeNotebookName = 'disk-reattach-before.py';
  const diskBeforeNotebookFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${diskBeforeNotebookName}`);

  const diskAfterNotebookName = 'disk-reattach-after.py';
  const diskAfterNotebookFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${diskAfterNotebookName}`);

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create, pause, resume, delete', async () => {
    await createWorkspace(page, { cdrVersionName: config.OLD_CDR_VERSION_NAME });

    const runtimePanel = new RuntimePanel(page);

    // Create runtime
    await runtimePanel.createRuntime();

    // Pause runtime
    await runtimePanel.pauseRuntime();

    // Restart runtime
    await runtimePanel.resumeRuntime();

    // Delete runtime
    await runtimePanel.deleteRuntime();
  });

  test('Create with detachable disk, re-attach, delete', async () => {
    await createWorkspace(page);

    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    await runtimePanel.getCustomizeButton().click();

    // Select detachable disk, start.
    await runtimePanel.pickDetachableDisk();
    await runtimePanel.createRuntime({waitForComplete: false});

    // Run notebook to write a file to disk.
    let dataPage = new WorkspaceDataPage(page);
    let notebookPage = await dataPage.createNotebook(makeRandomName('disk-before'));
    await notebookPage.uploadFile(diskBeforeNotebookName, diskBeforeNotebookFilePath);
    let codeOutput = await notebookPage.runCodeFile(1, diskBeforeNotebookName);
    expect(codeOutput).toMatch(/success$/);

    await notebookPage.save();
    dataPage = await notebookPage.goDataPage();

    // Select increase detachable disk, enable GPU to force a recreate.
    await runtimePanel.open();
    await runtimePanel.pickDetachableDiskGbs(await runtimePanel.getDetachableDiskGbs() + 10);
    await runtimePanel.pickEnableGpu();
    await runtimePanel.applyChanges();

    // Run notebook to verify file is still on disk; check new disk size.
    notebookPage = await dataPage.createNotebook(makeRandomName('disk-after'));
    await notebookPage.uploadFile(diskAfterNotebookName, diskAfterNotebookFilePath);
    codeOutput = await notebookPage.runCodeFile(1, diskAfterNotebookName);
    expect(codeOutput).toMatch(/success$/);

    // Delete runtime, then delete disk
    await runtimePanel.open();
    await runtimePanel.deleteRuntime();
    await runtimePanel.deleteUnattachedPd();
  });
});
