import {findWorkspace, signIn} from 'utils/test-utils';
import DataPage from 'app/page/data-page';
import {makeRandomName} from 'utils/str-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook Python pyplot test', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Simple pyplot output is image', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    const notebookName = makeRandomName('pyplot');
    const notebook = await dataPage.createNotebook(notebookName);
    const output =  await notebook.runCodeCell(1, {codeFile: 'resources/python-code/simple-pyplot.py'});

    const cell = await notebook.findCell(1);
    const cellOutput = await cell.findOutputElementHandle();
    const [imgElement] = await cellOutput.$x('./img[@src]');

    await notebook.deleteNotebook(notebookName);

    expect(output).toEqual(''); // generated pyplot is a image, no texts.
    expect(imgElement).toBeTruthy(); // output should be a img.
  });

});
