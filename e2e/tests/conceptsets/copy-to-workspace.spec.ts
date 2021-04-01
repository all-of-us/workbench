import { Domain } from 'app/component/concept-domain-card';
import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceCard from 'app/component/workspace-card';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import ConceptSetPage from 'app/page/conceptset-page';
import { SaveOption } from 'app/modal/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';

async function createConceptSet(srcWorkspaceCard: WorkspaceCard) {
  // Open Source Workspace Data Page.
  await srcWorkspaceCard.clickWorkspaceName();
  // Open Concept Sets tab.
  const dataPage = new WorkspaceDataPage(page);
  await dataPage.openConceptSetsSubtab();

  // Create new Concept Set
  const { conceptSearchPage, criteriaSearch } = await dataPage.openConceptSetSearch(Domain.Procedures);

  // Search by Procedure name.
  const procedureName = 'Radiologic examination';
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

describe('Copy Concept Set to another workspace', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Copy Concept Set from one workspace to another workspace when both have the same CDR Version.
   */
  test('Workspace OWNER can copy Concept Set when CDR Versions match', async () => {
    // Create a source and a destination workspace with the same CDR Version.

    const [, destWorkspace] = await createWorkspace(page, {
      openDataPage: false
    });

    const [srcWorkspaceCard, srcWorkspace] = await createWorkspace(page, {
      openDataPage: false
    });

    const { dataPage, conceptSetName } = await createConceptSet(srcWorkspaceCard);

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    // Copy Concept Set to another workspace with new Concept name.

    const conceptSetCopyName = makeRandomName();

    const conceptSetCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptSetCopyModal.copyToAnotherWorkspace(destWorkspace, conceptSetCopyName);

    // Click "Go to Copied Concept Set" button.
    await conceptSetCopyModal.waitForButton(LinkText.GoToCopiedConceptSet).then((butn) => butn.click());

    await dataPage.waitForLoad();

    // Verify destWorkspace is open.
    const url = page.url();
    expect(url).toContain(destWorkspace.replace(/-/g, ''));

    const resourceCard = new DataResourceCard(page);
    const exists = await resourceCard.cardExists(conceptSetCopyName, ResourceCard.ConceptSet);
    expect(exists).toBe(true);

    console.log(
      `Copied Concept Set "${conceptSetName} from workspace "${srcWorkspace}" to Concept Set "${conceptSetCopyName}" in another workspace "${destWorkspace}"`
    );

    // Delete Concept Set in destWorkspace.
    await dataPage.deleteResource(conceptSetCopyName, ResourceCard.ConceptSet);
  });

  /**
   * Test:
   * - Fail to Copy Concept Set from one workspace to another workspace when CDR Versions mismatch.
   */
  test('Workspace OWNER cannot copy Concept Set when CDR Versions mismatch', async () => {
    // Create a source and a destination workspace with differing CDR Versions.

    const [, destWorkspace] = await createWorkspace(page, {
      openDataPage: false
    });

    const [srcWorkspaceCard] = await createWorkspace(page, {
      cdrVersion: config.altCdrVersionName,
      openDataPage: false
    });

    const { conceptSetName } = await createConceptSet(srcWorkspaceCard);

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    const conceptCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptCopyModal.beginCopyToAnotherWorkspace(destWorkspace, makeRandomName());

    const copyButton = await conceptCopyModal.waitForButton(LinkText.Copy);
    expect(await copyButton.isCursorNotAllowed()).toBe(true);
  });
});
