import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Language} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findOrCreateWorkspace, signIn} from 'utils/test-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests in R language', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Run code from file', async () => {
    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const notebookName = makeRandomName('r-notebook');
    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('R');

    // Run math function in Code cell [1].
    let cellIndex = 1;
    const code1Output = await notebook.runCodeCell(cellIndex,
        {codeFile: 'resources/r-code/calculate-max.R', markdownWorkaround: true});
    expect(code1Output).toEqual('[1] 20');

    // Print sys environment details in Code cell [2].
    cellIndex = 2;
    const code2Output = await notebook.runCodeCell(cellIndex,
        {codeFile: 'resources/r-code/sys-print.R', markdownWorkaround: true});
    expect(code2Output).toContain('success');

    // Import R libs in Code cell [3].
    cellIndex = 3;
    const cell3Output = await notebook.runCodeCell(cellIndex,
        {codeFile: 'resources/r-code/import-libs.R', markdownWorkaround: true, timeOut: 5 * 60 * 1000});
    expect(cell3Output).toContain('success');

    // Delete R notebook
    await notebook.deleteNotebook(notebookName);
  }, 30 * 60 * 1000)

});
