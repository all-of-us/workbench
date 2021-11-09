import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import path from 'path';

// 45 minutes. Test could take a long time.
jest.setTimeout(45 * 60 * 1000);

describe('egress suspension', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
  });

  const notebookName = makeRandomName('egress-notebook');
  const egressFilename = 'generate-egress.py';
  const egressFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${egressFilename}`);

  test('VM egress suspends user\'s compute', async () => {
    await findOrCreateWorkspace(page);

    const dataPage = new WorkspaceDataPage(page);
    const notebookPage = await dataPage.createNotebook(notebookName);

    await notebookPage.uploadFile(egressFilename, egressFilePath);
    await notebookPage.runCodeFile(1, egressFilename, 5 * 60 * 1000);

    await notebookPage.save();

    // Wait until we become suspended.
    await notebookPage.waitForSecuritySuspendedStatus(true);
  });
});
