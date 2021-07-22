import RuntimePanel from 'app/component/runtime-panel';
import { config } from 'resources/workbench-config';
import { createWorkspace } from 'utils/test-utils';
import { withSignInTest } from 'libs/page-manager';

// 30 minutes. Test could take a long time.
jest.setTimeout(30 * 60 * 1000);

describe('Updating runtime status', () => {
  test('Create, pause, resume, delete', async () => {
    await withSignInTest()(async (page) => {
      await createWorkspace(page, { cdrVersion: config.ALTERNATIVE_CDR_VERSION_NAME });

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
  });
});
