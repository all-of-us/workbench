import HelpSidebar, {HelpSidebarTab} from 'app/component/help-sidebar';
import RuntimePanel from 'app/component/runtime-panel';
import {config} from 'resources/workbench-config';
import {RuntimeStatusIconColors} from 'utils/color-utils';
import {createWorkspace, signIn} from 'utils/test-utils';

import {ElementHandle} from "puppeteer";


const getElementColorHex = async (element: ElementHandle) => {
  const elementHandle = await element.evaluateHandle(
      (e) => window.getComputedStyle(e),
      element.asElement()
  );
  return await elementHandle.getProperty('color')
    .then(async prop => {
      // This gets you 'rgb(red, grn, blu)'
      const stringyRgb = (await prop.jsonValue()).toString();
      const hexRgb = stringyRgb
        // Remove 'rgb(...)'
        .slice(4, -1)
        // Extract stringy numbers
        .split(',')
        .map(str => {
          // Interpret in base 16
          const stringyBase16 = parseInt(str).toString(16);
          // Pad with 0 if not long enough
          return stringyBase16.length === 1 ? `0${stringyBase16}` : stringyBase16;
        });
      return `#${hexRgb.join('').toUpperCase()}`
    });
}

describe('Updating runtime parameters', () => {
  beforeEach(async () => {
    await signIn(page);
  });

  test('Create a default runtime', async() => {
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCreateButton();

    const runtimeStatusIcon = await helpSidebar.getRuntimeStatusIcon();
    const color = await getElementColorHex(runtimeStatusIcon);
    expect(color).toBe(RuntimeStatusIconColors.Starting);
  });

  test('Create a custom runtime', async() => {
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCustomizeButton();

    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(100);

    await runtimePanel.clickCreateButton();
    const runtimeStatusIcon = await helpSidebar.getRuntimeStatusIcon();
    const color = await getElementColorHex(runtimeStatusIcon);
    expect(color).toBe(RuntimeStatusIconColors.Starting);
  });
});
