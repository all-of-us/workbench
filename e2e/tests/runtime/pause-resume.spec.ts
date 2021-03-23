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
    await createWorkspace(page, config.altCdrVersionName).then((card) => card.clickWorkspaceName());

    const runtimePanel = new RuntimePanel(page);

    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);

    // Pause runtime
    await runtimePanel.pauseRuntime();
    await page.waitForTimeout(2000);

    // Restart runtime
    await runtimePanel.resumeRuntime();
    await page.waitForTimeout(2000);

    // Delete runtime
    await runtimePanel.deleteRuntime();
  });
});
