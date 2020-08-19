import DataPage from 'app/page/data-page';
import {Language} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests in R language', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Run code from file', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const notebookName = makeRandomName('r-notebook');
    const dataPage = new DataPage(page);
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('R');

    // Run math function in code cell [1]
    const code1Output = await notebook.runCodeCell(1, {codeFile: 'resources/r-code/calculate-max.R'});
    expect(code1Output).toEqual(20);

    // Print sys environment details in code cell [2]
    const code2Output = await notebook.runCodeCell(2, {codeFile: 'resources/r-code/sys-print.R'});
    expect(code2Output).toContain('success');

    // Delete R notebook
    await notebook.deleteNotebook(notebookName);
  })

});
