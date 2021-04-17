import WorkspacesPage from 'app/page/workspaces-page';
import { signIn } from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import HomePage from 'app/page/home-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceReviewResearchPurposeModal from 'app/modal/workspace-review-research-purpose-modal';
import * as fp from 'lodash/fp';

describe('WRITER Workspace actions tests', () => {
  beforeEach(async () => {
    await signIn(page, config.writerUserName, config.userPassword);
  });

  // Tests don't require creating new workspace
  test('WRITER cannot share, edit or delete workspace', async () => {
    const homePage = new HomePage(page);
    await homePage.getSeeAllWorkspacesLink().click();

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    // Verify Workspace Access Level is READER.
    const workspaceCards = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Writer);
    if (workspaceCards.length === 0) {
      return; // end test because no Workspace card available for checking
    }

    const workspaceCard = fp.shuffle(workspaceCards)[0];
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    // Share, Edit and Delete actions are not available for click.
    await workspaceCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Writer);

    // Make sure the Search input-field in Share modal is disabled.
    await workspaceCard.clickWorkspaceName(false);

    const aboutPage = new WorkspaceAboutPage(page);

    // Older workspace requires review research purpose again
    const reviewPurposeModal = new WorkspaceReviewResearchPurposeModal(page);
    const modalVisible = await reviewPurposeModal.isVisible(3000);
    if (modalVisible) {
      await reviewPurposeModal.clickReviewNowButton();
      await aboutPage.waitForLoad();
      await page
        .waitForXPath('//a[text()="Looks Good"]', { visible: true, timeout: 2000 })
        .then((link) => link.click())
        .catch(() => {
          // Ignore
        });
    } else {
      await new WorkspaceDataPage(page).openAboutPage();
    }

    await aboutPage.waitForLoad();
    const modal = await aboutPage.openShareModal();
    const searchInput = modal.waitForSearchBox();
    expect(await searchInput.isDisabled()).toBe(true);
    await modal.clickButton(LinkText.Cancel);
  });

  /*

  // TODO add new test
  test('WRITER cannot edit or delete workspace notebook, dataset or conceptset', async () => {
    // ADD HERE
  });

  */
});
