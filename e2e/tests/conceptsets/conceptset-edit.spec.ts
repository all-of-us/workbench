import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
// import DataResourceCard, {CardType} from 'app/component/data-resource-card';
// import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import ConceptsetActionsPage from 'app/page/conceptset-actions-page';
import ConceptsetPage from 'app/page/conceptset-page';
import {SaveOption} from 'app/page/conceptset-save-modal';
import DataPage from 'app/page/data-page';
// import {EllipsisMenuAction, LinkText, WorkspaceAccessLevel} from 'app/text-labels';
// import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';


describe('Editing Concept Sets', () => {

  beforeEach(async () => {
    await signIn(page);
  });

 /**
  * Test:
  * - Create new workspace.
  * - Create first new Concept Set in Procedure domain.
  * - Create second new Concept Set in Procedure domain. Add to first Concept Set.
  */
  test.skip('Add to existing Concept Set', async () => {
    // Create a workspace
    const workspaceCard = await findWorkspace(page, true);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button.
    const dataPage = new DataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Click Add Concept Sets button.
    const conceptSearchPage = await datasetBuildPage.clickAddConceptSetsButton();

    // Add a Concept in Procedures domain
    let procedures = await ConceptDomainCard.findDomainCard(page, Domain.Procedures);
    await procedures.clickSelectConceptButton();

    // Select first two rows.
    const row1 = await conceptSearchPage.dataTableSelectRow(1, 1);
    const row2 = await conceptSearchPage.dataTableSelectRow(3, 1);

    console.log('Selected Procedures table row 1: ', row1);
    console.log('Selected Procedures table row 2: ', row2);

    // Verify Code are numberical values
    expect(Number.isNaN(parseInt(row1.code, 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.code, 10))).toBe(false);

    // Verify Participant Count are numberical values
    expect(Number.isNaN(parseInt(row1.participantCount.replace(/,/g, ''), 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.participantCount.replace(/,/g, ''), 10))).toBe(false);

    // Verify "Add to Set" dynamic button text label.
    let addButtonLabel = await conceptSearchPage.clickAddToSetButton();
    expect(addButtonLabel).toBe('Add (2) to set');

    // Save new Concept Set.
    const conceptName = await conceptSearchPage.saveConcept(SaveOption.CreateNewSet);
    console.log(`Created Concept Set: "${conceptName}"`);

    // Add another Concept in Procedures domain.
    const conceptsetActionPage = new ConceptsetActionsPage(page);
    await conceptsetActionPage.clickCreateAnotherConceptSetButton();

    procedures = await ConceptDomainCard.findDomainCard(page, Domain.Procedures);
    await procedures.clickSelectConceptButton();

    // Search in Procedures domains
    const searchWords = 'Screening for disorder';
    await conceptSearchPage.searchConcepts(`"${searchWords}"`); // needs double-quotes or search fails.

    // Select first row. Its name cell should match the search words.
    const row = await conceptSearchPage.dataTableSelectRow(1, 1);
    expect(row.name).toBe(searchWords);

    addButtonLabel = await conceptSearchPage.clickAddToSetButton();
    expect(addButtonLabel).toBe('Add (1) to set');

    // Save to Existing Set: Only one Concept set and it is the new Concept Set created earlier in same workspace.
    const existingConceptName = await conceptSearchPage.saveConcept(SaveOption.ChooseExistingSet);
    expect(existingConceptName).toBe(conceptName);
    console.log(`Added to existing Concept Set "${conceptName}"`);

    // Concept Set saved. Click Concept Set link.
    const link = new Link(page, `//a[text()="${conceptName}"]`);
    await link.clickAndWait();

    // Land in Concept Set page.
    const conceptsetPage = new ConceptsetPage(page);
    await conceptsetPage.waitForLoad();

    // Verify Concept Set name displayed
    const xpath = `//*[@data-test-id="concept-set-title"]`;
    const title = await page.waitForXPath(xpath, {visible: true});
    const value = await (await title.getProperty('innerText')).jsonValue();
    expect(value.toString()).toBe(conceptName);
  });

  /**
   * Test:
   * - Use existing workspaces and use existing Concept Set when possible. Otherwise create new workspaces and new Concept Sets.
   */
  /*
  test('Copy Concept Set to another workspace', async () => {

    const workspaceCard = new WorkspaceCard(page);
    const allWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.Owner);

    let workspace1: WorkspaceCard;
    let workspace2: WorkspaceCard;

    if (allWorkspaces.length > 1) {
      workspace1 = allWorkspaces[0];
      workspace2 = allWorkspaces[1];
    } else {
      // Create new workspaces.
      workspace1 = await findWorkspace(page, true);
      workspace2 = await findWorkspace(page, true);
    }

    await workspace2.clickWorkspaceName();

    const dataPage = new DataPage(page);
    await dataPage.openTab(TabLabelAlias.ConceptSets, {waitPageChange: false});

    const dataCards = new DataResourceCard(page);
    const conceptCards = await dataCards.getResourceCard(CardType.ConceptSet);

    if (conceptCards.length === 0) {
      // Create new Concept Set

    } else {
      const nameLink = await conceptCards[0].getLink();
      await nameLink.click();
    }

    const conceptsetPage = new ConceptsetPage(page);
      await conceptsetPage.waitForLoad();

      const ellipsis = conceptsetPage.getEllipsisMenu(conceptName);
      await ellipsis.clickAction(EllipsisMenuAction.CopyToAnotherWorkspace, {waitForNav: false});

      const copiedConceptName = makeRandomName();
      const conceptCopyModal = await conceptsetPage.openCopyToWorkspaceModal(conceptName);
      await conceptCopyModal.copyToAnotherWorkspace()

      await conceptCopyModal.waitForButton(LinkText.GoToCopiedConceptSet);


  });
*/

});
