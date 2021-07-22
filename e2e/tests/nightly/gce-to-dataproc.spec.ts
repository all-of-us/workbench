import RuntimePanel, { ComputeType } from 'app/component/runtime-panel';
import { config } from 'resources/workbench-config';
import { createWorkspace } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { ResourceCard } from 'app/text-labels';
import { withSignInTest } from 'libs/page-manager';

// This test could take a long time to run
jest.setTimeout(60 * 30 * 1000);
// Retry one more when fails
jest.retryTimes(0);

describe('Updating runtime compute type', () => {
  test('Switch from GCE to dataproc', async () => {
    await withSignInTest()(async (page) => {
      await createWorkspace(page, { cdrVersion: config.ALTERNATIVE_CDR_VERSION_NAME });

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
      const cpusOutputText = await notebook.runCodeCell(1, { codeFile: 'resources/python-code/count-cpus.py' });
      // Default CPU count is 4
      expect(parseInt(cpusOutputText, 10)).toBe(4);
      // This gets the amount of memory available to Python in bytes
      const memoryOutputText = await notebook.runCodeCell(2, { codeFile: 'resources/python-code/count-memory.py' });
      // Default memory is 15 gibibytes, we'll check that it is between 14GiB and 16GiB
      expect(parseFloat(memoryOutputText)).toBeGreaterThanOrEqual(14);
      expect(parseFloat(memoryOutputText)).toBeLessThanOrEqual(16);

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
      const workersOutputText = await notebook.runCodeCell(3, { codeFile: 'resources/python-code/count-workers.py' });
      // Spark config always seems to start at this and then scale if you need additional threads.
      expect(workersOutputText).toBe("'2'");
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
      await workspaceAnalysisPage.deleteWorkspace();
    });
  });
});
