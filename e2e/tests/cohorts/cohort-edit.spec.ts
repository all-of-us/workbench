import { Page } from 'puppeteer';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import { makeWorkspaceName } from 'utils/str-utils';
import { MenuOption, Tabs } from 'app/text-labels';
import DataResourceCard from 'app/component/card/data-resource-card';
import Link from 'app/element/link';
import { Visits } from 'app/page/cohort-participants-group';

describe.skip('Editing Cohort Test', () => {
  // Tests require one and same workspace
  const workspaceName = makeWorkspaceName();
  let workspaceUrl: string;
  let cohortName: string;

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Include all visit types in a single Include Group.
  test('Create Cohort from all visits', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    // Add new Cohort.
    const cohortBuildPage = await new WorkspaceDataPage(page).clickAddCohortsButton();

    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeVisits([
      Visits.AmbulanceVisit,
      Visits.AmbulatoryClinicCenter,
      Visits.AmbulatoryRehabilitationVisit,
      Visits.EmergencyRoomVisit,
      Visits.EmergencyRoomAndInpatientVisit,
      Visits.HomeVisit,
      Visits.InpatientVisit,
      Visits.LaboratoryVisit,
      Visits.NonHospitalInstitutionVisit,
      Visits.OfficeVisit,
      Visits.OutpatientVisit,
      Visits.PharmacyVisit
    ]);
    const group1Count = await group1.getGroupCount();
    const totalCount = await cohortBuildPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);
    expect(Number.isNaN(group1Count)).toBe(false);

    // Save new cohort.
    cohortName = await cohortBuildPage.createCohort();

    // Delete cohort in Cohort Build page.
    // await new CohortActionsPage(page).deleteCohort();
  });

  test('Discard changes', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    // Open previously created Cohort.
    const dataPage = new WorkspaceDataPage(page);
    const cohortNameCell = await dataPage.findCohortEntry(cohortName);
    await cohortNameCell.click();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Wait for Total Count that also indicates page load is ready.
    await cohortBuildPage.getTotalCount();

    // Start making some changes. All changes won't be saved.
    // Switch on Temporal in Group 1.
    const group1 = cohortBuildPage.findIncludeParticipantsGroupByIndex();
    await group1.clickTemporalSwitch(true);

    // Click Data tab, Warning (Discard Changes) modal should open.
    await openTab(page, Tabs.Data);

    await cohortBuildPage.discardChangesConfirmationDialog();

    // Discard changes brings user back to the Data page.
    await dataPage.waitForLoad();
  });

  test('Save as cohort', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    // Open previously created Cohort.
    const dataPage = new WorkspaceDataPage(page);
    const cohortNameCell = await dataPage.findCohortEntry(cohortName);
    await cohortNameCell.click();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Save Total Count for comparison.
    const totalCount = await cohortBuildPage.getTotalCount();

    // Save as.
    const newCohortName = await cohortBuildPage.saveChanges(MenuOption.SaveAs);
    console.log(`Saved as Cohort: "${newCohortName}"`);

    // Click new cohort name link. Open Cohort Build page.
    const cohortLink = Link.findByName(page, { name: newCohortName });
    await cohortLink.clickAndWait();

    // Total Count should be unchanged.
    expect(await cohortBuildPage.getTotalCount()).toEqual(totalCount);

    // Delete cohort while inside the Cohort Build page
    const modalContent = await cohortBuildPage.deleteCohort();
    expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${newCohortName}?`);

    // Back to the Data page.
    await dataPage.waitForLoad();

    const resourceTable = new DataResourceCard(page);
    // Save as cohort is gone.
    expect(await resourceTable.findNameCellLinkFromTable({ name: newCohortName })).toBeFalsy();
    // Original cohort remains.
    expect(await resourceTable.findNameCellLinkFromTable({ name: cohortName })).toBeTruthy();
  });

  // Helper functions
  async function loadWorkspaceUrl(page: Page, workspaceName: string): Promise<void> {
    if (workspaceUrl) {
      // Faster: Load previously saved URL instead clicks thru links to open workspace data page.
      await page.goto(workspaceUrl, { waitUntil: ['load', 'networkidle0'] });
      return;
    }
    await findOrCreateWorkspace(page, { workspaceName });
    workspaceUrl = page.url(); // Save URL for load workspace directly without search.
  }
});
