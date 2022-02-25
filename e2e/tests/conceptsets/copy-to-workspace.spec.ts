import { Domain } from 'app/component/concept-domain-card';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import ConceptSetPage from 'app/page/conceptset-page';
import { SaveOption } from 'app/modal/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { Page } from 'puppeteer';
import expect from 'expect';

async function createConceptSet(page: Page): Promise<string> {
  const dataPage = new WorkspaceDataPage(page);
  const { conceptSearchPage, criteriaSearch } = await dataPage.openConceptSetSearch(Domain.Procedures);

  // Search by Procedure name.
  const procedureName = 'Surgery';
  await criteriaSearch.searchCriteria(procedureName);

  // Select first two rows.
  await criteriaSearch.resultsTableSelectRow(1, 1);
  await criteriaSearch.resultsTableSelectRow(2, 1);

  await conceptSearchPage.reviewAndSaveConceptSet();
  const conceptSetName = await conceptSearchPage.saveConceptSet(SaveOption.CreateNewSet);

  // Click on link to open Concept Set page.
  const conceptSetActionPage = new ConceptSetActionsPage(page);
  await conceptSetActionPage.openConceptSet(conceptSetName);
  return conceptSetName;
}

describe('Copy Concept Set to another workspace', () => {
  const defaultCdrVersionWorkspace = 'e2eTestCopyConceptSetToWorkspace'; // The copy-to workspace with default CDR version
  const srcWorkspace = 'e2eTestCopyConceptSetSrcWorkspace'; // workspace with default CDR version
  const oldCdrVersionWorkspace = 'e2eTestOldCdrWorkspace'; // workspace with old CDR version

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Copy Concept Set from one workspace to another workspace when both have the same CDR Version.
   */
  test('Can copy Concept Set when CDR Versions match', async () => {
    // Create a source and a destination workspace with the same CDR version name.
    await findOrCreateWorkspace(page, { workspaceName: defaultCdrVersionWorkspace });
    await findOrCreateWorkspace(page, { workspaceName: srcWorkspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const conceptSetName = await createConceptSet(page);

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    // Copy Concept Set to another workspace with new Concept name.
    const copyOfConceptSetName = makeRandomName();
    const conceptSetCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptSetCopyModal.copyToAnotherWorkspace(defaultCdrVersionWorkspace, copyOfConceptSetName);

    // Click "Go to Copied Concept Set" button.
    await conceptSetCopyModal.waitForButton(LinkText.GoToCopiedConceptSet).clickAndWait();

    // Verify destWorkspace is open.
    const url = page.url();
    expect(url).toContain(defaultCdrVersionWorkspace.toLowerCase());

    // Delete Concept Set in destWorkspace.
    await dataPage.deleteResource(copyOfConceptSetName, ResourceCard.ConceptSet);
  });

  /**
   * Test:
   * - Fail to Copy Concept Set from one workspace to another workspace when CDR Versions mismatch.
   */
  test('Cannot copy Concept Set when CDR Versions mismatch', async () => {
    // Create a source and a destination workspace with differing CDR Versions.
    await findOrCreateWorkspace(page, { workspaceName: defaultCdrVersionWorkspace });
    await findOrCreateWorkspace(page, {
      cdrVersion: config.OLD_CDR_VERSION_NAME,
      workspaceName: oldCdrVersionWorkspace
    });

    const conceptSetName = await createConceptSet(page);

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    const conceptCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptCopyModal.beginCopyToAnotherWorkspace(defaultCdrVersionWorkspace);

    const copyButton = conceptCopyModal.waitForButton(LinkText.Copy);
    await copyButton.expectEnabled(false);

    const cdrMismatchError = await conceptCopyModal.getCdrMismatchError();
    expect(cdrMismatchError).toMatch('Can’t copy to that workspace. It uses a different dataset version');

    await conceptCopyModal.waitForButton(LinkText.Close).click();
    await conceptCopyModal.waitUntilClose();
    await conceptSetPage.waitForLoad();
  });
});
