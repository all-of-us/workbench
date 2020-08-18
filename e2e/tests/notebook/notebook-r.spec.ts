import DataPage from 'app/page/data-page';
import {Language} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  describe('Create new notebooks', () => {

    test('R notebook', async () => {
      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebookName = makeRandomName('r-notebook');
      const dataPage = new DataPage(page);
      const notebook = await dataPage.createNotebook(notebookName, Language.R);

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('R');

      await notebook.waitForKernelIdle();

      // Run few basic R calls in empty code cell [1]
      const code1 =
           'name <- c("Jon", "Bill", "Maria")\n' +
           'age <- c(1, 10, 20)\n' +
           'data <- data.frame(name, age)\n' +
           'print(max(data$age))';
      const code1Output = await notebook.runCodeCell(1, {code: code1});

      const code2 = 'sessionInfo()';
      const code2Output = await notebook.runCodeCell(2, {code: code2});

      // Delete R notebook
      await notebook.deleteNotebook(notebookName);

      await expect(code1Output).toMatch(/20/);
      // R version is 3.6.2 at time of this writing. It can change.
      await expect(code2Output).toMatch(/R version 3.6.2/);
    })

  })

});
