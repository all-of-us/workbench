import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace } from 'utils/test-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import { MenuOption, ResourceCard } from 'app/text-labels';
import DataResourceCard from 'app/component/data-resource-card';
import Link from 'app/element/link';
import { withSignInTest } from 'libs/page-manager';

describe('Editing Cohort tests', () => {
  // Tests require one and same workspace
  const workspace = makeWorkspaceName();
  let workspaceUrl;

  test('Discard changes', async () => {
    await withSignInTest()(async (page) => {
      await findAWorkspace();

      const dataPage = new WorkspaceDataPage(page);
      const cohortCard = await dataPage.findOrCreateCohort();
      const cohortName = await cohortCard.getResourceName();
      await cohortCard.clickResourceName();

      const cohortBuildPage = new CohortBuildPage(page);
      await cohortBuildPage.waitForLoad();

      // Wait for Total Count that also indicates page load is ready.
      await cohortBuildPage.getTotalCount();

      // Switch on Temporal in Group 1.
      const group1 = cohortBuildPage.findIncludeParticipantsGroupByIndex();
      await group1.clickTemporalSwitch(true);

      // Click Data tab, Warning (Discard Changes) modal should open.
      await dataPage.openDataPage({ waitPageChange: false });

      await cohortBuildPage.discardChangesConfirmationDialog();

      // Back to Data page.
      await dataPage.waitForLoad();
      const resourceCard = new DataResourceCard(page);
      expect(await resourceCard.findCard(cohortName)).toBeTruthy();
    });
  });

  test('Delete cohort', async () => {
    await withSignInTest()(async (page) => {
      const cohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) => cohort.clickResourceName());

      const cohortBuildPage = new CohortBuildPage(page);
      await cohortBuildPage.waitForLoad();

      // Delete cohort while inside the Cohort Build page
      const modalContent = await cohortBuildPage.deleteCohort();
      expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

      // Back to the Data page.
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();

      const resourceCard = new DataResourceCard(page);
      expect(await resourceCard.findCard(cohortName)).toBeFalsy();
    });
  });

  test('Save as cohort', async () => {
    await withSignInTest()(async (page) => {
      const originalCohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) =>
        cohort.clickResourceName()
      );

      const cohortBuildPage = new CohortBuildPage(page);
      await cohortBuildPage.waitForLoad();

      // Save Total Count for comparison.
      const totalCount = await cohortBuildPage.getTotalCount();

      // Save as.
      const cohortName = await cohortBuildPage.saveChanges(MenuOption.SaveAs);
      console.log(`Saved as Cohort: "${cohortName}"`);

      // Click new cohort name link. Open Cohort Build page.
      const cohortLink = Link.findByName(page, { name: cohortName });
      await cohortLink.clickAndWait();

      // Total Count should be unchanged.
      expect(await cohortBuildPage.getTotalCount()).toEqual(totalCount);

      // Delete cohort while inside the Cohort Build page
      const modalContent = await cohortBuildPage.deleteCohort();
      expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

      // Back to the Data page.
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();

      const resourceCard = new DataResourceCard(page);
      // Save as cohort is gone.
      expect(await resourceCard.findCard(cohortName)).toBeFalsy();
      // Original cohort remains.
      expect(await resourceCard.findCard(originalCohortName)).toBeTruthy();
    });
  });

  // Helper functions
  async function findAWorkspace(): Promise<void> {
    if (workspaceUrl) {
      // Faster: Load previously saved URL instead clicks thru pages to open workspace data page.
      await page.goto(workspaceUrl, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
      return;
    }
    await findOrCreateWorkspace(page, { workspaceName: workspace });
    workspaceUrl = page.url();
  }

  async function setUpWorkspaceAndCohort(): Promise<DataResourceCard> {
    await findAWorkspace();
    await deleteAllCohort('Duplicate');
    const dataPage = new WorkspaceDataPage(page);
    const cohortCard = await dataPage.findOrCreateCohort();
    return duplicateCohort(cohortCard);
  }

  // Make a duplicate of an existing cohort to avoid hitting issue of concurrent data editing.
  async function duplicateCohort(cohortCard: DataResourceCard): Promise<DataResourceCard> {
    const cohortName = await cohortCard.getResourceName();
    const snowmanMenu = await cohortCard.getSnowmanMenu();
    await snowmanMenu.select(MenuOption.Duplicate, { waitForNav: false });
    await waitWhileLoading(page);
    const duplicateCohortName = `Duplicate of ${cohortName}`;
    const dataPage = new WorkspaceDataPage(page);
    // Rename duplicate Cohort name
    const newCohortName = makeRandomName();
    await dataPage.renameResource(duplicateCohortName, newCohortName, ResourceCard.Cohort);
    // Verify rename successful.
    expect(await DataResourceCard.findCard(page, newCohortName)).toBeTruthy();
    expect(await DataResourceCard.findCard(page, cohortName)).toBeTruthy();
    return dataPage.findCohortCard(newCohortName);
  }

  // Delete existing Cohorts with name contains 'Duplicate' string.
  async function deleteAllCohort(pattern: string): Promise<void> {
    const dataResourceCard = new DataResourceCard(page);
    const cards = await dataResourceCard.getResourceCard(ResourceCard.Cohort);

    const cardNames = [];
    for (const card of cards) {
      const name = await card.getResourceName();
      if (name.includes(pattern)) {
        cardNames.push(await card.getResourceName());
      }
    }

    for (const name of cardNames) {
      const modalTextContent = await new WorkspaceDataPage(page).deleteResource(name, ResourceCard.Cohort);
      // Verify Delete Confirmation modal text
      expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: ${name}?`);
      expect(await DataResourceCard.findCard(page, name, 2000)).toBeFalsy();
      console.log(`deleted cohort ${name}`);
    }
  }
});
