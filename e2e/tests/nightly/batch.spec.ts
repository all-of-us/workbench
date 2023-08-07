import WorkspaceDataPage from 'app/page/workspace-data-page';
import { AccessTierDisplayNames } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';
import path from 'path';

jest.setTimeout(30 * 60 * 1000);

describe.skip('Batch workflow support', () => {
  const pyFilename = 'batch.py';
  const pyFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${pyFilename}`);

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  //TODO: Refactor to contract test(s) https://precisionmedicineinitiative.atlassian.net/browse/RW-9658
  test.skip('batch jobs have restricted network access', async () => {
    await createWorkspace(page, {
      cdrVersionName: config.CONTROLLED_TIER_CDR_VERSION_NAME,
      dataAccessTier: AccessTierDisplayNames.Controlled
    });

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('batch');
    const notebook = await dataPage.createNotebook(notebookName);

    await notebook.uploadFile(pyFilename, pyFilePath);
    const outputText = await notebook.runCodeFile(1, pyFilename);

    expect(outputText).toMatch(/success$/);

    await notebook.deleteRuntime();
  });
});
