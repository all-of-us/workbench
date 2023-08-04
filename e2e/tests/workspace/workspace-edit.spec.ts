import { MenuOption, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import * as testData from 'resources/data/workspace-data';
import { findOrCreateWorkspaceCard, openTab, performActions, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { makeWorkspaceName } from 'utils/str-utils';

describe.skip('Editing workspace via workspace card snowman menu', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = makeWorkspaceName();

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Edit workspace: changing Question 2 answers.
   * - Verify landing in DATA tab.
   * - Verify Workspace Information in ABOUT tab.
   */
  test('User as OWNER can edit workspace', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName });

    await workspaceCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);

    // Data Access Tier is readonly.
    const accessTierSelect = workspaceEditPage.getDataAccessTierSelect();
    expect(await accessTierSelect.isDisabled()).toEqual(true);

    // CDR Version Select is readonly. Get selected value.
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.isDisabled()).toEqual(true);
    const selectedValue = await cdrVersionSelect.getSelectedValue();

    // Change question #2 answer
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    const updateButton = workspaceEditPage.getUpdateWorkspaceButton();
    await updateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(updateButton);

    // Check Workspace Information

    // Check CDR version
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    const cdrValue = await aboutPage.getCdrVersion();
    expect(cdrValue).toEqual(expect.stringContaining(selectedValue));

    // Check CreationDate string is a valid date and compare year string.
    const creationDate = await aboutPage.getCreationDate();
    // Current AoU Date format: "Tue Jun 16 2020"
    const splitDate = creationDate.split(' ');
    const year = splitDate[splitDate.length - 1];
    const utcYear = new Date(creationDate).getFullYear();
    expect(Number(year)).toBe(utcYear);
    expect(!isNaN(Date.parse(creationDate))).toBe(true);

    // Check LastUpdatedDate (Same date format as in Creation Date) string is a valid date format.
    const lastUpdatedDate = await aboutPage.getLastUpdatedDate();
    expect(!isNaN(Date.parse(lastUpdatedDate))).toBe(true);

    // LastUpdatedDate should be today date. See format option https://www.w3schools.com/jsref/jsref_tolocalestring.asp
    const todayWeekday = new Date().toLocaleString('en-us', { weekday: 'short' });
    const todayYear = new Date().toLocaleString('en-us', { year: 'numeric' });
    const todayDay = new Date().toLocaleString('en-us', { day: '2-digit' });

    const lastUpdatedWeekday = new Date(lastUpdatedDate).toLocaleString('en-us', { weekday: 'short' });
    const lastUpdatedYear = new Date(lastUpdatedDate).toLocaleString('en-us', { year: 'numeric' });
    const lastUpdatedDay = new Date(lastUpdatedDate).toLocaleString('en-us', { day: '2-digit' });

    expect(todayWeekday).toBe(lastUpdatedWeekday);
    expect(todayYear).toBe(lastUpdatedYear);
    expect(todayDay).toBe(lastUpdatedDay);
  });
  /**
   * Test:
   * - Find an existing workspace or Create a new workspace if none exists.
   * - navigate to About page
   * - Click the workspace action menu and Edit workspace: changing Question 2 answers.
   * - Verify landing in DATA tab.
   * - Verify Workspace Information in ABOUT tab.
   */

  test('User as OWNER can edit workspace via workspace action menu', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName });

    // Verify Workspace Access Level is OWNER.
    const accessLevel = await workspaceCard.getAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

    await workspaceCard.clickName();

    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);
    await aboutPage.editWorkspace();

    const workspaceEditPage = new WorkspaceEditPage(page);

    // CDR Version Select is readonly. Get selected value.
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    const selectedValue = await cdrVersionSelect.getSelectedValue();

    // Change question #2 answer
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    const updateButton = workspaceEditPage.getUpdateWorkspaceButton();
    await updateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(updateButton);

    // navigate to About Page
    await openTab(page, Tabs.About, aboutPage);

    const cdrValue = await aboutPage.getCdrVersion();
    expect(cdrValue).toEqual(expect.stringContaining(selectedValue));

    // Check CreationDate string is a valid date and compare year string.
    const creationDate = await aboutPage.getCreationDate();
    // Current AoU Date format: "Tue Jun 16 2020"
    const splitDate = creationDate.split(' ');
    const year = splitDate[splitDate.length - 1];
    const utcYear = new Date(creationDate).getFullYear();
    expect(Number(year)).toBe(utcYear);
    expect(!isNaN(Date.parse(creationDate))).toBe(true);

    // Check LastUpdatedDate (Same date format as in Creation Date) string is a valid date format.
    const lastUpdatedDate = await aboutPage.getLastUpdatedDate();
    expect(!isNaN(Date.parse(lastUpdatedDate))).toBe(true);

    // LastUpdatedDate should be today date. See format option https://www.w3schools.com/jsref/jsref_tolocalestring.asp
    const todayWeekday = new Date().toLocaleString('en-us', { weekday: 'short' });
    const todayYear = new Date().toLocaleString('en-us', { year: 'numeric' });
    const todayDay = new Date().toLocaleString('en-us', { day: '2-digit' });

    const lastUpdatedWeekday = new Date(lastUpdatedDate).toLocaleString('en-us', { weekday: 'short' });
    const lastUpdatedYear = new Date(lastUpdatedDate).toLocaleString('en-us', { year: 'numeric' });
    const lastUpdatedDay = new Date(lastUpdatedDate).toLocaleString('en-us', { day: '2-digit' });

    expect(todayWeekday).toBe(lastUpdatedWeekday);
    expect(todayYear).toBe(lastUpdatedYear);
    expect(todayDay).toBe(lastUpdatedDay);
  });
});
