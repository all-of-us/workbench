import WorkspaceDataPage from 'app/page/workspace-data-page';
import RuntimePanel from 'app/sidebar/runtime-panel';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';
import path from 'path';
import { twoRuntimesTimeout } from 'utils/timeout-constants';

jest.setTimeout(twoRuntimesTimeout);

describe('Updating runtime status', () => {
  // Notebooks to run before/after reattaching a PD.
  const diskBeforeNotebookName = 'disk-reattach-before.py';
  const diskBeforeNotebookFilePath = path.relative(
    process.cwd(),
    __dirname + `../../../resources/python-code/${diskBeforeNotebookName}`
  );

  const diskAfterNotebookName = 'disk-reattach-after.py';
  const diskAfterNotebookFilePath = path.relative(
    process.cwd(),
    __dirname + `../../../resources/python-code/${diskAfterNotebookName}`
  );

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create with detachable disk, re-attach, delete', async () => {
    await createWorkspace(page);

    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    await runtimePanel.getCustomizeButton().click();

    // Select detachable disk, start.
    await runtimePanel.pickDetachableDisk();
    await runtimePanel.createRuntime({ waitForComplete: false });

    // Run notebook to write a file to disk.
    let dataPage = new WorkspaceDataPage(page);
    let notebookPage = await dataPage.createNotebook(makeRandomName('disk-before'));
    await notebookPage.uploadFile(diskBeforeNotebookName, diskBeforeNotebookFilePath);
    let codeOutput = await notebookPage.runCodeFile(1, diskBeforeNotebookName);
    expect(codeOutput).toMatch(/success$/);

    // TODO(IA-3258): Remove this sleep once the ticket is resolved.
    // Due to IA-3258, files written within ~30s of runtime deletion are likely to
    // be truncated. Wait 90s to allow a wide margin for this bug.
    await page.waitForTimeout(90 * 1000);

    await notebookPage.save();
    dataPage = await notebookPage.goDataPage();

    // Delete the environment, but not the PD.
    await runtimePanel.open();
    await runtimePanel.deleteRuntime();

    // Create a new runtime, reattaching the PD and increasing the size.
    await runtimePanel.open();
    await runtimePanel.pickDetachableDisk();
    await runtimePanel.pickDetachableDiskGbs((await runtimePanel.getDetachableDiskGbs()) + 10);
    await runtimePanel.createRuntime({ waitForComplete: false });

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
