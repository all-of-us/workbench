import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import RuntimePanel from 'app/sidebar/runtime-panel';
import path from 'path';
import { waitForSecuritySuspendedStatus } from 'utils/runtime-utils';
import { logger } from 'libs/logger';
import fs from 'fs-extra';

// 45 minutes. Test could take a long time.
jest.setTimeout(45 * 60 * 1000);

describe('egress suspension', () => {
  let notebookUrl: string;

  const notebookName = makeRandomName('egress-notebook');
  const dataGenFilename = 'create-data-files.py';
  const dataGenFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${dataGenFilename}`);
  const downloadPath = path.resolve('logs/download');

  beforeEach(async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
    await (await page.target().createCDPSession()).send('Page.setDownloadBehavior', {
      behavior: 'allow',
      downloadPath
    });
    await fs.ensureDir(downloadPath);
  });

  test("VM egress suspends user's compute", async () => {
    await findOrCreateWorkspace(page);

    const dataPage = new WorkspaceDataPage(page);
    const notebookPage = await dataPage.createNotebook(notebookName);

    await notebookPage.uploadFile(dataGenFilename, dataGenFilePath);

    // Generates 6 files currently. A single large file may time out.
    logger.info('Generating 30MB files for download');
    await notebookPage.runCodeFile(1, dataGenFilename, 60 * 1000);
    await notebookPage.save();

    // Download these 30MB files one-by-one from the file tree, generating ~180MB egress
    const treePage = await notebookPage.selectFileOpenMenu();
    // Allow some time for Jupyter extensions to load. Without this, download modal may fail to show.
    // TODO(RW-8114): Try waitForNetworkIdle here instead.
    await treePage.waitForTimeout(5000);
    for (let i = 0; i < 6; i++) {
      const f = `data${i}.txt`;
      logger.info(`Downloading ${f} to generate egress`);
      await notebookPage.downloadFileFromTree(treePage, f);
      console.log(`${downloadPath}/${f}`);
      expect(fs.existsSync(`${downloadPath}/${f}`)).toBe(true);
    }
    await treePage.close();

    logger.info('Awaiting security suspension in the notebook page');
    await page.bringToFront();
    notebookUrl = page.url();
    await notebookPage.waitForLoad();

    await waitForSecuritySuspendedStatus(page);
  });

  test('Cloud Analysis Environment is auto suspended', async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
    await page.goto(notebookUrl, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'], timeout: 60 * 1000 });
    await waitForSecuritySuspendedStatus(page);

    // egress handling will auto. suspend cloud analysis environment.
    const runtimePanel = new RuntimePanel(page);
    expect(await runtimePanel.isRuntimeSuspended()).toBe(true);
  });
});
