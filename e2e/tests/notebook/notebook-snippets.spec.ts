import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Notebook Snippets Tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eNotebookSnippetsTest';
  const pyNotebookName = makeRandomName('snippets');

  // more exist, but these are the all-of-us-specific ones
  const expectedSnippetCategories = [
    'All of Us Dataset Builder Python snippets',
    'All of Us Python and Cloud Storage snippets',
    'All of Us Python and SQL snippets',
    'All of Us Cromwell Setup Python snippets'
  ];

  test('Create notebook and access Python snippets', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(pyNotebookName);

    // Verify kernel name.
    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('Python 3');

    const notebookIFrame = await notebook.getIFrame();

    const snippetsMenuXpath = '//*[@class="dropdown-toggle" and contains(normalize-space(text()), "Snippets")]';
    await notebookIFrame.waitForXPath(snippetsMenuXpath);

    for (const snippetCategory of expectedSnippetCategories) {
      const categoryXpath =
        `${snippetsMenuXpath}/following-sibling::ul` + `//*[contains(normalize-space(text()), "${snippetCategory}")]`;
      await notebookIFrame.waitForXPath(categoryXpath);
    }
  });
});
