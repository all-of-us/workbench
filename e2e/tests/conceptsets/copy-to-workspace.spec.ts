import { Domain } from 'app/component/concept-domain-card';
import DataResourceCard from 'app/component/data-resource-card';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import ConceptSetPage from 'app/page/conceptset-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';

async function findOrCreateConceptSet(): Promise<{ conceptSetName: string }> {
  let conceptSetName: string;

  // Open Concept Sets tab.
  const dataPage = new WorkspaceDataPage(page);

  // Search for existing ConceptSet instead create new
  const existingConceptSetName = await dataPage.findConceptSetsCard();
  if (existingConceptSetName !== null) {
    conceptSetName = await existingConceptSetName.getResourceName();
    await existingConceptSetName.clickResourceName();
    return { conceptSetName };
  }

  // Create new Concept Set
  await dataPage.openConceptSetSearch(Domain.Procedures);
  conceptSetName = await dataPage.createDefaultProcedures('Radiologic examination');

  // Click on link to open Concept Set page.
  const conceptSetActionPage = new ConceptSetActionsPage(page);
  await conceptSetActionPage.openConceptSet(conceptSetName);
  return { conceptSetName };
}

describe('Copy Concept Set to another workspace', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const destWorkspace = 'e2eCopyConceptSetsDestinationWorkspaceTest';
  const srcWorkspace = 'e2eCopyConceptSetsSourceWorkspaceTest';
  const destWorkspaceWithAltCdr = 'e2eCopyConceptSetsAltCdrWorkspace';

  /**
   * Test:
   * - Copy Concept Set from one workspace to another workspace when both have the same CDR Version.
   */
  test('Can copy Concept Set when CDR Versions match', async () => {
    // Create a source and a destination workspace with the same CDR Version.
    await findOrCreateWorkspace(page, { workspaceName: destWorkspace });
    await findOrCreateWorkspace(page, { workspaceName: srcWorkspace });

    const { conceptSetName } = await findOrCreateConceptSet();

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    // Copy Concept Set to another workspace with new Concept name.
    const conceptSetCopyName = makeRandomName();
    const conceptSetCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptSetCopyModal.copyToAnotherWorkspace(destWorkspace, conceptSetCopyName);

    // Click "Go to Copied Concept Set" button.
    await conceptSetCopyModal.waitForButton(LinkText.GoToCopiedConceptSet).click();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    // Verify destWorkspace is open.
    const url = page.url();
    expect(url).toContain(destWorkspace.replace(/-/g, '').toLowerCase());

    const resourceCard = new DataResourceCard(page);
    const exists = await resourceCard.cardExists(conceptSetCopyName, ResourceCard.ConceptSet);
    expect(exists).toBe(true);

    console.log(
      `Copied Concept Set "${conceptSetName} from workspace "${srcWorkspace}" ` +
        `to Concept Set "${conceptSetCopyName}" in another workspace "${destWorkspace}"`
    );

    // Delete Concept Set in destWorkspace.
    await dataPage.deleteResource(conceptSetCopyName, ResourceCard.ConceptSet);
  });

  /**
   * Test:
   * - Fail to Copy Concept Set from one workspace to another workspace when CDR Versions mismatch.
   */
  test('Cannot copy Concept Set when CDR Versions mismatch', async () => {
    // Create a source workspace with differing CDR Versions.
    await findOrCreateWorkspace(page, {
      workspaceName: destWorkspaceWithAltCdr,
      cdrVersion: config.altCdrVersionName
    });

    await findOrCreateWorkspace(page, { workspaceName: srcWorkspace });
    const { conceptSetName } = await findOrCreateConceptSet();

    // Concept Set page is open.
    const conceptSetPage = new ConceptSetPage(page);
    await conceptSetPage.waitForLoad();

    const conceptCopyModal = await conceptSetPage.openCopyToWorkspaceModal(conceptSetName);
    await conceptCopyModal.beginCopyToAnotherWorkspace(destWorkspaceWithAltCdr, makeRandomName());

    const copyButton = conceptCopyModal.waitForButton(LinkText.Copy);
    expect(await copyButton.isCursorNotAllowed()).toBe(true);
  });
});
