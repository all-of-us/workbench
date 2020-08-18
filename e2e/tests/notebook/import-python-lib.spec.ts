import {findWorkspace, signIn} from 'utils/test-utils';
import DataPage from 'app/page/data-page';
import {makeRandomName} from 'utils/str-utils';
import {config} from 'resources/workbench-config';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Import Python libraries', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Import os', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    const notebookName = makeRandomName('import-os');
    const notebook = await dataPage.createNotebook(notebookName);
    const outputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/import-os.py'});

    await notebook.deleteNotebook(notebookName);

    // Partial values check: UserEmail and python3 path is expected to be printed out.
    // rest of output strings vary per workspace.
    const partialArray = [
      '/usr/local/bin/python3',
      config.userEmail,
    ];
    const outputTextArray = outputText.split(/\n/);
    expect(outputTextArray).toEqual(expect.arrayContaining(partialArray));
  });

  test('Import common lib', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    const notebookName = makeRandomName('import-lib');
    const notebook = await dataPage.createNotebook(notebookName);
    const outputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/import-libs.py'});

    await notebook.deleteNotebook(notebookName);

    expect(outputText).toEqual('success');
  });

});
