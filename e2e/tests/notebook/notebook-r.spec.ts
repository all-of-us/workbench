import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
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
      expect.assertions(5);

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebookName = makeRandomName('test-notebook');
      const dataPage = new DataPage(page);
      const notebook = await dataPage.createNotebook(notebookName, Language.R);

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('R');

      const kernelState = await notebook.waitForKernelIdle();
      expect(kernelState).toBe(true);

      // Run few basic R calls in empty code cell [1]
      const code1 =
           'name <- c("Jon", "Bill", "Maria")\n' +
           'age <- c(1, 10, 20)\n' +
           'data <- data.frame(name, age)\n' +
           'print(max(data$age))';
      const code1Output = await notebook.runCodeCell(1, {code: code1});
      expect(code1Output).toMatch(/20/);

      const code2 = 'sessionInfo()';
      const code2Output = await notebook.runCodeCell(2, {code: code2});
      // R version is 3.6.2 at time of this writing. It can change.
      expect(code2Output).toMatch(/R version 3.6.2/);

      // Delete R notebook
      const analysisPage = await notebook.goBackAnalysisPage();

      await dataPage.openTab(TabLabelAlias.Analysis);
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
      expect(notebookCard).toBeTruthy();

      await analysisPage.deleteNotebook(notebookName);
    })


  })

});
