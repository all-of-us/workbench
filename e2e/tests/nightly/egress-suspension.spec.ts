import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import path from 'path';
import { waitForSecuritySuspendedStatus } from 'utils/runtime-utils';
import { logger } from 'libs/logger';

// 45 minutes. Test could take a long time.
jest.setTimeout(45 * 60 * 1000);

describe('egress suspension', () => {
  let notebookUrl: string;

  beforeEach(async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
  });

  const notebookName = makeRandomName('egress-notebook');
  const dataGenFilename = 'create-data-files.py';
  const dataGenFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${dataGenFilename}`);

  test("VM egress suspends user's compute", async () => {
    await findOrCreateWorkspace(page);

    const dataPage = new WorkspaceDataPage(page);
    const notebookPage = await dataPage.createNotebook(notebookName);

    await notebookPage.uploadFile(dataGenFilename, dataGenFilePath);

    // Generates 6 files currently. A single large file may time out.
    logger.info('Generating 30MB files for download');
    await notebookPage.runCodeFile(1, dataGenFilename, 60 * 1000);

    // Download these 30MB files one-by-one from the file tree, generating ~180MB egress
    const treePage = await notebookPage.selectFileOpenMenu();
    // Allow some time for Jupyter extensions to load. Without this, download modal may fail to show.
    // TODO(RW-8114): Try waitForNetworkIdle here instead.
    await treePage.waitForTimeout(5000);
    for (let i = 0; i < 6; i++) {
      const f = `data${i}.txt`;
      logger.info(`Downloading ${f} to generate egress`);
      await notebookPage.downloadFileFromTree(treePage, f);
      // Short pause after download file to simulate real user behavior
      // Very fast test playback speed doesn't seems to reliably trigger egress event
      await page.waitForTimeout(5000);
    }
    await treePage.close();

    logger.info('Awaiting security suspension in notebook page');
    await page.bringToFront();
    notebookUrl = page.url();
    await notebookPage.waitForLoad();

    await waitForSecuritySuspendedStatus(page);
  });

  test('Verify analysis environment is suspended due to security egress suspension', async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
    await page.goto(notebookUrl, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'], timeout: 60 * 1000 });
    await waitForSecuritySuspendedStatus(page);
  });
});
