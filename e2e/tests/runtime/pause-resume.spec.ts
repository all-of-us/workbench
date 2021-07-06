import RuntimePanel from 'app/component/runtime-panel';
import { config } from 'resources/workbench-config';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';

// 30 minutes. Test could take a long time.
jest.setTimeout(30 * 60 * 1000);

describe('Updating runtime status', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create, pause, resume, delete', async () => {
    await createWorkspace(page, { cdrVersion: config.ALTERNATIVE_CDR_VERSION });

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
