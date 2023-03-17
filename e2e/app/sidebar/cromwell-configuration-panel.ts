import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';

const defaultXpath = '//*[@id="cromwell-configuration-panel"]';

export default class CromwellConfigurationPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.CromwellConfiguration);
  }
}
