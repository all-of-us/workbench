import RuntimePanel, {StartStopIconState} from 'app/component/runtime-panel';
import {config} from 'resources/workbench-config';
import {createWorkspace, signInWithAccessToken} from 'utils/test-utils';

// 30 minutes. Test could take a long time.
jest.setTimeout(30 * 60 * 1000);

describe('Updating runtime', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create, pause, resume, delete', async() => {

    await createWorkspace(page, config.altCdrVersionName).then(card => card.clickWorkspaceName());

    const runtimePanel = new RuntimePanel(page);

    // Create runtime
    await runtimePanel.createRuntime();

    // Pause runtime
    await runtimePanel.open();
    await runtimePanel.clickStatusIcon();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    expect(await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopped)).toBe(true);
    console.log(`runtime is paused`);

    // Restart runtime
    await runtimePanel.clickStatusIcon();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    expect(await runtimePanel.waitForStartStopIconState(StartStopIconState.Running)).toBe(true);
    await runtimePanel.close();
    console.log(`runtime is resumed`);

    // Delete runtime
    await runtimePanel.deleteRuntime();
  });

});
