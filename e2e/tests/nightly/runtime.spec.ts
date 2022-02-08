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

  test.only('Create with detachable disk, re-attach, delete', async () => {
    await createWorkspace(page, { cdrVersionName: config.DEFAULT_CDR_VERSION_NAME });

    const runtimePanel = new RuntimePanel(page);

    // Select detachable disk, start.
    await runtimePanel.pickDetachableDisk();
    await runtimePanel.createRuntime({waitForComplete: false});

    // Run notebook to write a file to disk.
    let dataPage = new WorkspaceDataPage(page);
    let notebookPage = await dataPage.createNotebook(makeRandomName('disk-before'));
    await notebookPage.uploadFile(diskBeforeNotebookName, diskBeforeNotebookFilePath);
    await notebookPage.runCodeFile(1, diskBeforeNotebookName);

    // Select increase detachable disk, enable GPU to force a recreate.
    await runtimePanel.open();
    await runtimePanel.pickDetachableDiskGbs(await runtimePanel.getDetachableDiskGbs() + 10);
    await runtimePanel.pickEnableGpu();
    await runtimePanel.applyChanges();

    // Run notebook to verify file is still on disk; check new disk size.
    dataPage = new WorkspaceDataPage(page);
    notebookPage = await dataPage.createNotebook(makeRandomName('disk-after'));
    await notebookPage.uploadFile(diskAfterNotebookName, diskAfterNotebookFilePath);
    await notebookPage.runCodeFile(1, diskAfterNotebookName);

    // Delete runtime, then delete disk
    await runtimePanel.deleteRuntime();
    await runtimePanel.deleteUnattachedPd();
  });
});
