import { Domain } from 'app/component/concept-domain-card';
import DataResourceCard from 'app/component/card/data-resource-card';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import ConceptSetPage from 'app/page/conceptset-page';
import { SaveOption } from 'app/modal/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';

async function createConceptSet(): Promise<{ dataPage: WorkspaceDataPage; conceptSetName: string }> {
  const dataPage = new WorkspaceDataPage(page);

  // Create new Concept Set
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

  return { dataPage, conceptSetName };
}

describe.skip('Copy Concept Set to another workspace', () => {
  const destWorkspace = 'e2eTestCopyConceptSetToWorkspaceV4'; // The copy-to workspace with default CDR version

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Copy Concept Set from one workspace to another workspace when both have the same CDR Version.
   */
  test('Workspace OWNER can copy Concept Set when CDR Versions match', async () => {
    // Create a source and a destination workspace with the same CDR version name.
    await findOrCreateWorkspace(page, { workspaceName: destWorkspace, openDataPage: false });
    const srcWorkspace = await createWorkspace(page);

    const { dataPage, conceptSetName } = await createConceptSet();

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    // Copy Concept Set to another workspace with new Concept name.

    const conceptSetCopyName = makeRandomName();

    const conceptSetCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptSetCopyModal.copyToAnotherWorkspace(destWorkspace, conceptSetCopyName);

    // Click "Go to Copied Concept Set" button.
    await conceptSetCopyModal.waitForButton(LinkText.GoToCopiedConceptSet).click();

    await dataPage.waitForLoad();

    // Verify destWorkspace is open.
    const url = page.url();
    expect(url).toContain(destWorkspace.toLowerCase());

    const resourceCard = new DataResourceCard(page);
    const exists = await resourceCard.resourceExistsInTable(conceptSetCopyName);
    expect(exists).toBe(true);

    console.log(
      `Copied Concept Set "${conceptSetName} from workspace "${srcWorkspace}" ` +
        `to Concept Set "${conceptSetCopyName}" in another workspace "${destWorkspace}"`
    );

    // Delete Concept Set in destWorkspace.
    await dataPage.deleteResourceFromTable(conceptSetCopyName, ResourceCard.ConceptSet);
  });

  /**
   * Test:
   * - Fail to Copy Concept Set from one workspace to another workspace when CDR Versions mismatch.
   */
  test('Workspace OWNER cannot copy Concept Set when CDR Versions mismatch', async () => {
    // Create a source and a destination workspace with differing CDR Versions.
    await findOrCreateWorkspace(page, { workspaceName: destWorkspace, openDataPage: false });
    await createWorkspace(page, {
      cdrVersionName: config.OLD_CDR_VERSION_NAME
    });

    const { conceptSetName } = await createConceptSet();

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    const conceptCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptCopyModal.beginCopyToAnotherWorkspace(destWorkspace, makeRandomName());

    const copyButton = conceptCopyModal.waitForButton(LinkText.Copy);
    expect(await copyButton.isCursorNotAllowed()).toBe(true);
  });
});
