import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import ApplicationsPanel from 'app/sidebar/applications-panel';
import Button from 'app/element/button';

// 10 minutes
jest.setTimeout(10 * 60 * 1000);

describe('RStudio GKE app', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRStudioGkeAppTest';

  test('Launch and delete an RStudio GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const applicationsPanel = new ApplicationsPanel(page);
    await applicationsPanel.open();

    await new Button(page, `${applicationsPanel.getXpath()}//*[@data-test-id="RStudio-unexpanded"]`).click();

    await new Button(
      page,
      `${applicationsPanel.getXpath()}//*[@data-test-id="RStudio-expanded"]//*[@data-test-id="apps-panel-button-Create"]`
    ).click();

    // todo with order I should probably do them
    // (3) refresh the page
    // (1) open rstudio
    // (2) delete
  });
});
