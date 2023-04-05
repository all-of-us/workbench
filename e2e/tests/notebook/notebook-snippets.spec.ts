import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import expect from 'expect';
import { Language } from 'app/text-labels';

describe('Notebook Snippets Tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eNotebookSnippetsTest';

  // regression test for RW-9725
  test.each([
    [
      Language.Python,
      'Python 3',
      // more exist, but these are the all-of-us-specific ones
      [
        'All of Us Dataset Builder Python snippets',
        'All of Us Python and Cloud Storage snippets',
        'All of Us Python and SQL snippets',
        'All of Us Cromwell Setup Python snippets'
      ]
    ],
    [
      Language.R,
      'R', // more exist, but these are the all-of-us-specific ones
      [
        'All of Us Dataset Builder R snippets',
        'All of Us R and Cloud Storage snippets',
        'All of Us R and SQL snippets',
        'All of Us Cromwell Setup snippets'
      ]
    ]
  ])('Create notebook and access %s snippets', async (language, expectedKernelName, expectedSnippetCategories) => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(makeRandomName('snippets'), language);

    // Verify kernel name.
    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe(expectedKernelName);

    const notebookIFrame = await notebook.getIFrame();

    const snippetsMenuXpath = '//*[@class="dropdown-toggle" and contains(normalize-space(text()), "Snippets")]';
    await notebookIFrame.waitForXPath(snippetsMenuXpath);

    const snippetsMenuListXpath = `${snippetsMenuXpath}/following-sibling::ul`;

    for (const snippetCategory of expectedSnippetCategories) {
      const categoryXpath = `${snippetsMenuListXpath}//*[contains(normalize-space(text()), "${snippetCategory}")]`;
      await notebookIFrame.waitForXPath(categoryXpath);
    }
  });
});
