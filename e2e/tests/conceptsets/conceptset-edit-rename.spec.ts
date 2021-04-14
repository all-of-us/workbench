import ConceptDomainCard, { Domain } from 'app/component/concept-domain-card';
import Link from 'app/element/link';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import { SaveOption } from 'app/modal/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { ResourceCard } from 'app/text-labels';
import { makeRandomName, makeString } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';

describe('Editing and rename Concept Set', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eEditConceptSetsTest';
  /**
   * Test:
   * - Create new workspace.
   * - Create first new Concept Set in Procedure domain.
   * - Add new concept to first (existing) Concept Set.
   * - Rename Concept Set.
   * - Delete Concept Set.
   */
  test('Workspace OWNER can edit Concept Set', async () => {
    const workspaceName = await createWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openConceptSetSearch(Domain.Procedures);

    // Create new Concept Set
    const conceptSetName = await dataPage.createDefaultProcedures('Radiologic examination');

    // Add another Concept in Procedures domain.
    const conceptSetActionsPage = new ConceptSetActionsPage(page);
    const conceptSearchPage = await conceptSetActionsPage.openConceptSearch();

    const procedures = ConceptDomainCard.findDomainCard(page, Domain.Procedures);
    const criteriaSearch = await procedures.clickSelectConceptButton();

    // Search in Procedures domain
    const procedureName = 'Screening procedure';
    await criteriaSearch.searchCriteria(procedureName);
    // Select first row. Its name cell should match the search words.
    const row = await criteriaSearch.resultsTableSelectRow();
    expect(row.name).toBe(procedureName);
    await conceptSearchPage.reviewAndSaveConceptSet();

    // Save to Existing Set: Only one Concept set and it is the new Concept Set created earlier in same workspace.
    const existingConceptSetName = await conceptSearchPage.saveConceptSet(SaveOption.ChooseExistingSet);
    expect(existingConceptSetName).toBe(conceptSetName);
    console.log(`Added new Concept to existing Concept Set "${conceptSetName}"`);

    // Concept Set saved. Click Concept Set link. Land on Concept Set page.
    const conceptSetPage = await conceptSetActionsPage.openConceptSet(conceptSetName);

    // Verify Concept Set name is displayed.
    const value = await conceptSetPage.getConceptSetName();
    expect(value).toBe(conceptSetName);

    // Edit Concept name and description.
    const newName = makeRandomName();
    await conceptSetPage.edit(newName, makeString(20));
    console.log(`Renamed Concept Set: "${conceptSetName}" to "${newName}"`);

    // Navigate to workspace Data page, then delete Concept Set
    await new Link(page, `//a[text()="${workspaceName}"]`).click();
    await dataPage.waitForLoad();
    await dataPage.openConceptSetsSubtab();
    await dataPage.deleteResource(newName, ResourceCard.ConceptSet);
  });
});
