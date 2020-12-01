import {createWorkspace, signIn} from "utils/test-utils";
import HelpSidebar from "app/component/help-sidebar";
import {config} from "../../resources/workbench-config";

describe('Updating runtime parameters', () => {
  beforeEach(async () => {
    await signIn(page);
  });

  test('Create a default runtime', async() => {
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    const helpSidebar = new HelpSidebar(page);
    const state = await helpSidebar.maybeCreateRuntimeAndGetState();
    console.log(state);
  });
});
